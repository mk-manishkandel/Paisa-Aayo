package io.github.mkmanishkandel.paisaaayo.parser

/**
 * Stops the same payment from being announced twice.
 *
 * Apps re-post notifications (progress updates, group summaries) and the same payment often
 * arrives twice — once from the wallet app and once as a bank SMS. Unlike a blanket
 * "same amount within 60 s" lock, this keeps two genuine Rs 100 payments from different
 * customers apart when each has its own reference number.
 *
 * Rules, checked in order:
 *  1. same notification key + same text seen in the last [repostWindowMillis] → duplicate
 *  2. same reference number seen in the last [referenceWindowMillis] → duplicate
 *  3. same amount + direction from a *different* app within [crossAppWindowMillis] → duplicate
 *  4. same amount + direction from the same app, neither with a reference, within
 *     [sameAppWindowMillis] → duplicate (an edited re-post)
 */
class Deduplicator(
    private val repostWindowMillis: Long = 10 * 60_000L,
    private val referenceWindowMillis: Long = 24 * 60 * 60_000L,
    private val crossAppWindowMillis: Long = 45_000L,
    private val sameAppWindowMillis: Long = 5_000L,
    private val capacity: Int = 300,
) {
    private data class Seen(
        val notificationKey: String,
        val textHash: Int,
        val packageName: String,
        val amountPaisa: Long,
        val direction: Direction,
        val reference: String?,
        val atMillis: Long,
    )

    private val seen = ArrayDeque<Seen>()

    /** Returns true when the payment is a duplicate; otherwise records it and returns false. */
    @Synchronized
    fun checkAndRecord(
        notificationKey: String,
        textHash: Int,
        packageName: String,
        payment: ParseResult.Payment,
        nowMillis: Long,
    ): Boolean {
        seen.removeAll { nowMillis - it.atMillis > referenceWindowMillis }

        val duplicate = seen.any { s ->
            val age = nowMillis - s.atMillis
            val sameMoney = s.amountPaisa == payment.amountPaisa && s.direction == payment.direction
            when {
                s.notificationKey == notificationKey && s.textHash == textHash && age <= repostWindowMillis -> true
                payment.reference != null && s.reference == payment.reference -> true
                sameMoney && s.packageName != packageName && age <= crossAppWindowMillis -> true
                sameMoney && s.packageName == packageName && s.reference == null &&
                    payment.reference == null && age <= sameAppWindowMillis -> true
                else -> false
            }
        }
        if (!duplicate) {
            seen.addLast(
                Seen(notificationKey, textHash, packageName, payment.amountPaisa, payment.direction, payment.reference, nowMillis),
            )
            while (seen.size > capacity) seen.removeFirst()
        }
        return duplicate
    }
}
