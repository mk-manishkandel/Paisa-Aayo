package io.github.mkmanishkandel.paisaaayo.parser

/** Language of the spoken announcement. */
enum class SpeechLanguage { NEPALI, ENGLISH }

/**
 * Turns an amount into spoken words using the Nepali/South-Asian grouping
 * (सय / हजार / लाख / करोड — hundred / thousand / lakh / crore).
 */
object Speech {

    /** Nepali words for 0–99. Nepali numbers below 100 are irregular, so they are listed in full. */
    val NEPALI_0_99: List<String> = listOf(
        "शून्य", "एक", "दुई", "तीन", "चार", "पाँच", "छ", "सात", "आठ", "नौ",
        "दश", "एघार", "बाह्र", "तेह्र", "चौध", "पन्ध्र", "सोह्र", "सत्र", "अठार", "उन्नाइस",
        "बीस", "एक्काइस", "बाइस", "तेइस", "चौबीस", "पच्चीस", "छब्बीस", "सत्ताइस", "अठ्ठाइस", "उनन्तीस",
        "तीस", "एकतीस", "बत्तीस", "तेत्तीस", "चौँतीस", "पैँतीस", "छत्तीस", "सैँतीस", "अठतीस", "उनन्चालीस",
        "चालीस", "एकचालीस", "बयालीस", "त्रिचालीस", "चवालीस", "पैँतालीस", "छयालीस", "सतचालीस", "अठचालीस", "उनन्चास",
        "पचास", "एकाउन्न", "बाउन्न", "त्रिपन्न", "चउन्न", "पचपन्न", "छपन्न", "सन्ताउन्न", "अन्ठाउन्न", "उनन्साठी",
        "साठी", "एकसट्ठी", "बयसट्ठी", "त्रिसट्ठी", "चौसट्ठी", "पैँसट्ठी", "छयसट्ठी", "सतसट्ठी", "अठसट्ठी", "उनन्सत्तरी",
        "सत्तरी", "एकहत्तर", "बहत्तर", "त्रिहत्तर", "चौहत्तर", "पचहत्तर", "छयहत्तर", "सतहत्तर", "अठहत्तर", "उनासी",
        "असी", "एकासी", "बयासी", "त्रियासी", "चौरासी", "पचासी", "छयासी", "सतासी", "अठासी", "उनान्नब्बे",
        "नब्बे", "एकानब्बे", "बयानब्बे", "त्रियानब्बे", "चौरानब्बे", "पन्चानब्बे", "छयानब्बे", "सन्तानब्बे", "अन्ठानब्बे", "उनान्सय",
    )

    private val ENGLISH_ONES = listOf(
        "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten",
        "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen", "seventeen", "eighteen", "nineteen",
    )
    private val ENGLISH_TENS = listOf("", "", "twenty", "thirty", "forty", "fifty", "sixty", "seventy", "eighty", "ninety")

    /** Grouping units, largest first. */
    private enum class Unit(val size: Long, val nepali: String, val english: String, val clip: String) {
        CRORE(10_000_000, "करोड", "crore", "ne_karod"),
        LAKH(100_000, "लाख", "lakh", "ne_lakh"),
        THOUSAND(1_000, "हजार", "thousand", "ne_hajar"),
        HUNDRED(100, "सय", "hundred", "ne_saya"),
    }

    /** Splits [n] into (count, unit) pairs plus a remainder < 100. Crore counts may exceed 99. */
    private fun groups(n: Long): Pair<List<Pair<Long, Unit>>, Int> {
        var rest = n
        val parts = mutableListOf<Pair<Long, Unit>>()
        for (unit in Unit.entries) {
            val count = rest / unit.size
            if (count > 0) {
                parts += count to unit
                rest %= unit.size
            }
        }
        return parts to rest.toInt()
    }

    fun nepaliWords(n: Long): String {
        require(n >= 0)
        if (n < 100) return NEPALI_0_99[n.toInt()]
        val (parts, rest) = groups(n)
        val words = parts.map { (count, unit) -> "${nepaliWords(count)} ${unit.nepali}" }.toMutableList()
        if (rest > 0) words += NEPALI_0_99[rest]
        return words.joinToString(" ")
    }

