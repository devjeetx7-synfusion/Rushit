import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'r') as f:
    content = f.read()

# Fix "import androidx.compose.foundation.BorderStroke\npackage com.rushit.app.ui.screens.store"
content = content.replace("import androidx.compose.foundation.BorderStroke\npackage com.rushit.app.ui.screens.store", "package com.rushit.app.ui.screens.store\nimport androidx.compose.foundation.BorderStroke")

with open('app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt', 'w') as f:
    f.write(content)
