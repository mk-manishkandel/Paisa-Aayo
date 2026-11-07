package io.github.mkmanishkandel.paisaaayo.ui.settings

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.CallMade
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Gavel
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.mkmanishkandel.paisaaayo.BuildConfig
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.parser.Sources
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.data.Settings
import io.github.mkmanishkandel.paisaaayo.data.SpeechLanguageSetting
import io.github.mkmanishkandel.paisaaayo.service.TestPayment
import io.github.mkmanishkandel.paisaaayo.ui.Format
import io.github.mkmanishkandel.paisaaayo.ui.components.IconBadge
import io.github.mkmanishkandel.paisaaayo.ui.components.PaisaCard
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalNepali
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalPaisaColors
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.util.AppLanguage
import io.github.mkmanishkandel.paisaaayo.util.Links
import io.github.mkmanishkandel.paisaaayo.util.SystemSettings
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: Settings,
    onBack: () -> Unit,
    onLanguagePicked: (String) -> Unit,
    onRerunSetup: () -> Unit,
    authenticate: (onSuccess: () -> Unit) -> Unit,
) {
    val context = LocalContext.current
    val nepali = LocalNepali.current
    val repo = context.container.settings
    val dao = remember { context.container.database.payments() }
    val sources by remember { dao.sources() }.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    val hasVoicePack = remember { context.container.announcer.hasVoicePack }

    var hasAccess by remember { mutableStateOf(SystemSettings.hasNotificationAccess(context)) }
    var batteryOk by remember { mutableStateOf(SystemSettings.isIgnoringBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        hasAccess = SystemSettings.hasNotificationAccess(context)
        batteryOk = SystemSettings.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }
    val canLock = remember {
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_WEAK or DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), modifier = Modifier.semantics { heading() }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = Spacing.lg, end = Spacing.lg, bottom = Spacing.xxl),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            item {
                Section(stringResource(R.string.settings_section_language)) {
                    Label(stringResource(R.string.settings_app_language))
                    Segmented(
                        options = listOf(stringResource(R.string.lang_nepali), stringResource(R.string.lang_english)),
                        selected = if (nepali) 0 else 1,
                        onSelect = { onLanguagePicked(if (it == 0) AppLanguage.NEPALI else AppLanguage.ENGLISH) },
                    )
                    Label(stringResource(R.string.settings_speech_language))
                    Segmented(
                        options = listOf(
                            stringResource(R.string.speech_same_as_app),
                            stringResource(R.string.lang_nepali),
                            stringResource(R.string.lang_english),
                        ),
                        selected = settings.speechLanguage.ordinal,
                        onSelect = { scope.launch { repo.setSpeechLanguage(SpeechLanguageSetting.entries[it]) } },
                    )
                    Spacer(Modifier.heightIn(min = Spacing.sm))
                }
            }
            item {
                Section(stringResource(R.string.settings_section_announce)) {
                    SwitchRow(Icons.AutoMirrored.Rounded.CallMade, stringResource(R.string.settings_announce_debits), stringResource(R.string.settings_announce_debits_body), settings.announceDebits) {
                        scope.launch { repo.setAnnounceDebits(it) }
                    }
                    SwitchRow(Icons.Rounded.Storefront, stringResource(R.string.settings_announce_source), stringResource(R.string.settings_announce_source_body), settings.announceSource) {
                        scope.launch { repo.setAnnounceSource(it) }
                    }
                    SwitchRow(Icons.AutoMirrored.Rounded.VolumeUp, stringResource(R.string.settings_raise_volume), stringResource(R.string.settings_raise_volume_body), settings.raiseVolume) {
                        scope.launch { repo.setRaiseVolume(it) }
                    }
                    SwitchRow(
                        Icons.Rounded.RecordVoiceOver,
                        stringResource(R.string.settings_recorded_voice),
                        stringResource(if (hasVoicePack) R.string.settings_voice_pack_ok else R.string.settings_voice_pack_missing),
                        settings.useRecordedVoice && hasVoicePack,
                        enabled = hasVoicePack,
                    ) { scope.launch { repo.setUseRecordedVoice(it) } }
                    NavRow(Icons.Rounded.PlayArrow, stringResource(R.string.settings_test_voice), null) {
                        scope.launch { TestPayment.run(context) }
                    }
                }
            }
            item {
                Section(stringResource(R.string.settings_section_sources)) {
                    SwitchRow(Icons.Rounded.Sms, stringResource(R.string.settings_read_sms), stringResource(R.string.settings_read_sms_body), settings.readSms) {
                        scope.launch { repo.setReadSms(it) }
                    }
                    if (settings.readSms) {
                        Label(stringResource(R.string.settings_sms_senders))
                        if (settings.smsSenders.isEmpty()) {
                            Hint(stringResource(R.string.settings_sms_senders_empty))
                        } else {
                            Hint(stringResource(R.string.settings_sms_senders_hint))
                            settings.smsSenders.sortedBy { it.lowercase() }.forEach { sender ->
                                SwitchRow(Icons.Rounded.Sms, sender, null, sender !in settings.mutedSmsSenders) { on ->
                                    scope.launch { repo.setSmsSenderMuted(sender, !on) }
                                }
                            }
                        }
                    }
                    Label(stringResource(R.string.settings_apps))
                    Hint(stringResource(R.string.settings_sources_hint))
                    // SMS are chosen per sender above, so the messaging app itself isn't listed here.
                    sources.filterNot { Sources.isSmsApp(it.packageName) }.forEach { source ->
                        SwitchRow(Icons.Rounded.Apps, source.sourceLabel, null, source.packageName !in settings.mutedPackages) { on ->
                            scope.launch { repo.setPackageMuted(source.packageName, !on) }
                        }
                    }
                }
            }
            item {
                Section(stringResource(R.string.settings_section_reliability)) {
                    SwitchRow(Icons.Rounded.Notifications, stringResource(R.string.settings_keep_alive), stringResource(R.string.settings_keep_alive_body), settings.keepAliveNotification) {
                        scope.launch { repo.setKeepAlive(it) }
                    }
                    NavRow(
                        Icons.Rounded.NotificationsActive,
                        stringResource(R.string.settings_notification_access),
                        stringResource(if (hasAccess) R.string.status_allowed else R.string.status_not_allowed),
                        warn = !hasAccess,
                    ) { SystemSettings.openNotificationAccess(context) }
                    NavRow(
                        Icons.Rounded.BatteryChargingFull,
                        stringResource(R.string.settings_battery),
                        stringResource(if (batteryOk) R.string.battery_unrestricted else R.string.battery_restricted),
                        warn = !batteryOk,
                    ) { SystemSettings.requestIgnoreBatteryOptimizations(context) }
                    if (SystemSettings.autoStartBrand() != null) {
                        NavRow(Icons.Rounded.RocketLaunch, stringResource(R.string.settings_autostart), null) { SystemSettings.openAutoStart(context) }
                    }
                }
            }
            item {
                Section(stringResource(R.string.settings_section_privacy)) {
                    SwitchRow(
                        Icons.Rounded.Fingerprint,
                        stringResource(R.string.settings_app_lock),
                        stringResource(if (canLock) R.string.settings_app_lock_body else R.string.settings_app_lock_unavailable),
                        settings.appLock,
                        enabled = canLock,
                    ) { on -> authenticate { scope.launch { repo.setAppLock(on) } } }
                    Label(stringResource(R.string.settings_retention))
                    val retention = listOf(30, 90, 365)
                    Segmented(
                        options = retention.map { stringResource(R.string.retention_days, Format.number(it.toLong(), nepali)) },
                        selected = retention.indexOf(settings.retentionDays).coerceAtLeast(0),
                        onSelect = { scope.launch { repo.setRetentionDays(retention[it]) } },
                    )
                    NavRow(Icons.Rounded.DeleteForever, stringResource(R.string.settings_clear_history), null, warn = true) { confirmClear = true }
                }
            }
            item {
                Section(stringResource(R.string.settings_section_about)) {
                    NavRow(Icons.Rounded.WifiOff, stringResource(R.string.settings_no_internet), null, chevron = false) {}
                    NavRow(Icons.Rounded.Code, stringResource(R.string.settings_source_code), null) {
                        Links.open(context, Links.REPO)
                    }
                    NavRow(Icons.Rounded.PrivacyTip, stringResource(R.string.privacy_policy), null) {
                        Links.open(context, Links.PRIVACY_POLICY)
                    }
                    NavRow(Icons.Rounded.Gavel, stringResource(R.string.settings_license), null) {
                        Links.open(context, Links.LICENSE)
                    }
                    NavRow(Icons.Rounded.RestartAlt, stringResource(R.string.settings_rerun_setup), null, onClick = onRerunSetup)
                    NavRow(Icons.Rounded.Info, stringResource(R.string.settings_version, BuildConfig.VERSION_NAME), null, chevron = false) {}
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_history_confirm_title)) },
            text = { Text(stringResource(R.string.clear_history_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    scope.launch {
                        dao.deleteAll()
                        repo.clearSmsSenders()
                    }
                }) { Text(stringResource(R.string.action_delete), color = LocalPaisaColors.current.debit) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

@Composable
private fun Section(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = LocalPaisaColors.current.accentText,
            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.sm).semantics { heading() },
        )
        PaisaCard { Column(Modifier.padding(vertical = Spacing.xs), content = content) }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.md, bottom = Spacing.sm),
    )
}

