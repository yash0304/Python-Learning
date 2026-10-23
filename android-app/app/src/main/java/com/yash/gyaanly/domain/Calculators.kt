package com.yash.gyaanly.domain

import kotlin.math.abs
import kotlin.math.pow

data class CalcInput(val key: String, val label: String, val default: String, val hint: String = "")

class Calculator(
    val id: String,
    val name: String,
    val description: String,
    val formula: String,
    val inputs: List<CalcInput>,
    /** Returns result lines; throws IllegalArgumentException with a readable message on bad input. */
    val compute: (Map<String, String>) -> List<String>,
)

object Calculators {

    val all: List<Calculator> = listOf(
        Calculator(
            id = "npv",
            name = "Net Present Value (NPV)",
            description = "Value today of future cash flows minus the initial investment. Accept if NPV > 0.",
            formula = "NPV = Σ CFt / (1 + r)^t − Initial investment",
            inputs = listOf(
                CalcInput("rate", "Discount rate (%)", "10"),
                CalcInput("initial", "Initial investment", "1000"),
                CalcInput("flows", "Cash flows, year 1..n", "300, 400, 500", "Comma separated"),
            ),
        ) { v ->
            val r = v.num("rate") / 100
            val flows = v.list("flows")
            val pv = flows.mapIndexed { i, cf -> cf / (1 + r).pow(i + 1) }
            val npv = pv.sum() - v.num("initial")
            pv.mapIndexed { i, x -> "PV of year ${i + 1}: ${fmt(flows[i])} / ${fmt(1 + r)}^${i + 1} = ${fmt(x)}" } +
                "Sum of PVs = ${fmt(pv.sum())}" +
                "NPV = ${fmt(npv)} → ${if (npv >= 0) "creates value (accept)" else "destroys value (reject)"}"
        },
        Calculator(
            id = "irr",
            name = "Internal Rate of Return (IRR)",
            description = "The discount rate at which NPV = 0. Accept if IRR > cost of capital.",
            formula = "0 = Σ CFt / (1 + IRR)^t − Initial investment",
            inputs = listOf(
                CalcInput("initial", "Initial investment", "1000"),
                CalcInput("flows", "Cash flows, year 1..n", "300, 400, 500", "Comma separated"),
            ),
        ) { v ->
            val irr = irr(v.num("initial"), v.list("flows"))
            listOf("IRR ≈ ${fmt(irr * 100)}%", "Found by bisection: the rate where NPV changes sign.")
        },
        Calculator(
            id = "wacc",
            name = "WACC",
            description = "Weighted average cost of capital – the minimum return a company must earn.",
            formula = "WACC = E/V × Re + D/V × Rd × (1 − Tax)",
            inputs = listOf(
                CalcInput("equity", "Market value of equity (E)", "600"),
                CalcInput("debt", "Market value of debt (D)", "400"),
                CalcInput("re", "Cost of equity Re (%)", "12"),
                CalcInput("rd", "Cost of debt Rd (%)", "8"),
                CalcInput("tax", "Tax rate (%)", "25"),
            ),
        ) { v ->
            val e = v.num("equity"); val d = v.num("debt")
            require(e + d > 0) { "Equity + debt must be positive" }
            val we = e / (e + d); val wd = d / (e + d)
            val afterTaxRd = v.num("rd") * (1 - v.num("tax") / 100)
            val wacc = we * v.num("re") + wd * afterTaxRd
            listOf(
                "Weights: E/V = ${fmt(we)}, D/V = ${fmt(wd)}",
                "After-tax cost of debt = ${fmt(v.num("rd"))}% × (1 − ${fmt(v.num("tax") / 100)}) = ${fmt(afterTaxRd)}%",
                "WACC = ${fmt(we)} × ${fmt(v.num("re"))}% + ${fmt(wd)} × ${fmt(afterTaxRd)}% = ${fmt(wacc)}%",
            )
        },
        Calculator(
            id = "capm",
            name = "Cost of Equity (CAPM)",
            description = "Return shareholders expect for the risk they take.",
            formula = "Re = Rf + β × (Rm − Rf)",
            inputs = listOf(
                CalcInput("rf", "Risk-free rate (%)", "7"),
                CalcInput("beta", "Beta (β)", "1.2"),
                CalcInput("rm", "Expected market return (%)", "12"),
            ),
        ) { v ->
            val premium = v.num("rm") - v.num("rf")
            val re = v.num("rf") + v.num("beta") * premium
            listOf(
                "Market risk premium = ${fmt(premium)}%",
                "Re = ${fmt(v.num("rf"))}% + ${fmt(v.num("beta"))} × ${fmt(premium)}% = ${fmt(re)}%",
            )
        },
        Calculator(
            id = "cagr",
            name = "CAGR",
            description = "Compound annual growth rate between two values.",
            formula = "CAGR = (End / Start)^(1/years) − 1",
            inputs = listOf(
                CalcInput("start", "Start value", "100"),
                CalcInput("end", "End value", "200"),
                CalcInput("years", "Years", "5"),
            ),
        ) { v ->
            require(v.num("start") > 0 && v.num("years") > 0) { "Start value and years must be positive" }
            val cagr = (v.num("end") / v.num("start")).pow(1 / v.num("years")) - 1
            listOf("CAGR = (${fmt(v.num("end"))} / ${fmt(v.num("start"))})^(1/${fmt(v.num("years"))}) − 1 = ${fmt(cagr * 100)}%")
        },
        Calculator(
            id = "emi",
            name = "Loan EMI",
            description = "Equal monthly instalment for a loan.",
            formula = "EMI = P × r × (1 + r)^n / ((1 + r)^n − 1), r = annual rate / 12",
            inputs = listOf(
                CalcInput("principal", "Loan amount (P)", "1000000"),
                CalcInput("rate", "Annual interest rate (%)", "9"),
                CalcInput("months", "Tenure (months)", "120"),
            ),
        ) { v ->
            val p = v.num("principal"); val n = v.num("months")
            require(n > 0) { "Tenure must be positive" }
            val r = v.num("rate") / 12 / 100
            val emi = if (r == 0.0) p / n else p * r * (1 + r).pow(n) / ((1 + r).pow(n) - 1)
            listOf(
                "Monthly rate r = ${fmt(r * 100, 4)}%",
                "EMI = ${fmt(emi)}",
                "Total paid = ${fmt(emi * n)}, total interest = ${fmt(emi * n - p)}",
            )
        },
        Calculator(
            id = "breakeven",
            name = "Break-even Point",
            description = "Units you must sell before you start making profit.",
            formula = "Break-even units = Fixed costs / (Price − Variable cost per unit)",
            inputs = listOf(
                CalcInput("fixed", "Fixed costs", "50000"),
                CalcInput("price", "Selling price per unit", "100"),
                CalcInput("variable", "Variable cost per unit", "60"),
            ),
        ) { v ->
            val cm = v.num("price") - v.num("variable")
            require(cm > 0) { "Price must exceed variable cost" }
            val units = v.num("fixed") / cm
            listOf(
                "Contribution margin per unit = ${fmt(cm)}",
                "Break-even = ${fmt(v.num("fixed"))} / ${fmt(cm)} = ${fmt(units)} units",
                "Break-even revenue = ${fmt(units * v.num("price"))}",
            )
        },
        Calculator(
            id = "payback",
            name = "Payback Period",
            description = "How long until cumulative cash flows recover the investment.",
            formula = "Years before recovery + Unrecovered amount / Cash flow of recovery year",
            inputs = listOf(
                CalcInput("initial", "Initial investment", "1000"),
                CalcInput("flows", "Cash flows, year 1..n", "300, 400, 500", "Comma separated"),
            ),
        ) { v ->
            payback(v.num("initial"), v.list("flows"))
        },
        Calculator(
            id = "ratios",
            name = "Key Ratios",
            description = "Liquidity, leverage and profitability snapshot.",
            formula = "Current ratio = CA/CL · D/E = Debt/Equity · ROE = Net income/Equity · Net margin = Net income/Revenue",
            inputs = listOf(
                CalcInput("ca", "Current assets", "500"),
                CalcInput("cl", "Current liabilities", "250"),
                CalcInput("debt", "Total debt", "400"),
                CalcInput("equity", "Shareholders' equity", "800"),
                CalcInput("ni", "Net income", "120"),
                CalcInput("revenue", "Revenue", "1500"),
            ),
        ) { v ->
            listOf(
                "Current ratio = ${fmt(v.div("ca", "cl"))}x",
                "Debt-to-equity = ${fmt(v.div("debt", "equity"))}x",
                "ROE = ${fmt(v.div("ni", "equity") * 100)}%",
                "Net profit margin = ${fmt(v.div("ni", "revenue") * 100)}%",
            )
        },
    )

