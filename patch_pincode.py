import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'r') as f:
    content = f.read()

new_pincode = """@Composable
fun PincodeChecker() {
    var pincode by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Check Delivery Options", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pincode,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pincode = it },
                placeholder = { Text("Enter 6-digit Pincode", color = Muted) },
                modifier = Modifier.weight(1f).height(54.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite
                ),
                trailingIcon = {
                    if (pincode.isNotEmpty()) {
                        RushIcon(R.drawable.ic_cancel_01, "Clear", Modifier.size(16.dp).clickable { pincode = ""; result = null }, Muted)
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (pincode.length == 6) {
                        isLoading = true
                        scope.launch {
                            kotlinx.coroutines.delay(600)
                            isLoading = false
                            result = if (pincode.startsWith("1")) "Delivery not available for this area (Demo)." else "Delivery available in 3 days. COD eligible (Demo)."
                        }
                    } else {
                        result = "Please enter a valid 6-digit pincode."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (result != null) {
            Text(result!!, style = MaterialTheme.typography.bodySmall, color = if (result!!.contains("available in")) Success else Coral, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
    }
}"""

content = re.sub(
    r'@Composable\nfun PincodeChecker.*?\}\n    \}\n\}',
    new_pincode,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'w') as f:
    f.write(content)
