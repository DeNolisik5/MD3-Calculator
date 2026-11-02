package com.narezany.calculator.calc

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

/** Thrown for anything the calculator can not (or should not) compute. */
class CalcException(message: String) : Exception(message)

/**
 * A small, dependency free expression engine.
 *
 * Grammar (plus / minus bind weaker than multiply / divide, unary signs and
 * parentheses are supported):
 *
 *   expression := term (('+' | '-') term)*
 *   term       := factor (('×' | '÷') factor)*
 *   factor     := ('+' | '-')* (number | '(' expression ')') '%'*
 *
 * Percent is context aware, like the stock Android calculator:
 *   "200+10%"  -> 220   (10% of the accumulated 200)
 *   "200×10%"  -> 20    (10% is simply 0.1 here)
 *   "50%"      -> 0.5
 *
 * All arithmetic uses BigDecimal so 0.1 + 0.2 is exactly 0.3.
 */
object CalculatorEngine {

    private val MC = MathContext(24, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal(100)
    private val ZERO = BigDecimal.ZERO
    private const val MAX_SIGNIFICANT_DIGITS = 12

    /** Applies the display symbols so both "2*3" and "2×3" are accepted. */
    fun normalize(input: String): String = buildString(input.length) {
        for (ch in input) {
            when (ch) {
                '×', '*' -> append('×')
                '÷', '/' -> append('÷')
                '−', '–' -> append('-')
                ',' -> append('.')
                ' ', '\u00A0', '\u202F' -> Unit
                else -> append(ch)
            }
        }
    }

    /** Removes a dangling operator so a half typed expression can be previewed. */
    fun trimIncomplete(raw: String): String {
        var s = raw.trim()
        while (s.isNotEmpty() && s.last() in "+-×÷") {
            s = s.dropLast(1).trimEnd()
        }
        // A leading minus is meaningful ("-5"), keep it.
        if (s == "-") return ""
        return s
    }

    /** Evaluates [raw] and returns the exact result. */
    fun evaluate(raw: String): BigDecimal {
        val normalized = normalize(raw)
        if (normalized.isBlank()) return ZERO
        val parser = Parser(normalized)
        val value = parser.parseAll()
        if (value.compareTo(ZERO) == 0) return ZERO
        return value.round(MC).stripTrailingZeros()
    }

    /** Human readable result, never longer than ~14 characters. */
    fun format(value: BigDecimal): String {
        if (value.compareTo(ZERO) == 0) return "0"
        val rounded = value
            .round(MathContext(MAX_SIGNIFICANT_DIGITS, RoundingMode.HALF_UP))
            .stripTrailingZeros()

        val abs = rounded.abs()
        if (abs >= BigDecimal("1000000000000") || abs < BigDecimal("0.000000001")) {
            // Scientific notation for extreme values.
            return rounded.round(MathContext(8, RoundingMode.HALF_UP))
                .stripTrailingZeros()
                .toString()
        }

        var plain = rounded.toPlainString()
        // Never show "-0".
        if (plain == "-0" || plain == "-0.0") plain = "0"
        return plain
    }

    /** Formatted result or a short error description. */
    fun formatOrError(expression: String): Pair<String?, String?> = try {
        format(evaluate(expression)) to null
    } catch (e: CalcException) {
        null to (e.message ?: "Ошибка")
    } catch (e: ArithmeticException) {
        null to "Деление на ноль невозможно"
    } catch (e: NumberFormatException) {
        null to "Некорректное выражение"
    }

    private class Parser(private val src: String) {
        private var index = 0

        fun parseAll(): BigDecimal {
            val value = expression(null)
            skipSpaces()
            if (index < src.length) fail()
            return value
        }

        private fun expression(percentBase: BigDecimal?): BigDecimal {
            var acc = term(percentBase)
            while (true) {
                when (val op = peek()) {
                    null, ')' -> return acc
                    '+', '-' -> {
                        index++
                        val rhs = term(acc)
                        acc = if (op == '+') acc.add(rhs, MC) else acc.subtract(rhs, MC)
                    }
                    else -> fail()
                }
            }
        }

        private fun term(percentBase: BigDecimal?): BigDecimal {
            var acc = factor(percentBase)
            while (true) {
                when (val op = peek()) {
                    null, ')', '+', '-' -> return acc
                    '×', '÷' -> {
                        index++
                        val rhs = factor(null)
                        acc = if (op == '×') {
                            acc.multiply(rhs, MC)
                        } else {
                            if (rhs.compareTo(ZERO) == 0) {
                                throw CalcException("Деление на ноль невозможно")
                            }
                            acc.divide(rhs, MC)
                        }
                    }
                    else -> fail()
                }
            }
        }

        private fun factor(percentBase: BigDecimal?): BigDecimal {
            var sign = 1
            while (true) {
                when (peek()) {
                    '-' -> { sign = -sign; index++ }
                    '+' -> index++
                    else -> break
                }
            }

            var value: BigDecimal = when (peek()) {
                null -> fail()
                '(' -> {
                    index++
                    val inner = expression(percentBase)
                    if (peek() != ')') throw CalcException("Не хватает скобки")
                    index++
                    inner
                }
                else -> readNumber()
            }

            while (peek() == '%') {
                index++
                value = if (percentBase != null) {
                    percentBase.multiply(value, MC).divide(HUNDRED, MC)
                } else {
                    value.divide(HUNDRED, MC)
                }
            }

            return if (sign < 0) value.negate() else value
        }

        private fun readNumber(): BigDecimal {
            skipSpaces()
            val start = index
            var hasDot = false
            while (index < src.length) {
                val ch = src[index]
                when {
                    ch.isDigit() -> index++
                    ch == '.' && !hasDot -> { hasDot = true; index++ }
                    else -> break
                }
            }
            if (start == index) fail()
            var text = src.substring(start, index)
            if (text == ".") fail()
            if (text.startsWith(".")) text = "0$text"
            if (text.endsWith(".")) text = text.dropLast(1)
            return try {
                BigDecimal(text)
            } catch (e: NumberFormatException) {
                throw CalcException("Некорректное число")
            }
        }

        private fun peek(): Char? {
            skipSpaces()
            return if (index < src.length) src[index] else null
        }

        private fun skipSpaces() {
            while (index < src.length && src[index] == ' ') index++
        }

        private fun fail(): Nothing = throw CalcException("Некорректное выражение")
    }
}
