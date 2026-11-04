package io.github.mkmanishkandel.paisaaayo.parser

/** Which way the money moved, from the phone owner's point of view. */
enum class Direction { CREDIT, DEBIT }

/**
 * The parts of an Android notification the parser needs. Kept free of Android types so the
 * whole detection pipeline can be unit-tested on the JVM.
 */
data class NotificationInput(
    val packageName: String,
    val title: String,
    val body: String,
    /** Extra sender names / numbers (conversation title, MessagingStyle senders, people list). */
    val senders: List<String> = emptyList(),
)

sealed interface ParseResult {
    data class Payment(
        /** Amount in paisa (1 rupee = 100 paisa) to avoid floating-point errors. */
        val amountPaisa: Long,
        val direction: Direction,
        /** RRN / transaction ID when present, used for de-duplication. */
        val reference: String?,
    ) : ParseResult

    data class Ignored(val reason: IgnoreReason) : ParseResult
}

enum class IgnoreReason {
    BLOCKED_APP,
    EMPTY,
    PERSONAL_SENDER,
    OTP,
    REQUEST,
    FAILED,
    TELECOM,
    TOP_UP,
    PROMOTION,
    NO_CURRENCY,
    NO_DIRECTION,
    NO_AMOUNT,
}
