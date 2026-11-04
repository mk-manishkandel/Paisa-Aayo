package io.github.mkmanishkandel.paisaaayo.ui

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.mkmanishkandel.paisaaayo.R
import io.github.mkmanishkandel.paisaaayo.container
import io.github.mkmanishkandel.paisaaayo.data.Settings
import io.github.mkmanishkandel.paisaaayo.ui.components.IconBadge
import io.github.mkmanishkandel.paisaaayo.ui.components.PrimaryButton
import io.github.mkmanishkandel.paisaaayo.ui.history.HistoryScreen
import io.github.mkmanishkandel.paisaaayo.ui.home.HomeScreen
import io.github.mkmanishkandel.paisaaayo.ui.onboarding.OnboardingScreen
import io.github.mkmanishkandel.paisaaayo.ui.settings.SettingsScreen
import io.github.mkmanishkandel.paisaaayo.ui.theme.Motion
import io.github.mkmanishkandel.paisaaayo.ui.theme.PaisaAayoTheme
import io.github.mkmanishkandel.paisaaayo.ui.theme.Spacing
import io.github.mkmanishkandel.paisaaayo.util.AppLanguage
import kotlinx.coroutines.launch

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

class MainActivity : AppCompatActivity() {

    private var unlocked by mutableStateOf(false)
    private var settingsLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        unlocked = savedInstanceState?.getBoolean(KEY_UNLOCKED) ?: false
        splash.setKeepOnScreenCondition { !settingsLoaded }

        setContent {
            val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
            val current = settings ?: return@setContent
            settingsLoaded = true

            LaunchedEffect(current.appLock) {
                // Hide content from screenshots and the recent-apps preview when the lock is on.
                if (current.appLock) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                }
            }

            PaisaAayoTheme(nepali = AppLanguage.isNepali(this, current)) {
                Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
                    if (current.appLock && !unlocked) {
                        LockScreen(onUnlock = { authenticate { unlocked = true } })
                        LaunchedEffect(Unit) { authenticate { unlocked = true } }
                    } else {
                        AppNavigation(current)
                    }
                }
            }
        }
    }

    @Composable
    private fun AppNavigation(settings: Settings) {
        val nav = rememberNavController()
        // Decided once, so finishing onboarding doesn't rebuild the graph underneath us.
        val start = rememberSaveable { if (settings.onboardingDone) Routes.HOME else Routes.ONBOARDING }
        val pickLanguage: (String) -> Unit = { tag ->
            lifecycleScope.launch {
                container.settings.setAppLanguage(tag)
                AppLanguage.apply(tag)
            }
        }
        NavHost(
            navController = nav,
            startDestination = start,
            enterTransition = { fadeIn(tween(Motion.MEDIUM)) },
            exitTransition = { fadeOut(tween(Motion.SHORT)) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onLanguagePicked = pickLanguage,
                    onFinished = {
                        lifecycleScope.launch { container.settings.setOnboardingDone(true) }
                        nav.navigate(Routes.HOME) { popUpTo(0) { inclusive = true } }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onOpenHistory = { nav.navigate(Routes.HISTORY) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                )
            }
            composable(Routes.HISTORY) { HistoryScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    settings = settings,
                    onBack = { nav.popBackStack() },
                    onLanguagePicked = pickLanguage,
                    onRerunSetup = { nav.navigate(Routes.ONBOARDING) { popUpTo(0) { inclusive = true } } },
                    authenticate = ::authenticate,
                )
            }
        }
    }

    /** Biometric or screen-lock (PIN/pattern/password) confirmation. */
    private fun authenticate(onSuccess: () -> Unit) {
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    // Any successful check also unlocks the session (e.g. right after enabling the lock).
                    unlocked = true
                    onSuccess()
                }
            },
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.lock_prompt_title))
            .setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
            .build()
        runCatching { prompt.authenticate(info) }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_UNLOCKED, unlocked)
    }

    override fun onStop() {
        super.onStop()
        // Re-lock whenever the app leaves the screen (but not on rotation / language change).
        if (!isChangingConfigurations) unlocked = false
    }

    private companion object {
        const val KEY_UNLOCKED = "unlocked"
    }
}

@Composable
private fun LockScreen(onUnlock: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.lg, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(Icons.Rounded.Lock, MaterialTheme.colorScheme.onPrimaryContainer, MaterialTheme.colorScheme.primaryContainer, 72.dp)
        Text(stringResource(R.string.lock_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
        PrimaryButton(stringResource(R.string.lock_button), onUnlock)
    }
}