    fun byId(id: String): Calculator? = all.firstOrNull { it.id == id }

    fun npv(rate: Double, initial: Double, flows: List<Double>): Double =
        flows.mapIndexed { i, cf -> cf / (1 + rate).pow(i + 1) }.sum() - initial

    fun irr(initial: Double, flows: List<Double>): Double {
        var lo = -0.99
        var hi = 10.0
        require(npv(lo, initial, flows) * npv(hi, initial, flows) < 0) { "No IRR in range: cash flows never break even" }
        repeat(200) {
            val mid = (lo + hi) / 2
            if (npv(lo, initial, flows) * npv(mid, initial, flows) <= 0) hi = mid else lo = mid
            if (abs(hi - lo) < 1e-9) return mid
        }
        return (lo + hi) / 2
    }

    private fun payback(initial: Double, flows: List<Double>): List<String> {
        var remaining = initial
        for ((i, cf) in flows.withIndex()) {
            if (cf > 0 && cf >= remaining) {
                return listOf(
                    "Recovered during year ${i + 1}",
                    "Payback = $i + ${fmt(remaining)} / ${fmt(cf)} = ${fmt(i + remaining / cf)} years",
                )
            }
            remaining -= cf
        }
        return listOf("Investment not recovered within ${flows.size} years (still ${fmt(remaining)} short)")
    }

    fun fmt(x: Double, decimals: Int = 2): String = "%,.${decimals}f".format(x)

    private fun Map<String, String>.num(key: String): Double =
        this[key]?.trim()?.replace(",", "")?.toDoubleOrNull()
            ?: throw IllegalArgumentException("Enter a number for '$key'")

    private fun Map<String, String>.list(key: String): List<Double> {
        val values = this[key].orEmpty().split(",").map { it.trim() }.filter { it.isNotEmpty() }
        require(values.isNotEmpty()) { "Enter at least one cash flow" }
        return values.map { it.toDoubleOrNull() ?: throw IllegalArgumentException("'$it' is not a number") }
    }

    private fun Map<String, String>.div(a: String, b: String): Double {
        val denominator = num(b)
        require(denominator != 0.0) { "'$b' cannot be zero" }
        return num(a) / denominator
    }
}
