import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'r') as f:
    content = f.read()

# Update signature
content = re.sub(
    r'onPlaceOrder: \(\) -> Unit,',
    r'onPlaceOrder: (String) -> Unit,',
    content
)

# Update onClick
content = re.sub(
    r'                            onPlaceOrder\(\)',
    r'                            onPlaceOrder(selectedPaymentMethod)',
    content
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CheckoutTrackingScreens.kt', 'w') as f:
    f.write(content)
