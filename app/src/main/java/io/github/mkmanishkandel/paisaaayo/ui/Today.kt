package io.github.mkmanishkandel.paisaaayo.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.delay

/**
 * Start of the current day, kept up to date. Shop phones often leave the app open overnight,
 * so "today" must roll over at midnight, and after the clock or timezone changes (re-checked
 * whenever the screen comes back).
 */
@Composable
fun rememberStartOfToday(): Long {
    var start by remember { mutableLongStateOf(Format.startOfDay()) }
    LifecycleResumeEffect(Unit) {
        start = Format.startOfDay()
        onPauseOrDispose { }
    }
    LaunchedEffect(start) {
        val nextMidnight = Format.startOfDay(daysAgo = -1)
        delay((nextMidnight - System.currentTimeMillis()).coerceAtLeast(1_000L))
        start = Format.startOfDay()
    }
    return start
}
