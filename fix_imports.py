import os

files = [
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductScreensExt.kt'
]

for file in files:
    with open(file, 'r') as f:
        lines = f.readlines()
    
    # Find package line
    package_line_idx = -1
    for i, line in enumerate(lines):
        if line.startswith('package '):
            package_line_idx = i
            break
            
    if package_line_idx > 0:
        # Move package line to top
        package_line = lines.pop(package_line_idx)
        lines.insert(0, package_line)
        
    with open(file, 'w') as f:
        f.writelines(lines)
