files = [
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/ProductScreensExt.kt',
    'app/src/main/java/com/rushit/app/ui/screens/store/CatalogScreens.kt'
]
for file in files:
    with open(file, 'r') as f:
        text = f.read()
    count = 0
    for c in text:
        if c == '{': count += 1
        elif c == '}': count -= 1
    print(f"{file}: {count}")
