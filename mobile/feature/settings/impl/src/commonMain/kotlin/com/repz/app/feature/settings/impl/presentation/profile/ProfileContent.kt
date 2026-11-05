package com.repz.app.feature.settings.impl.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.repz.app.core.designsystem.component.RepzAlertDialog
import com.repz.app.core.designsystem.component.RepzAvatar
import com.repz.app.core.designsystem.component.RepzIcon
import com.repz.app.core.designsystem.component.RepzListItem
import com.repz.app.core.designsystem.component.RepzListItemTone
import com.repz.app.core.designsystem.theme.RepzSpacing
import com.repz.app.feature.settings.impl.generated.resources.Res
import com.repz.app.feature.settings.impl.generated.resources.log_out_dialog_body
import com.repz.app.feature.settings.impl.generated.resources.log_out_dialog_cancel
import com.repz.app.feature.settings.impl.generated.resources.log_out_dialog_confirm
import com.repz.app.feature.settings.impl.generated.resources.log_out_dialog_title
import com.repz.app.feature.settings.impl.generated.resources.profile_log_out
import org.jetbrains.compose.resources.stringResource

/**
 * Stateless (ADR-0005), board 13: the account header (Cookie9 avatar, name, email — the lifetime
 * stats line is a later follow-up, no workout data yet) and directly below it the Log out row
 * (no section caption, no other rows, no top app bar yet: Profile has nothing else to show).
 * [RepzAlertDialog] shows over it when [ProfileUiState.isLogOutDialogVisible].
 * No status-bar inset padding here: the app shell already consumes the scaffold's padding around
 * every tab root.
 */
@Composable
fun ProfileContent(
    state: ProfileUiState,
    onIntent: (ProfileIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            ProfileHeader(account = state.account)
            RepzListItem(
                headline = stringResource(Res.string.profile_log_out),
                onClick = { onIntent(ProfileIntent.LogOutClicked) },
                modifier = Modifier.testTag("profile_log_out"),
                leadingIcon = RepzIcon.Logout,
                tone = RepzListItemTone.Destructive,
            )
        }

        if (state.isLogOutDialogVisible) {
            RepzAlertDialog(
                title = stringResource(Res.string.log_out_dialog_title),
                text = stringResource(Res.string.log_out_dialog_body),
                confirmText = stringResource(Res.string.log_out_dialog_confirm),
                onConfirm = { onIntent(ProfileIntent.LogOutConfirmed) },
                dismissText = stringResource(Res.string.log_out_dialog_cancel),
                onDismissRequest = { onIntent(ProfileIntent.LogOutDismissed) },
                confirmButtonModifier = Modifier.testTag("log_out_confirm"),
                dismissButtonModifier = Modifier.testTag("log_out_cancel"),
                isConfirmDestructive = true,
            )
        }
    }
}

/**
 * Before any profile is cached, the header keeps the avatar's own height (no spinner, no error
 * copy); the name/email column simply isn't there yet, replacing itself as soon as the Room row arrives.
 */
@Composable
private fun ProfileHeader(
    account: ProfileAccount?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = RepzSpacing.lg, vertical = RepzSpacing.md),
        horizontalArrangement = Arrangement.spacedBy(RepzSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RepzAvatar(monogram = account?.monogram)
        if (account != null) {
            Column {
                Text(
                    text = account.displayName,
                    modifier = Modifier.testTag("profile_display_name"),
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                )
                Text(
                    text = account.email,
                    modifier = Modifier.testTag("profile_email"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
