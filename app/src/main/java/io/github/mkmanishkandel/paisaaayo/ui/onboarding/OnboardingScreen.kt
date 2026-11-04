package io.github.mkmanishkandel.paisaaayo.ui.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.service.TestPayment
import io.github.mkmanishkandel.paisaaayo.ui.components.HeroCoin
import io.github.mkmanishkandel.paisaaayo.ui.components.IconBadge
import io.github.mkmanishkandel.paisaaayo.ui.components.PrimaryButton
import io.github.mkmanishkandel.paisaaayo.ui.components.SecondaryButton
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalNepali
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalPaisaColors
import io.github.mkmanishkandel.paisaaayo.ui.theme.LocalReducedMotion
import io.github.mkmanishkandel.paisaaayo.ui.theme.Motion
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.util.AppLanguage
import io.github.mkmanishkandel.paisaaayo.util.SystemSettings
import kotlinx.coroutines.launch

private enum class Step { LANGUAGE, WELCOME, ACCESS, NOTIFY, BATTERY, TEST }

/**
 * First-run setup. One decision per screen, each with a clear primary action; every permission
 * screen re-checks its state when the user comes back from Android settings.
 */
@Composable
fun OnboardingScreen(
    onLanguagePicked: (String) -> Unit,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val steps = remember {
        Step.entries.filter { it != Step.NOTIFY || Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU }
    }
    // Saved across the activity re-creation that a language change triggers.
    var index by rememberSaveable { mutableIntStateOf(0) }
    val step = steps[index]
    val next: () -> Unit = { if (index < steps.lastIndex) index++ else onFinished() }
    val reduced = LocalReducedMotion.current

    var accessGranted by remember { mutableStateOf(SystemSettings.hasNotificationAccess(context)) }
    var notifyGranted by remember { mutableStateOf(SystemSettings.hasPostNotifications(context)) }
    var batteryOk by remember { mutableStateOf(SystemSettings.isIgnoringBatteryOptimizations(context)) }
    LifecycleResumeEffect(Unit) {
        accessGranted = SystemSettings.hasNotificationAccess(context)
        notifyGranted = SystemSettings.hasPostNotifications(context)
        batteryOk = SystemSettings.isIgnoringBatteryOptimizations(context)
        onPauseOrDispose { }
    }

    val paisa = LocalPaisaColors.current
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(paisa.heroGradientTop, paisa.heroGradientBottom))),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            TopBar(
                index = index,
                total = steps.size,
                onBack = if (index > 0) ({ index-- }) else null,
            )
            AnimatedContent(
                targetState = step,
                transitionSpec = {
                    if (reduced) {
                        fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                    } else {
                        val forward = targetState.ordinal > initialState.ordinal
                        (slideInHorizontally(tween(Motion.MEDIUM)) { if (forward) it / 4 else -it / 4 } + fadeIn(tween(Motion.MEDIUM))) togetherWith
                            fadeOut(tween(Motion.SHORT))
                    }
                },
                modifier = Modifier.weight(1f),
                label = "onboarding",
            ) { current ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                    verticalArrangement = Arrangement.spacedBy(Spacing.lg),
                ) {
                    when (current) {
                        Step.LANGUAGE -> LanguageStep(onLanguagePicked)
                        Step.WELCOME -> WelcomeStep()
                        Step.ACCESS -> AccessStep(accessGranted)
                        Step.NOTIFY -> SimpleStep(
                            Icons.Rounded.NotificationsActive,
                            stringResource(R.string.onb_notify_title),
                            stringResource(R.string.onb_notify_body),
                            done = if (notifyGranted) stringResource(R.string.onb_notify_granted) else null,
                        )
                        Step.BATTERY -> BatteryStep(batteryOk)
                        Step.TEST -> TestStep()
                    }
                }
            }
            BottomActions(
                step = step,
                accessGranted = accessGranted,
                notifyGranted = notifyGranted,
                batteryOk = batteryOk,
                onNext = next,
                onNotifyResult = { notifyGranted = it },
            )
        }
    }
}

@Composable
private fun TopBar(index: Int, total: Int, onBack: (() -> Unit)?) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).padding(horizontal = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        }
        // Segmented progress: one bar per step.
        Row(Modifier.weight(1f).padding(horizontal = Spacing.sm), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(total) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(if (i <= index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        val nepali = LocalNepali.current
        val label = stringResource(R.string.onb_step_of, index + 1, total)
        Text(
            if (nepali) io.github.mkmanishkandel.paisaaayo.parser.Money.toDevanagari(label) else label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = Spacing.md),
        )
    }
}

@Composable
private fun StepHeader(icon: ImageVector?, title: String, body: String) {
    if (icon != null) {
        IconBadge(icon, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, 64.dp)
    }
    Text(
        title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.semantics { heading() },
    )
    Text(body, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun DoneChip(text: String) {
    val paisa = LocalPaisaColors.current
    Row(
        Modifier.clip(CircleShape).background(paisa.creditContainer).padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = paisa.credit, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(Spacing.sm))
        Text(text, style = MaterialTheme.typography.labelLarge, color = paisa.credit)
    }
}

@Composable
private fun LanguageStep(onPick: (String) -> Unit) {
    val nepali = LocalNepali.current
    StepHeader(Icons.Rounded.Translate, stringResource(R.string.onb_lang_title), stringResource(R.string.onb_lang_body))
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
        LanguageCard(stringResource(R.string.lang_nepali), stringResource(R.string.lang_nepali_sample), nepali) { onPick(AppLanguage.NEPALI) }
        LanguageCard(stringResource(R.string.lang_english), stringResource(R.string.lang_english_sample), !nepali) { onPick(AppLanguage.ENGLISH) }
    }
}

