import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'r') as f:
    content = f.read()

# Add state for payment method
# Near the top of CheckoutScreen

payment_state = """    val itemsPaise = state.totals.totalPaise
    var selectedPaymentMethod by remember { androidx.compose.runtime.mutableStateOf("Cash on Delivery") }
    val paymentMethods = listOf("Cash on Delivery", "UPI", "Credit Card", "Net Banking", "Wallet")
    var showPaymentDemo by remember { androidx.compose.runtime.mutableStateOf(false) }"""

content = re.sub(
    r'    val itemsPaise = state.totals.totalPaise',
    payment_state,
    content
)

# Add Payment Method UI
payment_ui = """            Spacer(Modifier.height(12.dp))
            
            // Payment Method
            Surface(shape = RoundedCornerShape(18.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Payment Method", style = MaterialTheme.typography.titleMedium, color = Ink)
                    paymentMethods.forEach { method ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPaymentMethod = method }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            androidx.compose.material3.RadioButton(
                                selected = (selectedPaymentMethod == method),
                                onClick = { selectedPaymentMethod = method },
                                colors = androidx.compose.material3.RadioButtonDefaults.colors(selectedColor = Coral)
                            )
                            Text(
                                text = method,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Ink,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                    if (selectedPaymentMethod != "Cash on Delivery") {
                        Surface(shape = RoundedCornerShape(8.dp), color = Peach, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(
                                text = "Demo Mode: No real payment will be collected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Coral,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))"""

content = re.sub(
    r'            Surface\(shape = RoundedCornerShape\(16.dp\), color = SoftAmber',
    payment_ui + r'\n            Surface(shape = RoundedCornerShape(16.dp), color = SoftAmber',
    content
)

# Modify RushButton onClick to check payment method
button_ui = """                RushButton(
                    text = if (orderState.placing) "Placing demo order…" else if (selectedPaymentMethod == "Cash on Delivery") "Place Order · ${formatPrice(payable)}" else "Proceed to Payment · ${formatPrice(payable)}",
                    onClick = {
                        if (selectedPaymentMethod == "Cash on Delivery") {
                            onPlaceOrder()
                        } else {
                            showPaymentDemo = true
                        }
                    },
                    enabled = cartItems.isNotEmpty() && !state.address.isNullOrBlank() && !orderState.placing,
                    loading = orderState.placing,
                )
                if (showPaymentDemo) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showPaymentDemo = false },
                        title = { Text("Simulate Payment") },
                        text = { Text("Simulate a successful payment for ${formatPrice(payable)} via $selectedPaymentMethod?") },
                        confirmButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showPaymentDemo = false
                                onPlaceOrder()
                            }) {
                                Text("Simulate Success", color = Coral)
                            }
                        },
                        dismissButton = {
                            androidx.compose.material3.TextButton(onClick = {
                                showPaymentDemo = false
                                // Simulate failure doesn't place order
                            }) {
                                Text("Simulate Failure")
                            }
                        }
                    )
                }"""

content = re.sub(
    r'                RushButton\(\n                    text = if \(orderState.placing\) "Placing demo order…" else "Place Order · \$\{formatPrice\(payable\)\}",\n                    onClick = onPlaceOrder,\n                    enabled = cartItems.isNotEmpty\(\) && !state.address.isNullOrBlank\(\) && !orderState.placing,\n                    loading = orderState.placing,\n                \)',
    button_ui,
    content
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'w') as f:
    f.write(content)
