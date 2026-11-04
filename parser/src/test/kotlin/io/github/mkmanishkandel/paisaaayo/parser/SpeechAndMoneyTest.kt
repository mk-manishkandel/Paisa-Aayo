package io.github.mkmanishkandel.paisaaayo.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SpeechAndMoneyTest {

    @Test fun nepaliWords() {
        assertEquals("पाँच सय", Speech.nepaliWords(500))
        assertEquals("एक हजार दुई सय पचास", Speech.nepaliWords(1_250))
        assertEquals("एक लाख पच्चीस हजार", Speech.nepaliWords(125_000))
        assertEquals("दुई करोड तीन लाख", Speech.nepaliWords(20_300_000))
        assertEquals("एक सय पचास करोड", Speech.nepaliWords(1_500_000_000))
    }

    @Test fun englishWords() {
        assertEquals("one thousand two hundred fifty", Speech.englishWords(1_250))
        assertEquals("one lakh twenty-five thousand", Speech.englishWords(125_000))
    }

    @Test fun sentences() {
        assertEquals("पाँच सय रुपैयाँ प्राप्त भयो", Speech.sentence(SpeechLanguage.NEPALI, 50_000, Direction.CREDIT))
        assertEquals("पाँच सय रुपैयाँ पचास पैसा प्राप्त भयो", Speech.sentence(SpeechLanguage.NEPALI, 50_050, Direction.CREDIT))
        assertEquals("Received five hundred rupees on eSewa", Speech.sentence(SpeechLanguage.ENGLISH, 50_000, Direction.CREDIT, "eSewa"))
        assertEquals("Paid one rupee", Speech.sentence(SpeechLanguage.ENGLISH, 100, Direction.DEBIT))
    }

    @Test fun clipsAreAllInPack() {
        for (amount in listOf(1L, 99L, 100L, 1_234_56L, 98_76_54_321_00L, 50L)) {
            for (d in Direction.entries) {
                Speech.nepaliClips(amount, d).forEach { assertTrue(it in Speech.ALL_CLIPS, "missing clip $it") }
            }
        }
        assertEquals(
            listOf("ne_1", "ne_hajar", "ne_2", "ne_saya", "ne_50", "ne_rupaiya", "ne_prapta_bhayo"),
            Speech.nepaliClips(125_000, Direction.CREDIT),
        )
    }

    @Test fun moneyFormatting() {
        assertEquals("12,34,567", Money.format(1_234_567_00))
        assertEquals("1,250.50", Money.format(125_050))
        assertEquals("९९९", Money.format(99_900, devanagariDigits = true))
        assertEquals("0.00", Money.format(0, alwaysShowPaisa = true))
    }

    @Test fun dedupReference() {
        val d = Deduplicator()
        val p = ParseResult.Payment(10_000, Direction.CREDIT, "123456789")
        assertFalse(d.checkAndRecord("k1", 1, "a", p, 0))
        assertTrue(d.checkAndRecord("k2", 2, "b", p, 1_000))
    }

    @Test fun dedupKeepsTwoCustomersWithDifferentRefs() {
        val d = Deduplicator()
        assertFalse(d.checkAndRecord("k1", 1, "a", ParseResult.Payment(10_000, Direction.CREDIT, "111111111"), 0))
        assertFalse(d.checkAndRecord("k2", 2, "a", ParseResult.Payment(10_000, Direction.CREDIT, "222222222"), 2_000))
    }

    @Test fun dedupCrossChannel() {
        val d = Deduplicator()
        assertFalse(d.checkAndRecord("k1", 1, "wallet", ParseResult.Payment(10_000, Direction.CREDIT, null), 0))
        assertTrue(d.checkAndRecord("k2", 2, "sms", ParseResult.Payment(10_000, Direction.CREDIT, "333333333"), 20_000))
        // Well after the window, the same amount is a new payment.
        assertFalse(d.checkAndRecord("k3", 3, "sms", ParseResult.Payment(10_000, Direction.CREDIT, null), 120_000))
    }

    @Test fun dedupKeepsSameAmountFromTwoWalletsWithDifferentRefs() {
        // Two customers pay Rs 100 within seconds, one on eSewa and one on Khalti.
        val d = Deduplicator()
        assertFalse(d.checkAndRecord("k1", 1, "com.f1soft.esewa", ParseResult.Payment(10_000, Direction.CREDIT, "111111111"), 0))
        assertFalse(d.checkAndRecord("k2", 2, "com.khalti", ParseResult.Payment(10_000, Direction.CREDIT, "222222222"), 10_000))
    }

    @Test fun dedupWalletAndBankSmsWithDifferentRefsIsOnePayment() {
        // The bank SMS for a wallet payment usually carries its own reference.
        val d = Deduplicator()
        assertFalse(d.checkAndRecord("k1", 1, "com.f1soft.esewa", ParseResult.Payment(10_000, Direction.CREDIT, "111111111"), 0))
        assertTrue(d.checkAndRecord("k2", 2, "com.google.android.apps.messaging", ParseResult.Payment(10_000, Direction.CREDIT, "999999999"), 10_000))
    }

    @Test fun otherPaisaAayoBuildsAreIgnored() {
        val r = PaymentParser.parse(
            NotificationInput("io.github.mkmanishkandel.paisaaayo.debug", "Paisa Aayo", "You have received Rs 500.00 from Paisa Aayo test.", emptyList()),
        )
        assertEquals(ParseResult.Ignored(IgnoreReason.BLOCKED_APP), r)
    }

    @Test fun knownPaymentApps() {
        assertTrue(Sources.isKnownPaymentApp("com.f1soft.esewa"))
        assertTrue(Sources.isKnownPaymentApp("com.khalti"))
        assertFalse(Sources.isKnownPaymentApp("com.android.shell"))
        assertFalse(Sources.isKnownPaymentApp("com.google.android.apps.messaging"))
    }

    @Test fun dedupRepost() {
        val d = Deduplicator()
        val p = ParseResult.Payment(10_000, Direction.CREDIT, null)
        assertFalse(d.checkAndRecord("k1", 7, "a", p, 0))
        assertTrue(d.checkAndRecord("k1", 7, "a", p, 60_000))
    }
}
