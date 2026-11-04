package io.github.mkmanishkandel.paisaaayo.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaymentParserTest {

    private fun parse(body: String, title: String = "", pkg: String = "com.f1soft.esewa", senders: List<String> = emptyList()) =
        PaymentParser.parse(NotificationInput(pkg, title, body, senders))

    private fun assertCredit(expectedPaisa: Long, result: ParseResult, ref: String? = null) {
        val payment = result as? ParseResult.Payment ?: error("Expected payment, got $result")
        assertEquals(Direction.CREDIT, payment.direction)
        assertEquals(expectedPaisa, payment.amountPaisa)
        if (ref != null) assertEquals(ref, payment.reference)
    }

    private fun assertDebit(expectedPaisa: Long, result: ParseResult) {
        val payment = result as? ParseResult.Payment ?: error("Expected payment, got $result")
        assertEquals(Direction.DEBIT, payment.direction)
        assertEquals(expectedPaisa, payment.amountPaisa)
    }

    private fun assertIgnored(reason: IgnoreReason, result: ParseResult) {
        assertEquals(ParseResult.Ignored(reason), result)
    }

    // ---- credits ----

    @Test fun walletReceived() =
        assertCredit(50_000, parse("You have received Rs. 500.00 from 98XXXXXX12.", "eSewa"))

    @Test fun merchantQrPayment() =
        assertCredit(125_050, parse("Rs.1,250.50 has been received from Ram Thapa via Fonepay. Txn ID: 123456789012", pkg = "com.fonepay.merchant"), "123456789012")

    @Test fun bankSmsCredited() =
        assertCredit(
            100_000_00,
            parse("Dear Customer, your A/C 01XXXX234 has been credited by NPR 1,00,000.00 on 04/11. Bal: NPR 2,50,000.00", "NABIL_ALERT", pkg = "com.google.android.apps.messaging"),
        )

    @Test fun balanceBeforeAmountIsSkipped() =
        assertCredit(
            1_000_00,
            parse("A/C ##234 Avl Bal NPR 25,000.00 after credit of NPR 1,000.00 received from X", "GIBL", pkg = "com.android.mms"),
        )

    @Test fun transferredToYourAccountIsCredit() =
        assertCredit(75_000, parse("Rs 750 has been transferred to your account by Sita."))

    @Test fun amountBeforeVerb() =
        assertCredit(30_000, parse("NPR 300 deposited successfully"))

    @Test fun nepaliCredit() =
        assertCredit(150_000, parse("तपाईंको खातामा रु १,५००  जम्मा भयो।"))

    @Test fun walletLoaded() =
        assertCredit(200_000, parse("Rs 2000 has been loaded to your wallet successfully."))

    @Test fun transactionSuccessFrom() =
        assertCredit(45_000, parse("Transaction successful of NPR 450 from Hari Bahadur. Ref: 99887766554"))

    // ---- debits ----

    @Test fun sentToIsDebit() = assertDebit(20_000, parse("You sent Rs 200 to Shyam."))

    @Test fun debitedFromAccount() =
        assertDebit(5_000_00, parse("NPR 5,000.00 has been debited from your A/C 01XXX. Bal NPR 300", "SBL_ALERT", pkg = "com.android.mms"))

    // ---- rejections ----

    @Test fun remarkInjectionIsIgnored() {
        // The real amount is 10; the remark claims 5000. Only the real amount must be read.
        assertCredit(1_000, parse("You have received Rs 10 from 98XXXXXXXX. Remarks: received Rs 5000 bonus"))
    }

    @Test fun personalNumberSms() =
        assertIgnored(
            IgnoreReason.PERSONAL_SENDER,
            parse("I have sent you Rs 5000, it is credited", "+977 9812345678", pkg = "com.google.android.apps.messaging"),
        )

    @Test fun personalNameSms() =
        assertIgnored(
            IgnoreReason.PERSONAL_SENDER,
            parse("Rs 5000 received, check", "Ram Bahadur", pkg = "com.samsung.android.messaging"),
        )

    @Test fun chatAppsBlocked() =
        assertIgnored(IgnoreReason.BLOCKED_APP, parse("Rs 5000 received", "Friend", pkg = "com.whatsapp"))

    @Test fun otpIgnored() =
        assertIgnored(IgnoreReason.OTP, parse("Your OTP is 123456 for receiving Rs 500. Do not share."))

    @Test fun moneyRequestIgnored() =
        assertIgnored(IgnoreReason.REQUEST, parse("Sita has requested Rs 500 from you."))

    @Test fun failedIgnored() =
        assertIgnored(IgnoreReason.FAILED, parse("Your transaction of Rs 500 received from X has failed."))

    @Test fun telecomIgnored() =
        assertIgnored(IgnoreReason.TELECOM, parse("Ncell: Rs 100 balance received on your number."))

    @Test fun topUpIgnored() =
        assertIgnored(IgnoreReason.TOP_UP, parse("Top-up of Rs 100 received successfully."))

    @Test fun promoIgnored() =
        assertIgnored(IgnoreReason.PROMOTION, parse("Get flat Rs 100 cashback! Offer valid till Sunday."))

    @Test fun noCurrencyIgnored() =
        assertIgnored(IgnoreReason.NO_CURRENCY, parse("You received a new message from Hari"))

    @Test fun balanceOnlyIgnored() =
        assertIgnored(IgnoreReason.NO_DIRECTION, parse("Your available balance is Rs 5,000."))

    // ---- helpers ----

    @Test fun devanagariDigitsNormalised() =
        assertEquals("Rs 1,500", PaymentParser.normalize("Rs   १,५००"))

    @Test fun paisaConversion() {
        assertEquals(123_450L, PaymentParser.toPaisa("1,234.5"))
        assertEquals(100L, PaymentParser.toPaisa("1."))
        assertNull(PaymentParser.toPaisa("0"))
        assertNull(PaymentParser.toPaisa("99999999999"))
    }

    @Test fun phoneNormalisation() {
        assertEquals("9812345678", PaymentParser.phoneDigits("+977-9812345678"))
        assertEquals("9812345678", PaymentParser.phoneDigits("009779812345678"))
        assertNull(PaymentParser.phoneDigits("NABIL"))
    }
}
