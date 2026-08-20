package com.archieapps.exercises

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.archieapps.exercises.core.di.AppContainer
import com.archieapps.exercises.ui.design.ExercisesTheme
import com.archieapps.exercises.ui.feature.auth.LoginScreen
import com.archieapps.exercises.ui.feature.auth.LoginViewModel
import com.archieapps.exercises.ui.nav.ExercisesNavHost

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    /** O aviso de fim de descanso chega por notificação, então a permissão é pedida na abertura. */
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

    if (settings.isLoggedIn) {
        ExercisesNavHost(container)
    } else {
        val viewModel: LoginViewModel = viewModel(
            factory = viewModelFactory {
                initializer { LoginViewModel(container.authRepository) }
            }
        )
        val state by viewModel.state.collectAsStateWithLifecycle()

        Box(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            LoginScreen(
                state = state,
                onEmailChange = viewModel::onEmailChange,
                onPasswordChange = viewModel::onPasswordChange,
                onSubmit = viewModel::submit,
            )
        }
    }
}
