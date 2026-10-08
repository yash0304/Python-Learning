package com.yash.dailyedge

import com.yash.dailyedge.domain.Calculators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorsTest {

    @Test
    fun npvMatchesHandCalculation() {
        // 300/1.1 + 400/1.21 + 500/1.331 - 1000 = -21.04
        assertEquals(-21.04, Calculators.npv(0.10, 1000.0, listOf(300.0, 400.0, 500.0)), 0.01)
    }

    @Test
    fun irrMakesNpvZero() {
        val irr = Calculators.irr(1000.0, listOf(300.0, 400.0, 500.0))
        assertEquals(0.0, Calculators.npv(irr, 1000.0, listOf(300.0, 400.0, 500.0)), 1e-3)
        assertEquals(0.0889, irr, 0.0001)
    }

    @Test
    fun everyCalculatorRunsWithItsDefaults() {
        Calculators.all.forEach { calc ->
            val lines = calc.compute(calc.inputs.associate { it.key to it.default })
            assertTrue("${calc.id} produced no output", lines.isNotEmpty())
        }
    }

    @Test
    fun waccDefaults() {
        val wacc = Calculators.byId("wacc")!!
        val out = wacc.compute(wacc.inputs.associate { it.key to it.default })
        // 0.6 × 12 + 0.4 × 8 × 0.75 = 9.60
        assertTrue(out.last(), out.last().endsWith("9.60%"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun badInputIsRejected() {
        val npv = Calculators.byId("npv")!!
        npv.compute(mapOf("rate" to "ten", "initial" to "1000", "flows" to "100"))
    }
}
