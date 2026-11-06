package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzTextFieldShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzTextField(value = "", onValueChange = {}, label = "Email")
        RepzTextField(value = "lena@example.com", onValueChange = {}, label = "Email")
        RepzTextField(value = "Lena", onValueChange = {}, label = "Display name", supportingText = "1-50 characters")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            RepzTextField(value = "not-an-email", onValueChange = {}, label = "Email", isError = true)
            RepzFieldError("Enter a valid email, like name@example.com", modifier = Modifier.padding(start = RepzSpacing.lg))
        }

        var hidden by remember { mutableStateOf(true) }
        RepzPasswordField(
            value = "hunter2",
            onValueChange = {},
            label = "Password",
            isPasswordVisible = !hidden,
            onPasswordVisibilityToggle = { hidden = !hidden },
            supportingText = "At least 8 characters",
        )
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            RepzPasswordField(
                value = "short",
                onValueChange = {},
                label = "Password",
                isPasswordVisible = true,
                onPasswordVisibilityToggle = {},
                isError = true,
            )
            RepzFieldError("At least 8 characters", modifier = Modifier.padding(start = RepzSpacing.lg))
        }
    }
}

@Preview
@Composable
private fun RepzTextFieldLightPreview() {
    RepzTheme(darkTheme = false) { RepzTextFieldShowcase() }
}

@Preview
@Composable
private fun RepzTextFieldDarkPreview() {
    RepzTheme(darkTheme = true) { RepzTextFieldShowcase() }
}

// DESIGN_SYSTEM §9, board 04 §21: the filled style, every state the outlined showcase above covers.
@Composable
private fun RepzFilledTextFieldShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzTextField(value = "", onValueChange = {}, label = "Email", style = RepzTextFieldStyle.Filled)
        RepzTextField(
            value = "alex@repz.app",
            onValueChange = {},
            label = "Email",
            style = RepzTextFieldStyle.Filled,
        )
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            RepzTextField(
                value = "not-an-email",
                onValueChange = {},
                label = "Email",
                isError = true,
                style = RepzTextFieldStyle.Filled,
            )
            RepzFieldError("Enter a valid email, like name@example.com", modifier = Modifier.padding(start = RepzSpacing.lg))
        }

        var hiddenFilled by remember { mutableStateOf(true) }
        RepzPasswordField(
            value = "hunter2",
            onValueChange = {},
            label = "Password",
            isPasswordVisible = !hiddenFilled,
            onPasswordVisibilityToggle = { hiddenFilled = !hiddenFilled },
            style = RepzTextFieldStyle.Filled,
        )
    }
}

@Preview
@Composable
private fun RepzFilledTextFieldLightPreview() {
    RepzTheme(darkTheme = false) { RepzFilledTextFieldShowcase() }
}

@Preview
@Composable
private fun RepzFilledTextFieldDarkPreview() {
    RepzTheme(darkTheme = true) { RepzFilledTextFieldShowcase() }
}