    fun englishWords(n: Long): String {
        require(n >= 0)
        if (n < 100) return englishBelow100(n.toInt())
        val (parts, rest) = groups(n)
        val words = parts.map { (count, unit) -> "${englishWords(count)} ${unit.english}" }.toMutableList()
        if (rest > 0) words += englishBelow100(rest)
        return words.joinToString(" ")
    }

    private fun englishBelow100(n: Int): String = when {
        n < 20 -> ENGLISH_ONES[n]
        n % 10 == 0 -> ENGLISH_TENS[n / 10]
        else -> "${ENGLISH_TENS[n / 10]}-${ENGLISH_ONES[n % 10]}"
    }

    /** Sentence for text-to-speech. [source] is optional (e.g. "eSewa"). */
    fun sentence(
        language: SpeechLanguage,
        amountPaisa: Long,
        direction: Direction,
        source: String? = null,
    ): String {
        val rupees = amountPaisa / 100
        val paisa = amountPaisa % 100
        return when (language) {
            SpeechLanguage.NEPALI -> {
                val amount = buildString {
                    if (rupees > 0) append("${nepaliWords(rupees)} रुपैयाँ")
                    if (paisa > 0) {
                        if (rupees > 0) append(" ")
                        append("${nepaliWords(paisa)} पैसा")
                    }
                }
                val verb = if (direction == Direction.CREDIT) "प्राप्त भयो" else "भुक्तानी भयो"
                val from = when {
                    source.isNullOrBlank() -> ""
                    direction == Direction.CREDIT -> "$source मा "
                    else -> "$source बाट "
                }
                "$from$amount $verb"
            }
            SpeechLanguage.ENGLISH -> {
                val amount = buildString {
                    if (rupees > 0) append("${englishWords(rupees)} ${if (rupees == 1L) "rupee" else "rupees"}")
                    if (paisa > 0) {
                        if (rupees > 0) append(" and ")
                        append("${englishWords(paisa)} paisa")
                    }
                }
                val verb = if (direction == Direction.CREDIT) "Received" else "Paid"
                val via = if (source.isNullOrBlank()) "" else " on $source"
                "$verb $amount$via"
            }
        }
    }

    /**
     * Ordered list of recorded clip names (without extension) for the Nepali voice pack.
     * Every name returned here is listed in [ALL_CLIPS].
     */
    fun nepaliClips(amountPaisa: Long, direction: Direction): List<String> {
        val rupees = amountPaisa / 100
        val paisa = amountPaisa % 100
        val clips = mutableListOf<String>()
        if (rupees > 0) {
            clips += numberClips(rupees)
            clips += "ne_rupaiya"
        }
        if (paisa > 0) {
            clips += numberClips(paisa)
            clips += "ne_paisa"
        }
        clips += if (direction == Direction.CREDIT) "ne_prapta_bhayo" else "ne_bhuktani_bhayo"
        return clips
    }

    private fun numberClips(n: Long): List<String> {
        if (n < 100) return listOf("ne_${n}")
        val (parts, rest) = groups(n)
        val clips = mutableListOf<String>()
        for ((count, unit) in parts) {
            clips += numberClips(count)
            clips += unit.clip
        }
        if (rest > 0) clips += "ne_$rest"
        return clips
    }

    /** Every clip a complete Nepali voice pack must contain, with the words to record. */
    val ALL_CLIPS: Map<String, String> = buildMap {
        for (i in 1..99) put("ne_$i", NEPALI_0_99[i])
        for (unit in Unit.entries) put(unit.clip, unit.nepali)
        put("ne_rupaiya", "रुपैयाँ")
        put("ne_paisa", "पैसा")
        put("ne_prapta_bhayo", "प्राप्त भयो")
        put("ne_bhuktani_bhayo", "भुक्तानी भयो")
    }
}
