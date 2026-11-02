package com.narezany.calculator

import com.narezany.calculator.calc.CalcException
import com.narezany.calculator.calc.CalculatorEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

/**
 * Pure JVM tests for the expression engine: `gradle :app:testDebugUnitTest`
 */
class CalculatorEngineTest {

    private fun result(expression: String): String =
        CalculatorEngine.format(CalculatorEngine.evaluate(expression))

    @Test
    fun precedenceIsRespected() {
        assertEquals("14", result("2+3×4"))
        assertEquals("11", result("2+3×4-6÷2"))
        assertEquals("20", result("(2+3)×4"))
    }

    @Test
    fun unaryMinusIsSupported() {
        assertEquals("-2", result("-5+3"))
        assertEquals("5", result("2--3"))
        assertEquals("-60", result("12×-5"))
    }

    @Test
    fun percentIsContextAware() {
        assertEquals("220", result("200+10%"))
        assertEquals("180", result("200-10%"))
        assertEquals("20", result("200×10%"))
        assertEquals("0.5", result("50%"))
        assertEquals("5.1", result("10%+5"))
    }

    @Test
    fun decimalMathIsExact() {
        assertEquals("0.3", result("0.1+0.2"))
        assertEquals("2.25", result("1.5×1.5"))
        assertEquals("50", result("100×50%"))
    }

    @Test
    fun longDivisionIsRounded() {
        assertEquals("0.333333333333", result("1÷3"))
    }

    @Test
    fun divisionByZeroThrows() {
        assertThrows(CalcException::class.java) {
            CalculatorEngine.evaluate("5÷0")
        }
    }

    @Test
    fun brokenExpressionsThrow() {
        assertThrows(CalcException::class.java) {
            CalculatorEngine.evaluate("5+×2")
        }
        assertThrows(CalcException::class.java) {
            CalculatorEngine.evaluate("(2+3")
        }
    }

    @Test
    fun danglingOperatorCanBeTrimmedForPreview() {
        assertEquals("12", CalculatorEngine.trimIncomplete("12+"))
        assertEquals("", CalculatorEngine.trimIncomplete("-"))
        assertEquals("200×10%", CalculatorEngine.trimIncomplete("200×10%"))
    }
}
