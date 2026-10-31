package com.rushit.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val dummyPayments = listOf(
    "RI172805456|COD|₹542|Pending",
    "RI172805455|UPI|₹386|Success",
    "RI172805454|Credit Card|₹724|Success",
    "RI172805453|Net Banking|₹218|Refunded"
)

val dummyPayouts = listOf(
    "Seller A|Oct 2026|₹1500|₹150|₹1350|Processing",
    "Seller B|Oct 2026|₹3400|₹340|₹3060|Paid",
    "Seller C|Oct 2026|₹800|₹80|₹720|Pending"
)

@Composable
fun AdminPaymentsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Payment & Refund History", "Local demo records", backAction = onBack)
        LazyColumn(
            Modifier.fillMaxSize().adminPageInsets(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dummyPayments) { payment ->
                val parts = payment.split('|')
                AdminSurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(parts[0], fontWeight = FontWeight.Bold)
                        Text(parts[3], color = if (parts[3] == "Success") AdminColorScheme.success else AdminColorScheme.secondaryText)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Method: ${parts[1]}", fontSize = 13.sp, color = AdminColorScheme.secondaryText)
                        Text("Amount: ${parts[2]}", fontSize = 13.sp, color = AdminColorScheme.primaryText)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminPayoutsScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        ScreenHeader("Settlements & Payouts", "Local demo records", backAction = onBack)
        LazyColumn(
            Modifier.fillMaxSize().adminPageInsets(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(dummyPayouts) { payout ->
                val parts = payout.split('|')
                AdminSurfaceCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(parts[0], fontWeight = FontWeight.Bold)
                        Text(parts[5], color = if (parts[5] == "Paid") AdminColorScheme.success else AdminColorScheme.secondaryText)
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Period: ${parts[1]}", fontSize = 13.sp, color = AdminColorScheme.secondaryText)
                        Text("Gross: ${parts[2]}", fontSize = 13.sp, color = AdminColorScheme.secondaryText)
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Platform Fee: ${parts[3]}", fontSize = 13.sp, color = AdminColorScheme.secondaryText)
                        Text("Payout: ${parts[4]}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AdminColorScheme.primaryText)
                    }
                }
            }
        }
    }
}
