package io.github.mkmanishkandel.paisaaayo.parser

/** Formats paisa amounts with South-Asian digit grouping: 12,34,567.50 */
object Money {

    fun format(amountPaisa: Long, devanagariDigits: Boolean = false, alwaysShowPaisa: Boolean = false): String {
        val negative = amountPaisa < 0
        val abs = kotlin.math.abs(amountPaisa)
        val rupees = abs / 100
        val paisa = abs % 100
        val grouped = group(rupees.toString())
        val text = buildString {
            if (negative) append('-')
            append(grouped)
            if (paisa > 0 || alwaysShowPaisa) append('.').append(paisa.toString().padStart(2, '0'))
        }
        return if (devanagariDigits) toDevanagari(text) else text
    }

    /** "1234567" → "12,34,567" */
    private fun group(digits: String): String {
        if (digits.length <= 3) return digits
        val last3 = digits.takeLast(3)
        val head = digits.dropLast(3)
        val headGroups = head.reversed().chunked(2).joinToString(",").reversed()
        return "$headGroups,$last3"
    }

    fun toDevanagari(text: String): String =
        text.map { c -> if (c in '0'..'9') '०' + (c - '0') else c }.joinToString("")
}
