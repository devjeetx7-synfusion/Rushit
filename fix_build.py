import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'r') as f:
    content = f.read()

content = content.replace("Spacer(androidx.compose.foundation.layout.Modifier.height(", "Spacer(Modifier.height(")

content = re.sub(r'import androidx.compose.foundation.layout.heightIn\n', 'import androidx.compose.foundation.layout.heightIn\nimport androidx.compose.foundation.layout.height\n', content)

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'w') as f:
    f.write(content)
