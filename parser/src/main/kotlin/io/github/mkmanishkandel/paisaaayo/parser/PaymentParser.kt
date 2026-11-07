package io.github.mkmanishkandel.paisaaayo.parser

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Decides whether a notification is a real incoming/outgoing payment and extracts the amount.
 *
 * Pipeline (each step can reject the notification with an [IgnoreReason]):
 *  1. app filter — email, chat, social and browser apps are never trusted
 *  2. SMS sender filter — messages from personal numbers / contact names are ignored
 *     (stops "I sent you Rs 5000" scams from being read aloud)
 *  3. remark stripping — free-text remark fields are removed before matching, so a payer
 *     can't make the app announce text they typed
 *  4. OTP, money-request, failed-transaction, telecom, top-up and promotion filters
 *  5. credit / debit classification (English + Nepali)
 *  6. amount + reference extraction
 */
object PaymentParser {

    private const val MAX_RUPEES = 1_000_000_000L // 100 crore — anything above is a parse error

    // ---------- filters ----------

    private val BLOCKED_SMS_SENDERS = setOf("1415")

    private val INSTITUTION_WORDS = listOf(
        "bank", "finance", "bikas", "laghubitta", "sahakari", "cooperative", "saccos", "insurance",
        "capital", "remit", "money", "transfer", "esewa", "khalti", "fonepay", "imepay", "ime pay",
        "connectips", "nchl", "prabhu", "wallet", "pay", "ltd", "limited", "pvt", "alert",
        "service", "support", "merchant", "business", "nepal", "nabil", "nmb", "siddhartha",
        "global", "laxmi", "sanima", "citizens", "everest", "kumari", "prime", "himalayan",
        "sunrise", "mega", "machhapuchchhre", "adbl", "rbb", "nic asia", "ncc", "fund",
        "बैंक", "वित्त", "बिकास", "सहकारी",
    )

    private val PERSON_NAME = Regex("""^[\p{L}\p{M}.]+(?:\s+[\p{L}\p{M}.]+){1,3}$""")
    private val PHONE_LIKE = Regex("""^[+()\d\s.\-]+$""")

    private val REMARK_MARKER = Regex(
        """(?:\bre\s*:|\bremarks?\b|\bpurpose\s*:|\bnote\s*:|\bnarration\s*:|\bparticulars?\s*:|\bmsg\s*:|\bcomments?\s*:|\bmessage\s*:|कैफियत|प्रयोजन)""",
        RegexOption.IGNORE_CASE,
    )
    private val REFERENCE = Regex(
        """(?:\bRRN|\bRef(?:erence)?(?:\s*(?:No|Number|ID))?|\bTransaction(?:\s*ID)?|\bTxn(?:\s*ID)?|\bCode|\bID|\bMERWD)[:\-#.\s]*(\d{8,25})""",
        RegexOption.IGNORE_CASE,
    )
    private val REFERENCE_ALNUM = Regex(
        """\b(?:Txn|Transaction|Ref)\s*(?:ID|No|Number)?[:\-#.\s]*([A-Za-z0-9]{8,25})""",
        RegexOption.IGNORE_CASE,
    )

    private val OTP = Regex(
        """\botp\b|one[\s-]?time\s+(?:password|pin|code)|verification\s+code|\bpin\s+is\b|ओटीपी""",
        RegexOption.IGNORE_CASE,
    )
    private val REQUEST = Regex(
        """\b(?:has\s+)?requested\b|\b(?:payment|money|fund)\s+request\b|\brequest(?:ing)?\s+(?:you\s+)?(?:to\s+pay|for\s+(?:rs|npr|payment))|माग\s*गर""",
        RegexOption.IGNORE_CASE,
    )
    private val FAILED = Regex(
        """\b(?:failed|unsuccessful|declined|could\s+not\s+be\s+(?:processed|completed)|insufficient\s+(?:balance|fund))\b|असफल""",
        RegexOption.IGNORE_CASE,
    )
    private val TELECOM = listOf("ncell", "ntc", "nepal telecoms?", "nepal doocom", "nt mobile", "nt gsm", "nt landline", "nt broadband")
        .map { Regex("""\b$it\b""", RegexOption.IGNORE_CASE) }
    private val TOP_UP = Regex("""\btop[\s\-_]?ups?\b""", RegexOption.IGNORE_CASE)

