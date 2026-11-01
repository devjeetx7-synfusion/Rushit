package com.rushit.app.ui.screens.login

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rushit.app.BuildConfig
import com.rushit.app.R
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Muted
import java.net.URI

/**
 * The Terms of Service / Privacy Policy line shared by every sign-in screen.
 *
 * A link is only ever opened when it is a usable HTTPS URL, so an unconfigured build explains the
 * missing policy instead of opening a blank browser tab. The failure explanation is a dialog rather
 * than a colour change, because a policy that cannot be read must not look like a dead link.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AuthPolicyFooter(
    modifier: Modifier = Modifier,
    horizontalPadding: Int = 12,
) {
    val uriHandler = LocalUriHandler.current
    var dialog by rememberSaveable { mutableStateOf<String?>(null) }
    var dialogTitle by rememberSaveable { mutableStateOf("") }

    fun openPolicy(url: String, title: String) {
        dialogTitle = title
        val valid = runCatching { URI(url).let { it.scheme == "https" && !it.host.isNullOrBlank() } }
            .getOrDefault(false)
        dialog = if (!valid) {
            "$title is not configured in this build. Please contact the RushIt team for the current policy."
        } else {
            try {
                uriHandler.openUri(url)
                null
            } catch (error: Exception) {
                "Couldn't open $title. Check that a browser is installed and try again."
            }
        }
    }

    Row(
        modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Center,
    ) {
        RushIcon(
            R.drawable.ic_lock_keyhole,
            null,
            Modifier
                .padding(top = 2.dp)
                .size(19.dp),
            Muted,
        )
        Spacer(Modifier.width(8.dp))
        FlowRow(
            Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                "By continuing, you agree to our",
                style = MaterialTheme.typography.bodySmall,
                color = Muted,
            )
            Text(
                "Terms of Service",
                style = MaterialTheme.typography.bodySmall,
                color = DeepRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClickLabel = "Open Terms of Service") {
                        openPolicy(BuildConfig.TERMS_URL, "Terms of Service")
                    },
            )
            Text("and", style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(
                "Privacy Policy.",
                style = MaterialTheme.typography.bodySmall,
                color = DeepRed,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .clickable(role = Role.Button, onClickLabel = "Open Privacy Policy") {
                        openPolicy(BuildConfig.PRIVACY_URL, "Privacy Policy")
                    },
            )
        }
    }

    dialog?.let { message ->
        AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text(dialogTitle) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { dialog = null }) { Text("Got it") } },
            containerColor = CardWhite,
        )
    }
}
