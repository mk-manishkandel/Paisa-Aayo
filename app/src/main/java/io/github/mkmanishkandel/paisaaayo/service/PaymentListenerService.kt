package io.github.mkmanishkandel.paisaaayo.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import io.github.mkmanishkandel.paisaaayo.BuildConfig
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.data.PaymentEntity
import io.github.mkmanishkandel.paisaaayo.data.Settings
import io.github.mkmanishkandel.paisaaayo.parser.Deduplicator
import io.github.mkmanishkandel.paisaaayo.parser.Direction
import io.github.mkmanishkandel.paisaaayo.parser.ParseResult
import io.github.mkmanishkandel.paisaaayo.parser.PaymentParser
import io.github.mkmanishkandel.paisaaayo.parser.Sources
import io.github.mkmanishkandel.paisaaayo.util.AppLanguage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Receives every notification on the phone (Android binds this after the user grants
 * notification access), keeps only real payments and announces them.
 */
class PaymentListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Two stages so a slow or stuck voice can never delay (or drop) recording a payment:
    // parsing + saving is fast; announcements wait their turn on their own queue.
    private val queue = Channel<NotificationSnapshot>(capacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val announcements = Channel<Announcement>(capacity = 32, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val dedup = Deduplicator()
    private var connected = false

    override fun onCreate() {
        super.onCreate()
        // One consumer per stage, so payments are saved and announced strictly in arrival order.
        scope.launch {
            for (snapshot in queue) {
                try {
                    handle(snapshot)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to process notification", e)
                }
            }
        }
        scope.launch {
            for (a in announcements) {
                try {
                    announce(this@PaymentListenerService, a.amountPaisa, a.direction, a.source, container.settings.settings.first())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to announce", e)
                }
            }
        }
        scope.launch {
            container.settings.settings.map { it.keepAliveNotification }.distinctUntilChanged().collect { enabled ->
                if (connected) applyKeepAlive(enabled)
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true
        ListenerState.set(true)
        scope.launch { applyKeepAlive(container.settings.settings.first().keepAliveNotification) }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        connected = false
        ListenerState.set(false)
        // Ask Android to bind us again (e.g. after an OEM battery saver unbinds us).
        requestRebind(ComponentName(this, PaymentListenerService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val snapshot = sbn?.let { runCatching { NotificationExtractor.extract(it) }.getOrNull() } ?: return
        queue.trySend(snapshot)
    }

    override fun onDestroy() {
        ListenerState.set(false)
        scope.cancel()
        queue.close()
        announcements.close()
        super.onDestroy()
    }

    private fun applyKeepAlive(enabled: Boolean) {
        if (enabled) KeepAlive.start(this) else KeepAlive.stop(this)
    }

    private suspend fun handle(snapshot: NotificationSnapshot) {
        val settings = container.settings.settings.first()
        val isTest = snapshot.packageName == packageName
        if (isTest && snapshot.tag != TestPayment.TAG) return // our own keep-alive etc.
        if (!isTest && !isSourceAllowed(snapshot.packageName, settings)) return
        if (snapshot.redacted) {
            if (!isTest) noteHidden(snapshot)
            return
        }

        val input = if (isTest) snapshot.input.copy(packageName = TEST_PACKAGE) else snapshot.input
        val result = PaymentParser.parse(input)
        if (result !is ParseResult.Payment) {
            if (BuildConfig.DEBUG) Log.d(TAG, "Ignored ${snapshot.packageName}: $result")
            return
        }

        val now = System.currentTimeMillis()
        val dao = container.database.payments()
        if (!isTest) {
            if (dedup.checkAndRecord(snapshot.key, snapshot.textHash, snapshot.packageName, result, now)) return
            val ref = result.reference
            if (ref != null && dao.referenceExists(ref, now - DAY_MS)) return
        }

        val label = if (isTest) getString(R.string.test_source) else NotificationExtractor.sourceLabel(packageManager, snapshot)
        dao.insert(
            PaymentEntity(
                amountPaisa = result.amountPaisa,
                direction = result.direction.name,
                sourceLabel = label,
                packageName = snapshot.packageName,
                reference = result.reference,
                timestamp = now,
                isTest = isTest,
            ),
        )

        if (result.direction == Direction.DEBIT && !settings.announceDebits && !isTest) return
        announcements.trySend(Announcement(result.amountPaisa, result.direction, label.takeIf { settings.announceSource && !isTest }))
    }

    private class Announcement(val amountPaisa: Long, val direction: Direction, val source: String?)

    /**
     * Android hid this notification's text. If it came from a payment app, remember it so Home can
     * tell the shopkeeper to check that app. Hidden SMS are usually real OTPs, so they're skipped.
     */
    private suspend fun noteHidden(snapshot: NotificationSnapshot) {
        val pkg = snapshot.packageName
        if (Sources.isBlockedPackage(pkg) || Sources.isSmsApp(pkg)) return
        if (!Sources.isKnownPaymentApp(pkg) && !container.database.payments().hasPaymentsFrom(pkg)) return
        // The hidden copy carries Android's own app info, so name the app from its package instead.
        val label = NotificationExtractor.sourceLabel(packageManager, snapshot.copy(appInfo = null))
        container.settings.recordHidden(label, System.currentTimeMillis())
    }

    private fun isSourceAllowed(packageName: String, settings: Settings): Boolean {
        if (packageName in settings.mutedPackages) return false
        if (!settings.readSms && Sources.isSmsApp(packageName)) return false
        return true
    }

    companion object {
        private const val TAG = "PaymentListener"
        private const val TEST_PACKAGE = "paisa.aayo.test"
        private const val DAY_MS = 24 * 60 * 60 * 1000L

        suspend fun announce(context: Context, amountPaisa: Long, direction: Direction, source: String?, settings: Settings) {
            context.container.announcer.announce(
                amountPaisa = amountPaisa,
                direction = direction,
                source = source,
                language = AppLanguage.speechLanguage(context, settings),
                useRecordedVoice = settings.useRecordedVoice,
                raiseVolume = settings.raiseVolume,
            )
        }
    }
}