    private val PROMO_STRONG = listOf(
        """\bget\s+(?:flat\s+|upto\s+|up\s+to\s+|extra\s+)?(?:rs|npr|₹|रु)""", """\bworth\b""",
        """\bfor\s+just\b""", """\bjust\s+for\b""", """\bonly\s+for\s+(?:rs|npr)""", """\boffers?\b""",
        """\bdiscounts?\b""", """\bvouchers?\b""", """\bcoupons?\b""", """\bpromo(?:tion|tional|s)?\b""",
        """\bgame\s*cards?\b""", """\bgift\s*cards?\b""", """\blimited\s+(?:time|period|offer|stock)\b""",
        """\bhurry\b""", """\bgrab\b""", """\benjoy\b""", """\bdeals?\b""", """\bsale\b""", """\bbuy\b""",
        """\b\d+\s*%\s*off\b""", """\bflat\s+(?:rs|npr|\d)""", """\bwin\s+(?:rs|npr|prizes?|gifts?|upto|up\s+to|big)""",
        """\blucky\s+draw\b""", """\bredeem\b""", """\buse\s+(?:promo\s+)?code\b""",
        """\bterms\s*(?:&|and)\s*conditions\b""", """\bt\s*&\s*cs?\b""", """\bunlock\b""", """\bsubscribe\b""",
        """\bupgrade\b""", """\bexclusive\b""", """\bspecial\s+(?:offer|price|deal)\b""",
        "अफर", "छुट", "उपहार", "जित्न", "कुपन", "भाउचर",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val PROMO_WEAK = listOf(
        """\b\d[\d,]*\s*points?\b""", """\bpoints?\b""", """\bfree\b""", """\brewards?\b""", """\bcash\s*back\b""",
        """\bbonus\b""", """\bearn\b""", """\bclaim\b""", """\bdownload\b""", """\binstall\b""", """\bclick\b""",
        """\bvisit\b""", """\bshop(?:ping)?\b""", """\bup\s*to\b""", """\bupto\b""", """\bgames?\b""", """\bplay\b""",
        """\bextra\b""", """\+\s*(?:rs|npr)""", """\.\.\.\s*$""", """…\s*$""",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    // ---------- direction ----------

    private val CURRENCY = Regex(
        """(?<![a-z])(?:rs|npr|inr|rupees?)(?![a-z])|₹|रु|\bamount(?:\s+(?:is|of))?\s*[:\-]?\s*\d""",
        RegexOption.IGNORE_CASE,
    )

    /** "transferred / deposited ... to your account" — money arriving even though "transferred to" looks like a debit. */
    private val CREDIT_TO_YOU = Regex(
        """\b(?:transferred|sent|deposited|credited|added|loaded)\b[^.]{0,40}?\b(?:to|into|in)\s+(?:your|ur)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val FROM_YOUR = Regex(
        """\bfrom\s+(?:your|ur|my)\s+(?:a/?c|ac|acc|account|card|wallet)\b|\bdebited\b""",
        RegexOption.IGNORE_CASE,
    )
    private val STRONG_DEBIT = listOf(
        "debited", "withdrawn", "withdrawal", "transferred to", "transfer to", "trf to", "sent to",
        "you sent", "paid to", "you paid", "you have paid", "successfully transferred",
        "you have transferred", "you transferred",
        "पठाएको", "पठाइयो", "घटाइयो", "निकालेको", "तिर्नुभयो",
    )
    private val WEAK_DEBIT = listOf(
        "debit", "payment to", "sent rs", "sent npr", "has been sent", "paid rs", "paid npr",
        "purchase", "purchased", "bill payment", "recharge", "payment successful",
        "paid successfully", "successfully paid", "गएको", "निकासी", "खर्च", "भुक्तानी गरियो",
        "भुक्तानी गर्नुभयो", "तिरेको",
    )
    private val CREDIT = Regex(
        """\b(?:received|receive|credited|deposited|deposit|cash\s*in)\b|\b(?:has\s+been|have\s+been|was|is|successfully|fund|funds|balance|amount)\s+loaded\b|\bloaded\s+(?:successfully|to|into)\b|जम्मा|प्राप्त|आएको|आयो""",
        RegexOption.IGNORE_CASE,
    )
    private val TXN_SUCCESS_FROM = Regex(
        """\b(?:transaction|txn)\s+(?:is\s+|was\s+)?success(?:ful|fully)?\b.{0,60}?\bfrom\b""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    // ---------- amount ----------

    private const val NUMBER = """(\d[\d,]*(?:\.\d+)?)"""
    private val AMOUNT_AFTER_CURRENCY = Regex(
        """(?:\b(?:rs|npr|inr)\.?|₹|रु\.?|रुपैयाँ|\bamount(?:\s+(?:is|of))?)\s*[:\-]?\s*$NUMBER""",
        RegexOption.IGNORE_CASE,
    )
    private val AMOUNT_BEFORE_CURRENCY = Regex(
        """(?<![\d,.])$NUMBER\s*(?:npr|rs|rupees?|रुपैयाँ)(?![a-z])""",
        RegexOption.IGNORE_CASE,
    )
    private val AMOUNT_AFTER_VERB = Regex(
        """\b(?:deposited|deposit|credited|received)\b\s*(?:of|with|:|-)?\s*(?:rs\.?|npr\.?)?\s*$NUMBER(?![\d/\-:])""",
        RegexOption.IGNORE_CASE,
    )
    private val AMOUNT_BEFORE_VERB = Regex(
        """(?<![\d,.:/\-])$NUMBER\s+(?:(?:has|have)\s+been\s+|is\s+|was\s+|successfully\s+)?(?:deposited|credited|received)\b""",
        RegexOption.IGNORE_CASE,
    )
    private val BALANCE_CONTEXT = Regex("""(?:\bbal\b|balance|\bavl\b|available|limit|ब्यालेन्स|मौज्दात)\W{0,12}$""", RegexOption.IGNORE_CASE)

    private val WHITESPACE = Regex("""\s+""")

    fun parse(input: NotificationInput): ParseResult {
        if (Sources.isBlockedPackage(input.packageName)) return ignored(IgnoreReason.BLOCKED_APP)

        val title = normalize(input.title)
        val body = normalize(input.body)
        if (body.isEmpty()) return ignored(IgnoreReason.EMPTY)

        if (Sources.isSmsApp(input.packageName)) {
            val senders = (listOf(input.title) + input.senders).map(::normalize).filter { it.isNotEmpty() }
            if (senders.any { isBlockedSender(it) || isPersonalSender(it) }) {
                return ignored(IgnoreReason.PERSONAL_SENDER)
            }
        }

        val core = stripRemarks(body)
        val context = "$title $core"

        if (OTP.containsMatchIn(context)) return ignored(IgnoreReason.OTP)
        if (REQUEST.containsMatchIn(context)) return ignored(IgnoreReason.REQUEST)
        if (FAILED.containsMatchIn(context)) return ignored(IgnoreReason.FAILED)
        if (TELECOM.any { it.containsMatchIn(context) }) return ignored(IgnoreReason.TELECOM)
        if (TOP_UP.containsMatchIn(context)) return ignored(IgnoreReason.TOP_UP)
        if (isPromotion(context)) return ignored(IgnoreReason.PROMOTION)
        if (!CURRENCY.containsMatchIn(core)) return ignored(IgnoreReason.NO_CURRENCY)

        val direction = detectDirection(title, core) ?: return ignored(IgnoreReason.NO_DIRECTION)
        val amount = extractAmountPaisa(core) ?: return ignored(IgnoreReason.NO_AMOUNT)

        return ParseResult.Payment(amount, direction, extractReference(body))
    }

    /**
     * The business sender of an SMS notification (e.g. "NABIL_ALERT" or "31001"), or null when the
     * notification isn't an SMS or comes from a personal number, contact name or blocked sender.
     * Used to let the shopkeeper switch individual SMS senders on and off.
     */
    fun smsSender(input: NotificationInput): String? {
        if (!Sources.isSmsApp(input.packageName)) return null
        val senders = (listOf(input.title) + input.senders).map(::normalize).filter { it.isNotEmpty() }
        if (senders.isEmpty() || senders.any { isBlockedSender(it) || isPersonalSender(it) }) return null
        return senders.first().take(MAX_SENDER_LENGTH)
    }

    private const val MAX_SENDER_LENGTH = 40

    // ---------- helpers (internal for tests) ----------

    internal fun normalize(text: String): String =
        text.map { c -> if (c in '०'..'९') '0' + (c - '०') else c }
            .joinToString("")
            .replace(WHITESPACE, " ")
            .trim()

    internal fun stripRemarks(body: String): String {
        var text = body
        REMARK_MARKER.find(text)?.let { m ->
            val before = text.substring(0, m.range.first).trim()
            if (hasAmount(before)) text = before
        }
        REFERENCE.find(text)?.let { m ->
            val upToRef = text.substring(0, m.range.last + 1).trim()
            if (hasAmount(upToRef)) text = upToRef
        }
        return text
    }

    private fun hasAmount(text: String) = CURRENCY.containsMatchIn(text) && text.any(Char::isDigit)

    internal fun isPersonalSender(sender: String): Boolean {
        val decoded = sender.removePrefix("tel:").removePrefix("smsto:").removePrefix("sms:").trim()
        phoneDigits(decoded)?.let { if (it.length >= 10) return true }
        val lower = decoded.lowercase()
        if (INSTITUTION_WORDS.any { it in lower }) return false
        return PERSON_NAME.matches(decoded)
    }

    private fun isBlockedSender(sender: String): Boolean {
        val digits = phoneDigits(sender)
        return sender.lowercase() in BLOCKED_SMS_SENDERS || (digits != null && digits in BLOCKED_SMS_SENDERS)
    }

    /** Normalised Nepali phone digits, or null if [text] isn't a phone number. */
    internal fun phoneDigits(text: String): String? {
        if (text.isEmpty() || !PHONE_LIKE.matches(text)) return null
        var digits = text.filter(Char::isDigit)
        if (digits.startsWith("00")) digits = digits.substring(2)
        if (digits.startsWith("977") && digits.length >= 12) digits = digits.substring(3)
        if (digits.startsWith("0") && digits.length == 11) digits = digits.substring(1)
        return digits.ifEmpty { null }
    }

    private fun isPromotion(text: String): Boolean {
        if (PROMO_STRONG.any { it.containsMatchIn(text) }) return true
        val weakHits = PROMO_WEAK.count { it.containsMatchIn(text) }
        return weakHits >= 2 && !REFERENCE.containsMatchIn(text)
    }

    internal fun detectDirection(title: String, core: String): Direction? {
        val lower = core.lowercase()
        val fromYour = FROM_YOUR.containsMatchIn(core)
        return when {
            CREDIT_TO_YOU.containsMatchIn(core) && !fromYour -> Direction.CREDIT
            STRONG_DEBIT.any { it in lower } -> Direction.DEBIT
            CREDIT.containsMatchIn(core) -> Direction.CREDIT
            TXN_SUCCESS_FROM.containsMatchIn(core) && !fromYour -> Direction.CREDIT
            WEAK_DEBIT.any { it in lower } -> Direction.DEBIT
            CREDIT.containsMatchIn(title) -> Direction.CREDIT
            else -> null
        }
    }

    internal fun extractAmountPaisa(text: String): Long? {
        // Prefer a currency-tagged amount that is not a balance ("Bal: NPR 25,000").
        val tagged = (AMOUNT_AFTER_CURRENCY.findAll(text) + AMOUNT_BEFORE_CURRENCY.findAll(text))
            .sortedBy { it.range.first }
            .toList()
        val nonBalance = tagged.firstOrNull { !BALANCE_CONTEXT.containsMatchIn(text.substring(0, it.range.first)) }
        val candidates = listOfNotNull(
            nonBalance,
            AMOUNT_AFTER_VERB.find(text),
            AMOUNT_BEFORE_VERB.find(text),
            tagged.firstOrNull(),
        )
        return candidates.firstNotNullOfOrNull { toPaisa(it.groupValues[1]) }
    }

    internal fun toPaisa(raw: String): Long? {
        val cleaned = raw.replace(",", "").trimEnd('.')
        val value = cleaned.toBigDecimalOrNull() ?: return null
        if (value <= BigDecimal.ZERO || value > BigDecimal.valueOf(MAX_RUPEES)) return null
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact()
    }

    internal fun extractReference(text: String): String? {
        REFERENCE.find(text)?.let { return it.groupValues[1] }
        return REFERENCE_ALNUM.findAll(text)
            .map { it.groupValues[1] }
            .firstOrNull { ref -> ref.any(Char::isDigit) }
    }

    private fun ignored(reason: IgnoreReason) = ParseResult.Ignored(reason)
}
