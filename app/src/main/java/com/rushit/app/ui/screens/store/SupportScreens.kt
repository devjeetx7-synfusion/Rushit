package com.rushit.app.ui.screens.store

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.ui.components.MessageCard
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted

/** No notification provider is configured; don't imply an empty inbox is a working service. */
@Composable
fun NotificationsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Notifications", onBack)
        MessageCard("Notifications aren't connected", "There is no notification service configured in this build. You can view device-local demo orders in Orders.", Modifier.padding(16.dp), icon = R.drawable.ic_notification_01)
    }
}

@Composable
fun HelpScreen(onBack: () -> Unit) {
    val faqs = listOf(
        "Are orders real?" to "Checkout creates a demo order stored on this device. No payment, rider or real delivery is involved.",
        "How do I track an order?" to "Open Orders and select a demo order. The preview controls change only its local simulated status; there is no live GPS.",
        "Can I get a refund?" to "Demo checkout does not charge a payment. Payment and refund services are not configured in this build.",
        "How do I sign in?" to "Sign in using your mobile number with a Firebase SMS verification code, or tap Continue with Google.",
        "Where can I get support?" to "No support contact or live support integration has been configured for this build."
    )
    var expanded by rememberSaveable { mutableStateOf(-1) }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Help & Support", onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Frequently asked questions", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.height(12.dp))
            faqs.forEachIndexed { index, (question, answer) ->
                Surface(shape = RoundedCornerShape(16.dp), color = CardWhite, border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(Modifier.fillMaxWidth().semantics { stateDescription = if (expanded == index) "Expanded" else "Collapsed" }
                            .clickable(role = Role.Button) { expanded = if (expanded == index) -1 else index }
                            .padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(question, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, color = Ink)
                            RushIcon(R.drawable.ic_arrow_down_01, null, Modifier.size(20.dp), Coral)
                        }
                        AnimatedVisibility(visible = expanded == index) {
                            Text(answer, Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp), style = MaterialTheme.typography.bodyMedium, color = Muted)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
