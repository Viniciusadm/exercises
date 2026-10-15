package com.archieapps.exercises

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.exercises.core.di.AppContainer
import com.archieapps.exercises.core.rest.OpenSessionBus
import com.archieapps.exercises.core.rest.RestNotifications
import com.archieapps.exercises.ui.design.ExercisesTheme
import com.archieapps.exercises.ui.feature.auth.LoginScreen
import com.archieapps.exercises.ui.feature.auth.LoginViewModel
import com.archieapps.exercises.ui.feature.auth.RegisterScreen
import com.archieapps.exercises.ui.feature.auth.RegisterViewModel
import com.archieapps.exercises.ui.nav.ExercisesNavHost

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIntent(intent)
        askForNotifications()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        val container = (application as ExercisesApplication).container

        setContent {
            ExercisesTheme {
                ExercisesRoot(container)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == RestNotifications.ACTION_OPEN_SESSION) OpenSessionBus.request()
    }

    private fun askForNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

        val granted = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

        if (!granted) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

@Composable
private fun ExercisesRoot(container: AppContainer) {
    val settings by container.settings.state.collectAsStateWithLifecycle()

    Box(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars.union(WindowInsets.displayCutout))
    ) {
        if (settings.isLoggedIn) {
            ExercisesNavHost(container)
        } else {
            LoginRoute(container)
        }
    }
}

@Composable
private fun LoginRoute(container: AppContainer) {
    var registering by rememberSaveable { mutableStateOf(false) }

    if (registering) {
        RegisterRoute(container, onBackToLogin = { registering = false })
        return
    }

    val viewModel: LoginViewModel = viewModel(
        factory = viewModelFactory {
            initializer { LoginViewModel(container.authRepository) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    LoginScreen(
        state = state,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onCreateAccount = { registering = true },
    )
}

@Composable
private fun RegisterRoute(container: AppContainer, onBackToLogin: () -> Unit) {
    val viewModel: RegisterViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RegisterViewModel(container.authRepository) }
        }
    )
    val state by viewModel.state.collectAsStateWithLifecycle()

    BackHandler(onBack = onBackToLogin)

    RegisterScreen(
        state = state,
        onNameChange = viewModel::onNameChange,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onConfirmationChange = viewModel::onConfirmationChange,
        onSubmit = viewModel::submit,
        onBackToLogin = onBackToLogin,
    )
}
