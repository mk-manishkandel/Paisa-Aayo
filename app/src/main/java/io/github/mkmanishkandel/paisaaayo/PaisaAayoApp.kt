package io.github.mkmanishkandel.paisaaayo

import android.app.Application
import android.content.Context
import io.github.mkmanishkandel.paisaaayo.data.AppDatabase
import io.github.mkmanishkandel.paisaaayo.data.SettingsRepository
import io.github.mkmanishkandel.paisaaayo.service.Announcer
import io.github.mkmanishkandel.paisaaayo.service.KeepAlive
import io.github.mkmanishkandel.paisaaayo.service.WatchdogWorker

/** Tiny manual dependency container — no DI framework needed for an app this size. */
class AppContainer(context: Context) {
    val database: AppDatabase by lazy { AppDatabase.create(context) }
    val settings = SettingsRepository(context)
    val announcer: Announcer by lazy { Announcer(context) }
}

class PaisaAayoApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        KeepAlive.createChannels(this)
        WatchdogWorker.schedule(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as PaisaAayoApp).container
