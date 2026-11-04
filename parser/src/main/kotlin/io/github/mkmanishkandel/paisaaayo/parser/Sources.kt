package io.github.mkmanishkandel.paisaaayo.parser

/** Knowledge about which apps post notifications and how to name them. */
object Sources {

    /** Email clients: statements and newsletters mention amounts but are never live payments. */
    private val EMAIL_APPS = setOf(
        "com.google.android.gm", "com.google.android.gm.lite", "com.android.email",
        "com.samsung.android.email.provider", "com.microsoft.office.outlook",
        "com.yahoo.mobile.client.android.mail", "ch.protonmail.android", "me.proton.android.mail",
        "com.fsck.k9", "net.thunderbird.android", "com.readdle.spark", "com.easilydo.mail",
        "me.bluemail.mail", "com.zoho.mail", "com.tutanota.android",
    )

    /** Chat, social and browser apps: anyone can type "I sent you Rs 500" there. */
    private val SOCIAL_PREFIXES = listOf(
        "com.whatsapp", "com.facebook", "com.instagram", "com.zhiliaoapp", "com.ss.android.ugc",
        "com.twitter", "com.x.android", "com.snapchat", "org.telegram", "org.thunderdog.challegram",
        "com.viber", "com.discord", "com.slack", "com.microsoft.teams", "com.skype", "jp.naver.line",
        "com.kakao.talk", "com.tencent.mm", "com.imo.android", "com.reddit", "com.pinterest",
        "com.linkedin", "com.tumblr", "com.bereal", "com.google.android.youtube",
        "com.android.chrome", "com.chrome", "org.mozilla", "com.opera", "com.brave",
        "com.duckduckgo", "com.UCMobile", "com.mi.globalbrowser", "com.miui.browser",
        "com.heytap.browser", "com.vivo.browser", "com.sec.android.app.sbrowser",
        "com.transsion.phoenix", "com.android.browser", "com.microsoft.emmx",
        // Other Paisa Aayo builds (debug/release side by side): their test payment isn't real money.
        "io.github.mkmanishkandel.paisaaayo",
    )

    private val SMS_APPS = setOf(
        "com.google.android.apps.messaging", "com.android.mms", "com.android.messaging",
        "com.samsung.android.messaging", "com.miui.mms", "com.coloros.mms", "com.oplus.mms",
        "com.oneplus.mms", "com.huawei.message", "com.sonyericsson.conversations", "com.truecaller",
    )

    /** Package keyword → display name, used when the system can't give us an app label. */
    private val KNOWN_NAMES = listOf(
        "esewa" to "eSewa", "khalti" to "Khalti", "fonepay" to "Fonepay", "imepay" to "IME Pay",
        "connectips" to "ConnectIPS", "nchl" to "NCHL", "prabhu" to "Prabhu Pay",
        "nabil" to "Nabil Bank", "nmb" to "NMB Bank", "siddhartha" to "Siddhartha Bank",
        "globalime" to "Global IME Bank", "sanima" to "Sanima Bank", "laxmi" to "Laxmi Sunrise Bank",
        "nicasia" to "NIC Asia Bank", "adbl" to "ADBL", "rbb" to "RBB", "cellpay" to "CellPay",
        "namastepay" to "Namaste Pay", "himalayan" to "Himalayan Bank", "kumari" to "Kumari Bank",
        "everest" to "Everest Bank", "citizens" to "Citizens Bank", "prime" to "Prime Bank",
        "machhapuchchhre" to "Machhapuchchhre Bank", "sbi" to "Nepal SBI Bank",
    )

    fun isBlockedPackage(packageName: String): Boolean =
        packageName in EMAIL_APPS || SOCIAL_PREFIXES.any { packageName.startsWith(it) }

    fun isSmsApp(packageName: String): Boolean {
        if (packageName in SMS_APPS) return true
        val p = packageName.lowercase()
        return "mms" in p || "messag" in p || ".sms" in p
    }

    /** True for well-known Nepali wallet and bank apps (matched by package keyword). */
    fun isKnownPaymentApp(packageName: String): Boolean {
        val lower = packageName.lowercase()
        return KNOWN_NAMES.any { (key, _) -> key in lower }
    }

    /** Best-effort human name for a package when no app label is available. */
    fun fallbackName(packageName: String): String {
        val lower = packageName.lowercase()
        KNOWN_NAMES.firstOrNull { (key, _) -> key in lower }?.let { return it.second }
        val last = packageName.substringAfterLast('.')
        return last.replaceFirstChar { it.titlecase() }
    }
}
