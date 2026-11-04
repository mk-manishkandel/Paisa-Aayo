package io.github.mkmanishkandel.paisaaayo.service

import android.app.Notification
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import io.github.mkmanishkandel.paisaaayo.parser.NotificationInput
import io.github.mkmanishkandel.paisaaayo.parser.Sources

/** Everything we keep from a posted notification. Built on the binder thread, processed later. */
data class NotificationSnapshot(
    val key: String,
    val tag: String?,
    val packageName: String,
    val input: NotificationInput,
    val textHash: Int,
    val appInfo: ApplicationInfo?,
    /** Android 15+ replaced the content because it looked like it held a one-time code. */
    val redacted: Boolean = false,
)

object NotificationExtractor {

    /**
     * Android 15+ hides notifications that look like they contain a code (an OTP, but also many
     * 6–8 character transaction IDs) from listeners like ours, replacing the text with this
     * system string. We can't read those; we can only notice that it happened.
     */
    private val redactedMessages: Set<String> by lazy {
        val system = runCatching {
            val res = android.content.res.Resources.getSystem()
            @android.annotation.SuppressLint("DiscouragedApi") // No public constant exists for this string.
            val id = res.getIdentifier("redacted_notification_message", "string", "android")
            if (id != 0) res.getString(id) else null
        }.getOrNull()
        setOfNotNull(system, "Sensitive notification content hidden")
    }

    fun isRedacted(body: String): Boolean = body.trim() in redactedMessages

    fun extract(sbn: StatusBarNotification): NotificationSnapshot? {
        val n = sbn.notification ?: return null
        // Group summaries repeat their children; ongoing ones are progress bars / music players.
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return null
        if (sbn.isOngoing) return null
        val extras = n.extras ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val senders = mutableListOf<String>()
        val parts = LinkedHashSet<String>()

        runCatching { NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(n) }
            .getOrNull()
            ?.let { style ->
                style.conversationTitle?.toString()?.let(senders::add)
                style.messages.forEach { m -> m.person?.name?.toString()?.let(senders::add) }
                style.messages.maxByOrNull { it.timestamp }?.text?.toString()?.let(parts::add)
            }

        val big = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        big?.let(parts::add)
        // EXTRA_TEXT is usually a truncated copy of EXTRA_BIG_TEXT — skip it if so.
        if (text != null && (big == null || !big.startsWith(text.removeSuffix("…").trim()))) parts.add(text)
        if (parts.isEmpty()) {
            extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.forEach { parts.add(it.toString()) }
            n.tickerText?.toString()?.let(parts::add)
        }
        val body = parts.filter { it.isNotBlank() }.joinToString(" ")
        if (body.isBlank()) return null

        @Suppress("DEPRECATION")
        val appInfo = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable("android.appInfo", ApplicationInfo::class.java)
            } else {
                extras.getParcelable("android.appInfo") as? ApplicationInfo
            }
        }.getOrNull()

        return NotificationSnapshot(
            key = sbn.key ?: "${sbn.packageName}:${sbn.id}",
            tag = sbn.tag,
            packageName = sbn.packageName,
            input = NotificationInput(sbn.packageName, title, body, senders),
            textHash = (title + body).hashCode(),
            appInfo = appInfo,
            redacted = isRedacted(body),
        )
    }

    /** Human label for the source shown in history. */
    fun sourceLabel(pm: PackageManager, snapshot: NotificationSnapshot): String {
        if (Sources.isSmsApp(snapshot.packageName)) {
            val sender = snapshot.input.title.trim()
            return if (sender.isNotEmpty()) "SMS · $sender" else "SMS"
        }
        val fromInfo = runCatching { snapshot.appInfo?.loadLabel(pm)?.toString() }.getOrNull()
        val fromPm = fromInfo ?: runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(snapshot.packageName, 0)).toString()
        }.getOrNull()
        return fromPm?.takeIf { it.isNotBlank() && it != snapshot.packageName }
            ?: Sources.fallbackName(snapshot.packageName)
    }
}
