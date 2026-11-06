package io.github.mkmanishkandel.paisaaayo.ui.home

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.service.ListenerState
import io.github.mkmanishkandel.paisaaayo.service.PaymentListenerService
import io.github.mkmanishkandel.paisaaayo.service.TestPayment
import io.github.mkmanishkandel.paisaaayo.ui.Format
import io.github.mkmanishkandel.paisaaayo.ui.rememberStartOfToday
import io.github.mkmanishkandel.paisaaayo.ui.components.EmptyState
import io.github.mkmanishkandel.paisaaayo.ui.components.LiveDot
import io.github.mkmanishkandel.paisaaayo.ui.components.PaisaCard
import io.github.mkmanishkandel.paisaaayo.ui.components.PaymentRow
import io.github.mkmanishkandel.paisaaayo.ui.components.PrimaryButton
import io.github.mkmanishkandel.paisaaayo.ui.components.SectionHeader
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalNepali
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalPaisaColors
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.ui.theme.tabular
import io.github.mkmanishkandel.paisaaayo.util.SystemSettings
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight

private enum class ListenStatus { LISTENING, RECONNECTING, OFF }

@Composable
fun HomeScreen(onOpenHistory: () -> Unit, onOpenSettings: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { context.container.database.payments() }
    val todayStart = rememberStartOfToday()
    val recent by remember { dao.recent(8) }.collectAsStateWithLifecycle(emptyList())
    val todayTotal by remember(todayStart) { dao.creditTotalSince(todayStart) }.collectAsStateWithLifecycle(0L)
    val todayCount by remember(todayStart) { dao.creditCountSince(todayStart) }.collectAsStateWithLifecycle(0)

    val connected by ListenerState.connected.collectAsState()
    var hasAccess by remember { mutableStateOf(SystemSettings.hasNotificationAccess(context)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = SystemSettings.hasNotificationAccess(context)
        onPauseOrDispose { }
    }
    val status = when {
        !hasAccess -> ListenStatus.OFF
        connected -> ListenStatus.LISTENING
        else -> ListenStatus.RECONNECTING
    }
    val scope = rememberCoroutineScope()
    val settings by remember { context.container.settings.settings }.collectAsStateWithLifecycle(null)
    val hidden = settings?.takeIf {
        it.lastHiddenAt > it.hiddenDismissedAt && System.currentTimeMillis() - it.lastHiddenAt < HIDDEN_NOTICE_MS
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing),
        contentPadding = PaddingValues(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
    ) {
        item {
            Row(Modifier.fillMaxWidth().heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                IconButton(onClick = onOpenHistory) {
                    Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.nav_history))
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.nav_settings))
                }
            }
        }
        item {
            StatusCard(status) {
                if (status == ListenStatus.RECONNECTING) {
                    NotificationListenerService.requestRebind(ComponentName(context, PaymentListenerService::class.java))
                } else {
                    SystemSettings.openNotificationAccess(context)
                }
            }
        }
        if (hidden != null) {
            item {
                HiddenNotificationCard(hidden.lastHiddenSource, hidden.lastHiddenAt) {
                    scope.launch { context.container.settings.dismissHidden(hidden.lastHiddenAt) }
                }
            }
        }
        item { TodayCard(todayTotal, todayCount) { scope.launch { TestPayment.run(context) } } }
        item {
            SectionHeader(stringResource(R.string.home_recent)) {
                TextButton(onClick = onOpenHistory, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.home_see_all), color = LocalPaisaColors.current.accentText)
                }
            }
        }
        item {
            PaisaCard(Modifier.animateContentSize()) {
                if (recent.isEmpty()) {
                    EmptyState(Icons.AutoMirrored.Rounded.ReceiptLong, stringResource(R.string.home_empty_title), stringResource(R.string.home_empty_body))
                } else {
                    recent.forEachIndexed { i, payment ->
                        if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(start = 72.dp))
                        PaymentRow(payment)
                    }
                }
            }
        }
        item { Spacer(Modifier.heightIn(min = Spacing.lg)) }
    }
}

@Composable
private fun StatusCard(status: ListenStatus, onFix: () -> Unit) {
    val paisa = LocalPaisaColors.current
    val (color, title, body) = when (status) {
        ListenStatus.LISTENING -> Triple(paisa.credit, R.string.home_status_listening, R.string.home_status_listening_body)
        ListenStatus.RECONNECTING -> Triple(paisa.accentText, R.string.home_status_reconnecting, R.string.home_status_reconnecting_body)
        ListenStatus.OFF -> Triple(paisa.debit, R.string.home_status_off, R.string.home_status_off_body)
    }
    PaisaCard {
        Row(
            Modifier
                .padding(Spacing.lg)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LiveDot(color)
            Spacer(Modifier.width(Spacing.md))
            Column(Modifier.weight(1f)) {
                Text(stringResource(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(stringResource(body), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (status != ListenStatus.LISTENING) {
                Spacer(Modifier.width(Spacing.sm))
                PrimaryButton(stringResource(R.string.home_fix), onFix, modifier = Modifier.width(112.dp))
            }
        }
    }
}

private const val HIDDEN_NOTICE_MS = 24 * 60 * 60 * 1000L

/** Android 15+ hid a payment app's notification from us; tell the shopkeeper to check that app. */
@Composable
private fun HiddenNotificationCard(source: String, at: Long, onDismiss: () -> Unit) {
    val nepali = LocalNepali.current
    val paisa = LocalPaisaColors.current
    PaisaCard {
        Column(
            Modifier
                .padding(Spacing.lg)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VisibilityOff, contentDescription = null, tint = paisa.accentText)
                Spacer(Modifier.width(Spacing.sm))
                Text(
                    stringResource(R.string.home_hidden_title, source),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Text(
                stringResource(R.string.home_hidden_body, source, Format.time(at, nepali)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 48.dp).align(Alignment.End)) {
                Text(stringResource(R.string.home_hidden_dismiss), color = paisa.accentText)
            }
        }
    }
}

@Composable
private fun TodayCard(totalPaisa: Long, count: Int, onTest: () -> Unit) {
    val nepali = LocalNepali.current
    val scheme = MaterialTheme.colorScheme
    // Solid gold in both themes. Ink on gold is ~9:1; the 75 % ink secondary text is still ~5.5:1.
    val gold = scheme.primary
    val ink = scheme.onPrimary
    val inkSoft = ink.copy(alpha = 0.75f)
    PaisaCard(container = gold, borderColor = gold) {
        Box(Modifier.fillMaxWidth()) {
            // Brand mark as a faint watermark in the corner.
            Icon(
                painterResource(R.drawable.ic_brand_mark),
                contentDescription = null,
                tint = ink.copy(alpha = 0.08f),
                modifier = Modifier.align(Alignment.TopEnd).offset(x = 28.dp, y = (-20).dp).size(150.dp),
            )
            Column(
                Modifier.fillMaxWidth().padding(Spacing.xl),
                verticalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                Text(
                    stringResource(R.string.home_today_received),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = inkSoft,
                )
                Text(
                    Format.amount(totalPaisa, nepali),
                    style = MaterialTheme.typography.displayMedium.tabular,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                )
                Text(
                    pluralStringResource(R.plurals.payments_count, count, Format.number(count.toLong(), nepali)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = inkSoft,
                )
                Spacer(Modifier.size(Spacing.md))
                Button(
                    onClick = onTest,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = ink, contentColor = gold),
                ) {
                    Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(Spacing.sm))
                    Text(stringResource(R.string.home_test), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}
