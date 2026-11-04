package io.github.mkmanishkandel.paisaaayo.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.ui.MainActivity

/** The quiet "listening" notification that makes Android far less likely to kill the listener. */
object KeepAlive {
    const val CHANNEL_KEEPALIVE = "listener"
    const val CHANNEL_TEST = "test_payments"
    private const val NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        val keepAlive = NotificationChannel(
            CHANNEL_KEEPALIVE,
            context.getString(R.string.channel_keepalive),
            NotificationManager.IMPORTANCE_MIN,
        ).apply {
            description = context.getString(R.string.channel_keepalive_desc)
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        val test = NotificationChannel(
            CHANNEL_TEST,
            context.getString(R.string.channel_test),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            setShowBadge(false)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannels(listOf(keepAlive, test))
    }

    private fun build(context: Context): Notification {
        val open = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(context, CHANNEL_KEEPALIVE)
            .setSmallIcon(R.drawable.ic_stat_listening)
            .setContentTitle(context.getString(R.string.keepalive_title))
            .setContentText(context.getString(R.string.keepalive_text))
            .setContentIntent(open)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun start(service: Service) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                service.startForeground(NOTIFICATION_ID, build(service), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                service.startForeground(NOTIFICATION_ID, build(service))
            }
        } catch (e: Exception) {
            // Android 12+ may refuse while the app is in the background; the listener still works.
            Log.w(TAG, "Could not start keep-alive notification", e)
        }
    }

    fun stop(service: Service) {
        runCatching { service.stopForeground(Service.STOP_FOREGROUND_REMOVE) }
    }

    private const val TAG = "KeepAlive"
}
