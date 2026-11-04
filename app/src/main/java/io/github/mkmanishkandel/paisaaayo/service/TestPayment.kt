package io.github.mkmanishkandel.paisaaayo.service

import android.annotation.SuppressLint
import android.content.Context
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.parser.Direction
import io.github.mkmanishkandel.paisaaayo.util.SystemSettings
import kotlinx.coroutines.flow.first

/**
 * Plays a sample Rs 500 payment. When the listener is running, a real (silent) notification is
 * posted so the whole pipeline is exercised end-to-end; otherwise the voice is played directly.
 */
object TestPayment {
    const val TAG = "paisa_aayo_test"
    private const val AMOUNT_PAISA = 500_00L
    private const val NOTIFICATION_ID = 2001

    @SuppressLint("MissingPermission") // Guarded by hasPostNotifications().
    suspend fun run(context: Context) {
        if (ListenerState.connected.value && SystemSettings.hasPostNotifications(context)) {
            val notification = NotificationCompat.Builder(context, KeepAlive.CHANNEL_TEST)
                .setSmallIcon(R.drawable.ic_stat_listening)
                .setContentTitle(context.getString(R.string.app_name))
                // Fixed English text so the parser recognises it whatever the UI language is.
                .setContentText("You have received Rs 500.00 from Paisa Aayo test.")
                .setSilent(true)
                .setAutoCancel(true)
                .setTimeoutAfter(15_000)
                .build()
            NotificationManagerCompat.from(context).notify(TAG, NOTIFICATION_ID, notification)
        } else {
            val settings = context.container.settings.settings.first()
            PaymentListenerService.announce(context, AMOUNT_PAISA, Direction.CREDIT, null, settings)
        }
    }
}
