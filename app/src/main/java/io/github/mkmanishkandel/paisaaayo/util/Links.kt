package io.github.mkmanishkandel.paisaaayo.util

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri

/** Public web pages. Opened in the browser; the app itself has no internet permission. */
object Links {
    const val REPO = "https://github.com/mk-manishkandel/Paisa-Aayo"
    const val PRIVACY_POLICY = "$REPO/blob/main/PRIVACY.md"
    const val LICENSE = "$REPO/blob/main/LICENSE"

    fun open(context: Context, url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
