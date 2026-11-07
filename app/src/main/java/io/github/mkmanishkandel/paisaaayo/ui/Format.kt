package io.github.mkmanishkandel.paisaaayo.ui

import io.github.mkmanishkandel.paisaaayo.parser.Money
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Display formatting that respects the UI language (Devanagari digits in Nepali). */
object Format {
    fun amount(paisa: Long, nepali: Boolean): String {
        val prefix = if (nepali) "रु" else "Rs"
        // Non-breaking space: "रु" and the number must never wrap onto separate lines.
        return "$prefix\u00A0${Money.format(paisa, devanagariDigits = nepali)}"
    }

    fun number(value: Long, nepali: Boolean): String =
        if (nepali) Money.toDevanagari(value.toString()) else value.toString()

    fun time(millis: Long, nepali: Boolean): String {
        val text = SimpleDateFormat("h:mm a", if (nepali) Locale.forLanguageTag("ne-NP") else Locale.ENGLISH).format(Date(millis))
        return if (nepali) Money.toDevanagari(text) else text
    }

    fun day(millis: Long, nepali: Boolean): String {
        val text = SimpleDateFormat("EEE, d MMM yyyy", if (nepali) Locale.forLanguageTag("ne-NP") else Locale.ENGLISH).format(Date(millis))
        return if (nepali) Money.toDevanagari(text) else text
    }

    fun startOfDay(millis: Long = System.currentTimeMillis(), daysAgo: Int = 0): Long =
        Calendar.getInstance().apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            add(Calendar.DAY_OF_YEAR, -daysAgo)
        }.timeInMillis
}
