import re

files = [
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductScreensExt.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt'
]

for file in files:
    with open(file, 'r') as f:
        lines = f.readlines()
    
    package_line = None
    package_idx = -1
    for i, line in enumerate(lines):
        if line.startswith('package '):
            package_line = line
            package_idx = i
            break
            
    if package_line and package_idx >= 0:
        lines.pop(package_idx)
        lines.insert(0, package_line)
        
    with open(file, 'w') as f:
        f.writelines(lines)
