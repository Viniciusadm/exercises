package com.archieapps.exercises.ui.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.archieapps.exercises.ui.component.GhostButton
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun RegisterScreen(
    state: RegisterUiState,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(horizontal = Spacing.xl),
        verticalArrangement = Arrangement.Center,
    ) {
        Spacer(Modifier.height(Spacing.xxl))
        SectionLabel(text = "controle de treino")
        Spacer(Modifier.height(Spacing.sm))
        Text(text = "Criar conta", style = MaterialTheme.typography.displayMedium)
        Text(
            text = "Seus treinos ficam só na sua conta.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
        )

        Spacer(Modifier.height(Spacing.xl))

        IronTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = "Nome",
        )
        Spacer(Modifier.height(Spacing.md))
        IronTextField(
            value = state.email,
            onValueChange = onEmailChange,
            label = "E-mail",
            keyboardType = KeyboardType.Email,
        )
        Spacer(Modifier.height(Spacing.md))
        IronTextField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = "Senha",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            supportingText = if (state.passwordTooShort) "Use pelo menos 8 caracteres." else null,
        )
        Spacer(Modifier.height(Spacing.md))
        IronTextField(
            value = state.confirmation,
            onValueChange = onConfirmationChange,
            label = "Confirmar senha",
            keyboardType = KeyboardType.Password,
            isPassword = true,
            imeAction = ImeAction.Done,
            supportingText = if (state.confirmationMismatch) "As senhas não conferem." else null,
        )

        if (state.error != null) {
            Spacer(Modifier.height(Spacing.md))
            NoticeBanner(message = state.error, isError = true)
        }

        Spacer(Modifier.height(Spacing.xl))
        PrimaryButton(
            text = "Criar conta",
            onClick = onSubmit,
            enabled = state.canSubmit,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(Spacing.md))
        GhostButton(
            text = "Já tenho conta",
            onClick = onBackToLogin,
            enabled = !state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.xxl))
    }
}
