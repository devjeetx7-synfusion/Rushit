import re

with open('app/src/main/java/com/rushit/app/ui/screens/order/OrderDetailsScreen.kt', 'r') as f:
    content = f.read()

# Update Items display
item_row = """                            Column(Modifier.weight(1f)) {
                                Text(line.name, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (line.variantName != null) {
                                    Text(line.variantName, style = MaterialTheme.typography.bodySmall, color = Muted)
                                }
                                Text("${line.unit} · ${formatPrice(line.unitPricePaise)}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            }"""

content = re.sub(
    r'                            Column\(Modifier.weight\(1f\)\) \{\n                                Text\(line.name, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 1, overflow = TextOverflow.Ellipsis\)\n                                Text\("\$\{line.unit\} · \$\{formatPrice\(line.unitPricePaise\)\}", style = MaterialTheme.typography.bodySmall, color = Muted\)\n                            \}',
    item_row,
    content
)

# Update Bill details to include payment info
bill_details = """                    BillRow("Items total", formatPrice(order.subtotalPaise))
                    BillRow("Delivery fee", if (order.deliveryFeePaise == 0L) "FREE" else formatPrice(order.deliveryFeePaise))
                    BillRow("Platform fee", formatPrice(order.platformFeePaise))
                    if (order.discountPaise > 0) BillRow("Discount", "−" + formatPrice(order.discountPaise))
                    HorizontalDivider(color = Border)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("To pay", style = MaterialTheme.typography.titleMedium, color = Ink)
                        Text(formatPrice(order.totalPaise), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Payment Information", style = MaterialTheme.typography.titleSmall, color = Ink)
                    BillRow("Method", order.paymentMethod)
                    BillRow("Status", order.paymentStatus)"""

content = re.sub(
    r'                    BillRow\("Items total", formatPrice\(order.subtotalPaise\)\).*?Text\(formatPrice\(order.totalPaise\), style = MaterialTheme.typography.titleMedium, color = Ink\)\n                    \}',
    bill_details,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/screens/order/OrderDetailsScreen.kt', 'w') as f:
    f.write(content)
