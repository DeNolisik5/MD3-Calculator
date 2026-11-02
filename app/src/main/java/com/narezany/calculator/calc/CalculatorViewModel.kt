package com.narezany.calculator.calc

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Everything the screen needs, kept in a single immutable value.
 * Because it lives in a ViewModel + StateFlow, rotating the device (which
 * recreates the Activity) keeps the expression exactly as it was.
 */
data class CalculatorUiState(
    val expression: String = "",
    val preview: String = "",
    val history: String = "",
    val error: String? = null,
    val justEvaluated: Boolean = false,
) {
    val previewVisible: Boolean
        get() = preview.isNotEmpty() && preview != expression && error == null
}

class CalculatorViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    private val _state = MutableStateFlow(restore())
    val state: StateFlow<CalculatorUiState> = _state.asStateFlow()

    init {
        persist(_state.value)
    }

    // ---------------------------------------------------------------- input

    fun onDigit(digit: Char) {
        val current = _state.value
        var expression = if (current.error != null || current.justEvaluated) "" else current.expression

        if (expression.endsWith("%") || expression.endsWith(")")) expression += "×"

        val number = trailingNumber(expression)
        if (number.count { it.isDigit() } >= MAX_INPUT_DIGITS) return

        expression = when (number) {
            "0" -> expression.dropLast(1) + digit
            "-0" -> expression.dropLast(1) + digit
            else -> expression + digit
        }

        commit(CalculatorUiState(expression = expression, history = current.history))
    }

    fun onDecimal() {
        val current = _state.value
        var expression = if (current.error != null || current.justEvaluated) "" else current.expression

        if (expression.endsWith("%") || expression.endsWith(")")) expression += "×"

        val number = trailingNumber(expression)
        if (number.contains('.')) return

        expression = when {
            expression.isEmpty() -> "0."
            number.isEmpty() -> "${expression}0."
            else -> "$expression."
        }

        commit(CalculatorUiState(expression = expression, history = current.history))
    }

    fun onOperator(symbol: String) {
        val current = _state.value
        var expression = if (current.error != null) "" else current.expression

        if (expression.isEmpty()) {
            if (symbol == "-") commit(CalculatorUiState(expression = "-"))
            return
        }
        if (expression == "-") return

        val last = expression.last()
        expression = when {
            last in OPERATORS -> expression.dropLast(1) + symbol
            last == '.' -> expression.dropLast(1) + symbol
            last == '%' || last == ')' || last.isDigit() -> expression + symbol
            else -> return
        }

        commit(CalculatorUiState(expression = expression, history = current.history))
    }

    fun onPercent() {
        val current = _state.value
        if (current.error != null) return

        var expression = current.expression
        val last = expression.lastOrNull() ?: return
        if (last == '%') return
        if (!(last.isDigit() || last == ')' || last == '.')) return

        if (last == '.') expression = expression.dropLast(1)
        expression += "%"

        commit(CalculatorUiState(expression = expression, history = current.history))
    }

    fun onToggleSign() {
        val current = _state.value
        if (current.error != null) {
            clear()
            return
        }

        val expression = current.expression
        if (expression.isEmpty() || expression == "-") {
            commit(CalculatorUiState(expression = if (expression.isEmpty()) "-" else ""))
            return
        }
        if (expression.endsWith("%") || expression.endsWith(")")) return

        val match = LAST_NUMBER.find(expression) ?: return
        val start = match.range.first
        val before = expression.getOrNull(start - 1)
        val unaryMinus = before == '-' &&
            (start - 1 == 0 || expression[start - 2] in "+-×÷(")

        val updated = if (unaryMinus) {
            expression.removeRange(start - 1, start)
        } else {
            expression.substring(0, start) + "-" + expression.substring(start)
        }

        commit(CalculatorUiState(expression = updated, history = current.history))
    }

    fun onBackspace() {
        val current = _state.value
        if (current.error != null) {
            clear()
            return
        }

        var expression = current.expression
        if (expression.isEmpty()) return

        expression = expression.dropLast(1)
        if (expression == "-") expression = ""

        commit(CalculatorUiState(expression = expression, history = current.history))
    }

    fun onClear() = clear()

    fun onEquals() {
        val current = _state.value
        val raw = current.expression
        if (raw.isBlank() || raw == "-") return

        val cleaned = CalculatorEngine.trimIncomplete(raw)
        if (cleaned.isBlank()) return

        val (result, error) = CalculatorEngine.formatOrError(cleaned)
        if (result != null) {
            commit(
                CalculatorUiState(
                    expression = result,
                    history = "$cleaned =",
                    justEvaluated = true,
                )
            )
        } else {
            commit(
                current.copy(
                    error = error ?: "Ошибка",
                    preview = "",
                    justEvaluated = false,
                )
            )
        }
    }

    // -------------------------------------------------------------- helpers

    private fun clear() {
        commit(CalculatorUiState())
    }

    private fun commit(state: CalculatorUiState) {
        val next = state.withPreview().let {
            if (it.expression.length > MAX_EXPRESSION_LENGTH) {
                it.copy(expression = it.expression.take(MAX_EXPRESSION_LENGTH)).withPreview()
            } else {
                it
            }
        }
        _state.value = next
        persist(next)
    }

    private fun CalculatorUiState.withPreview(): CalculatorUiState {
        val base = CalculatorEngine.trimIncomplete(expression)
        val hasOperators = expression.any { it in OPERATORS || it == '%' }
        val preview = if (base.isBlank() || !hasOperators || justEvaluated) {
            ""
        } else {
            val (value, _) = CalculatorEngine.formatOrError(base)
            value.orEmpty()
        }
        return copy(preview = preview)
    }

    private fun persist(state: CalculatorUiState) {
        savedState[KEY_EXPRESSION] = state.expression
        savedState[KEY_HISTORY] = state.history
    }

    private fun restore(): CalculatorUiState {
        val expression = savedState.get<String>(KEY_EXPRESSION).orEmpty()
        val history = savedState.get<String>(KEY_HISTORY).orEmpty()
        return CalculatorUiState(expression = expression, history = history).withPreview()
    }

    /** Last typed number (may contain a dot), or an empty string. */
    private fun trailingNumber(expression: String): String {
        val match = TRAILING_NUMBER.find(expression) ?: return ""
        return match.value
    }

    companion object {
        private const val KEY_EXPRESSION = "calculator_expression"
        private const val KEY_HISTORY = "calculator_history"
        private const val MAX_INPUT_DIGITS = 15
        private const val MAX_EXPRESSION_LENGTH = 200

        private val OPERATORS = setOf('+', '-', '×', '÷')
        private val LAST_NUMBER = Regex("""\d+(?:\.\d*)?$""")
        private val TRAILING_NUMBER = Regex("""-?\d*\.?\d*$""")
    }
}
