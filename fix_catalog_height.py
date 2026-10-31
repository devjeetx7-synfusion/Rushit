import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'r') as f:
    content = f.read()

content = content.replace("Spacer(Modifier.height(", "Spacer(androidx.compose.foundation.layout.Modifier.height(")

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'w') as f:
    f.write(content)

