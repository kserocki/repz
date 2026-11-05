package com.repz.app.core.designsystem.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.repz.app.core.designsystem.component.RepzBanner
import com.repz.app.core.designsystem.component.RepzFieldError
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzPasswordField
import com.repz.app.core.designsystem.component.RepzTextField
import com.repz.app.core.designsystem.theme.RepzSpacing

/** `components-inputs` (DESIGN_SYSTEM §9 `RepzTextField`/`RepzPasswordField`/`RepzBanner`): every
 * field state (empty, filled, supporting text, error) and password visibility, plus a banner with
 * and without a body. */
@Composable
internal fun ComponentsInputsCatalogPage() {
    Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.lg)) {
        val emailLabel = "Email"
        val passwordLabel = "Password"
        val nameLabel = "Display name"
        val passwordHint = "At least 8 characters"
        val emailError = "Enter a valid email, like name@example.com"

        RepzTextField(value = "", onValueChange = {}, label = emailLabel)
        RepzTextField(value = "lena@example.com", onValueChange = {}, label = emailLabel)
        RepzTextField(value = "Lena", onValueChange = {}, label = nameLabel, supportingText = "1-50 characters")
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            RepzTextField(value = "not-an-email", onValueChange = {}, label = emailLabel, isError = true)
            RepzFieldError(emailError, modifier = Modifier.padding(start = RepzSpacing.lg))
        }

        var hiddenOne by remember { mutableStateOf(true) }
        RepzPasswordField(
            value = "hunter2",
            onValueChange = {},
            label = passwordLabel,
            isPasswordVisible = !hiddenOne,
            onPasswordVisibilityToggle = { hiddenOne = !hiddenOne },
            supportingText = passwordHint,
        )
        Column(verticalArrangement = Arrangement.spacedBy(RepzSpacing.xs)) {
            RepzPasswordField(
                value = "short",
                onValueChange = {},
                label = passwordLabel,
                isPasswordVisible = true,
                onPasswordVisibilityToggle = {},
                isError = true,
            )
            RepzFieldError(passwordHint, modifier = Modifier.padding(start = RepzSpacing.lg))
        }

        val registerBanner = "You need a connection to create an account"
        val loginOfflineTitle = "You're offline"
        val loginOfflineBody = "Logging in needs a connection. Once you're in, Repz works offline."
        RepzBanner(icon = RepzIcon.CloudOff, title = registerBanner)
        RepzBanner(icon = RepzIcon.CloudOff, title = loginOfflineTitle, body = loginOfflineBody)
    }
}
