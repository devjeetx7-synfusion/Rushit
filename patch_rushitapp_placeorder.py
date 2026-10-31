import re

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'r') as f:
    content = f.read()

# Update onPlaceOrder
new_onplaceorder = """                            onPlaceOrder = { paymentMethod ->
                                val lines = orderLinesFor(shopState.cart, shopState.products)
                                if (lines.isNotEmpty()) {
                                    val subtotal = lines.sumOf { it.lineTotalPaise }
                                    orders.placeOrder(
                                        OrderDraft(
                                            items = lines,
                                            mode = shopState.selectedMode,
                                            addressSummary = shopState.address,
                                            platformFeePaise = DeliveryPricing.platformFee(),
                                            deliveryFeePaise = DeliveryPricing.deliveryFee(subtotal),
                                            paymentMethod = paymentMethod
                                        ),
                                    )
                                }
                            },"""

content = re.sub(
    r'                            onPlaceOrder = \{\n                                val lines = orderLinesFor\(shopState.cart, shopState.products\).*?\n                            \},',
    new_onplaceorder,
    content,
    flags=re.DOTALL
)

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'w') as f:
    f.write(content)