@Composable
private fun LanguageCard(name: String, sample: String, selected: Boolean, onClick: () -> Unit) {
    val border = if (selected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick, role = Role.RadioButton),
        shape = MaterialTheme.shapes.large,
        border = border,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.padding(Spacing.lg).heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
                Text("“$sample”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            RadioButton(selected = selected, onClick = null)
        }
    }
}

@Composable
private fun WelcomeStep() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroCoin(size = 168.dp) }
    StepHeader(null, stringResource(R.string.onb_welcome_title), stringResource(R.string.onb_welcome_body))
    Feature(Icons.Rounded.WifiOff, stringResource(R.string.onb_point_offline_title), stringResource(R.string.onb_point_offline_body))
    Feature(Icons.Rounded.VolunteerActivism, stringResource(R.string.onb_point_free_title), stringResource(R.string.onb_point_free_body))
    Feature(Icons.Rounded.RecordVoiceOver, stringResource(R.string.onb_point_voice_title), stringResource(R.string.onb_point_voice_body))
}

@Composable
private fun Feature(icon: ImageVector, title: String, body: String) {
    Row(verticalAlignment = Alignment.Top) {
        IconBadge(icon, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, 40.dp)
        Spacer(Modifier.width(Spacing.md))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AccessStep(granted: Boolean) {
    val context = LocalContext.current
    StepHeader(Icons.Rounded.Lock, stringResource(R.string.onb_access_title), stringResource(R.string.onb_access_body))
    if (granted) {
        DoneChip(stringResource(R.string.onb_access_granted))
    } else {
        Text(stringResource(R.string.onb_access_steps), style = MaterialTheme.typography.bodyMedium, color = LocalPaisaColors.current.accentText)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ blocks notification access for apps installed outside an app store
            // until "Allow restricted settings" is chosen in App info.
            Text(stringResource(R.string.onb_restricted_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = { SystemSettings.openAppInfo(context) }, modifier = Modifier.heightIn(min = 48.dp)) {
                Text(stringResource(R.string.onb_app_info_button))
            }
        }
    }
}

@Composable
private fun SimpleStep(icon: ImageVector, title: String, body: String, done: String?) {
    StepHeader(icon, title, body)
    if (done != null) DoneChip(done)
}

@Composable
private fun BatteryStep(batteryOk: Boolean) {
    val context = LocalContext.current
    StepHeader(Icons.Rounded.BatteryChargingFull, stringResource(R.string.onb_battery_title), stringResource(R.string.onb_battery_body))
    if (batteryOk) DoneChip(stringResource(R.string.onb_battery_done))
    SystemSettings.autoStartBrand()?.let { brand ->
        Text(stringResource(R.string.onb_autostart_hint, brand), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        SecondaryButton(stringResource(R.string.onb_autostart_button), onClick = { SystemSettings.openAutoStart(context) }, icon = Icons.Rounded.RocketLaunch)
    }
}

@Composable
private fun TestStep() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { HeroCoin(size = 140.dp) }
    StepHeader(null, stringResource(R.string.onb_test_title), stringResource(R.string.onb_test_body))
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    SecondaryButton(stringResource(R.string.onb_test_button), onClick = { scope.launch { TestPayment.run(context) } }, icon = Icons.Rounded.PlayArrow)
}

@Composable
private fun BottomActions(
    step: Step,
    accessGranted: Boolean,
    notifyGranted: Boolean,
    batteryOk: Boolean,
    onNext: () -> Unit,
    onNotifyResult: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val notifyLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onNotifyResult(granted)
        if (granted) onNext()
    }
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.xl, vertical = Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val continueText = stringResource(R.string.action_continue)
        when (step) {
            Step.LANGUAGE, Step.WELCOME -> PrimaryButton(continueText, onNext)
            Step.ACCESS ->
                if (accessGranted) {
                    PrimaryButton(continueText, onNext)
                } else {
                    PrimaryButton(stringResource(R.string.onb_access_button), { SystemSettings.openNotificationAccess(context) })
                    SkipButton(onNext)
                }
            Step.NOTIFY ->
                if (notifyGranted) {
                    PrimaryButton(continueText, onNext)
                } else {
                    PrimaryButton(stringResource(R.string.onb_notify_button), {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            notifyLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    })
                    SkipButton(onNext)
                }
            Step.BATTERY ->
                if (batteryOk) {
                    PrimaryButton(continueText, onNext)
                } else {
                    PrimaryButton(stringResource(R.string.onb_battery_button), { SystemSettings.requestIgnoreBatteryOptimizations(context) })
                    SkipButton(onNext)
                }
            Step.TEST -> PrimaryButton(stringResource(R.string.onb_finish), onNext)
        }
    }
}

@Composable
private fun SkipButton(onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.heightIn(min = 48.dp)) {
        Text(
            stringResource(R.string.action_skip),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