@Composable
private fun Hint(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.sm),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = Spacing.lg)) {
        options.forEachIndexed { i, label ->
            SegmentedButton(
                selected = i == selected,
                onClick = { onSelect(i) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.heightIn(min = 48.dp),
            ) { Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1) }
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    body: String?,
    checked: Boolean,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(icon, MaterialTheme.colorScheme.onSurfaceVariant, MaterialTheme.colorScheme.surfaceVariant, 40.dp)
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (body != null) Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(Spacing.sm))
        Switch(checked = checked, onCheckedChange = null, enabled = enabled)
    }
}

@Composable
private fun NavRow(
    icon: ImageVector,
    title: String,
    value: String?,
    warn: Boolean = false,
    chevron: Boolean = true,
    onClick: () -> Unit,
) {
    val paisa = LocalPaisaColors.current
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = chevron, role = Role.Button, onClick = onClick)
            .padding(horizontal = Spacing.lg, vertical = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconBadge(
            icon,
            if (warn) paisa.debit else MaterialTheme.colorScheme.onSurfaceVariant,
            if (warn) paisa.debitContainer else MaterialTheme.colorScheme.surfaceVariant,
            40.dp,
        )
        Spacer(Modifier.width(Spacing.md))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            if (value != null) {
                Text(value, style = MaterialTheme.typography.bodySmall, color = if (warn) paisa.debit else MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (chevron) {
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
