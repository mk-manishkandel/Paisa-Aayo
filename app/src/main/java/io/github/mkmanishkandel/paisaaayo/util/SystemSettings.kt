package io.github.mkmanishkandel.paisaaayo.util

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import io.github.mkmanishkandel.paisaaayo.service.PaymentListenerService

/** Deep links into Android / OEM settings screens, each with safe fallbacks. */
object SystemSettings {

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun hasPostNotifications(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    /** True when installed by an app store (Play etc.). Sideloaded apps hit Android 13's "restricted settings". */
    fun isInstalledFromStore(context: Context): Boolean = runCatching {
        val installer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getInstallerPackageName(context.packageName)
        }
        installer != null && installer !in SIDELOAD_INSTALLERS
    }.getOrDefault(false)

    private val SIDELOAD_INSTALLERS = setOf(
        "com.google.android.packageinstaller", "com.android.packageinstaller", "com.android.shell",
    )

    fun isIgnoringBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService(PowerManager::class.java) ?: return true
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun openNotificationAccess(context: Context) {
        val component = ComponentName(context, PaymentListenerService::class.java)
        val detail = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
        } else {
            null
        }
        if (detail == null || !tryStart(context, detail)) {
            tryStart(context, Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    }

    fun openAppInfo(context: Context) {
        tryStart(context, Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:${context.packageName}".toUri()))
    }

    /** Uses the direct system dialog; falls back to the battery optimisation list. */
    @SuppressLint("BatteryLife") // Core feature: a payment speaker must run in the background.
    fun requestIgnoreBatteryOptimizations(context: Context) {
        val direct = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, "package:${context.packageName}".toUri())
        if (!tryStart(context, direct)) {
            tryStart(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    /** Manufacturer name if this phone is known to need a separate auto-start toggle, else null. */
    fun autoStartBrand(): String? {
        val m = Build.MANUFACTURER.lowercase()
        return AUTOSTART.keys.firstOrNull { it in m }?.replaceFirstChar { it.titlecase() }
    }

    fun openAutoStart(context: Context) {
        val m = Build.MANUFACTURER.lowercase()
        val candidates = AUTOSTART.entries.firstOrNull { it.key in m }?.value.orEmpty()
        for ((pkg, cls) in candidates) {
            if (tryStart(context, Intent().setComponent(ComponentName(pkg, cls)))) return
        }
        openAppInfo(context)
    }

    private val AUTOSTART: Map<String, List<Pair<String, String>>> = mapOf(
        "xiaomi" to listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "redmi" to listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "poco" to listOf("com.miui.securitycenter" to "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        "oppo" to listOf(
            "com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity",
            "com.coloros.safecenter" to "com.coloros.safecenter.startupapp.StartupAppListActivity",
            "com.oppo.safe" to "com.oppo.safe.permission.startup.StartupAppListActivity",
        ),
        "realme" to listOf("com.coloros.safecenter" to "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        "oneplus" to listOf("com.oneplus.security" to "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
        "vivo" to listOf(
            "com.vivo.permissionmanager" to "com.vivo.permissionmanager.activity.BgStartUpManagerActivity",
            "com.iqoo.secure" to "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager",
        ),
        "huawei" to listOf("com.huawei.systemmanager" to "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        "honor" to listOf("com.hihonor.systemmanager" to "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        "asus" to listOf("com.asus.mobilemanager" to "com.asus.mobilemanager.autostart.AutoStartActivity"),
        "infinix" to listOf("com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"),
        "tecno" to listOf("com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"),
        "itel" to listOf("com.transsion.phonemaster" to "com.cyin.himgr.autostart.AutoStartActivity"),
        "samsung" to listOf("com.samsung.android.lool" to "com.samsung.android.sm.battery.ui.BatteryActivity"),
    )

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        if (context !is android.app.Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
