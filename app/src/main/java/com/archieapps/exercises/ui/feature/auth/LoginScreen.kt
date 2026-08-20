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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.archieapps.exercises.BuildConfig
import com.archieapps.exercises.ui.component.NoticeBanner
import com.archieapps.exercises.ui.component.PrimaryButton
import com.archieapps.exercises.ui.component.SectionLabel
import com.archieapps.exercises.ui.design.IronBorder
import com.archieapps.exercises.ui.design.IronPrimary
import com.archieapps.exercises.ui.design.IronSurface
import com.archieapps.exercises.ui.design.IronTextMuted
import com.archieapps.exercises.ui.design.IronTextSecondary
import com.archieapps.exercises.ui.design.Spacing

@Composable
fun LoginScreen(
    state: LoginUiState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
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
        Text(text = "Entrar", style = MaterialTheme.typography.displayMedium)
        Text(
            text = "Use a mesma conta do painel.",
            style = MaterialTheme.typography.bodyMedium,
            color = IronTextSecondary,
        )

        Spacer(Modifier.height(Spacing.xl))

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
            imeAction = ImeAction.Done,
        )

        if (state.error != null) {
            Spacer(Modifier.height(Spacing.md))
            NoticeBanner(message = state.error, isError = true)
        }

        Spacer(Modifier.height(Spacing.xl))
        PrimaryButton(
            text = "Entrar",
            onClick = onSubmit,
            enabled = state.canSubmit,
            loading = state.loading,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(Spacing.lg))

        Text(
            text = "Servidor: ${BuildConfig.BASE_URL}",
            style = MaterialTheme.typography.labelMedium,
            color = IronTextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
fun IronTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    isPassword: Boolean = false,
    supportingText: String? = null,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        textStyle = MaterialTheme.typography.bodyLarge,
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        supportingText = supportingText?.let { { Text(it, style = MaterialTheme.typography.labelMedium) } },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = IronPrimary,
            unfocusedBorderColor = IronBorder,
            focusedContainerColor = IronSurface,
            unfocusedContainerColor = IronSurface,
            focusedLabelColor = IronPrimary,
            unfocusedLabelColor = IronTextMuted,
        ),
    )
}
