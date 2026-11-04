package io.github.mkmanishkandel.paisaaayo.service

import android.content.ComponentName
import android.content.Context
import android.service.notification.NotificationListenerService
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.util.SystemSettings
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * Runs every 15 minutes (survives reboots via WorkManager):
 *  - re-binds the listener if Android silently dropped it
 *  - deletes history older than the user's retention period
 */
class WatchdogWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (SystemSettings.hasNotificationAccess(ctx) && !ListenerState.connected.value) {
            runCatching {
                NotificationListenerService.requestRebind(ComponentName(ctx, PaymentListenerService::class.java))
            }
        }
        val retentionDays = ctx.container.settings.settings.first().retentionDays
        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        ctx.container.database.payments().deleteBefore(cutoff)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WatchdogWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("listener_watchdog", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
