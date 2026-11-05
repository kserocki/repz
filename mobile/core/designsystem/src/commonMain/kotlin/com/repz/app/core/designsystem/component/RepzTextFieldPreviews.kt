package com.repz.app.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.core.designsystem.theme.RepzTheme

@Composable
private fun RepzTextFieldShowcase() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        RepzTextField(value = "", onValueChange = {}, label = "Email")
        RepzTextField(value = "lena@example.com", onValueChange = {}, label = "Email")
        RepzTextField(value = "Lena", onValueChange = {}, label = "Display name", supportingText = "1-50 characters")
        RepzTextField(
            value = "not-an-email",
            onValueChange = {},
            label = "Email",
            errorText = "Enter a valid email, like name@example.com",
        )

        var hidden by remember { mutableStateOf(true) }
        RepzPasswordField(
            value = "hunter2",
            onValueChange = {},
            label = "Password",
            isPasswordVisible = !hidden,
            onPasswordVisibilityToggle = { hidden = !hidden },
            supportingText = "At least 8 characters",
        )
        RepzPasswordField(
            value = "short",
            onValueChange = {},
            label = "Password",
            isPasswordVisible = true,
            onPasswordVisibilityToggle = {},
            errorText = "At least 8 characters",
        )
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
