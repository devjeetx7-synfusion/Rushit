import re

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'r') as f:
    content = f.read()

content = content.replace("onAdd = { shop.changeQuantity(it, delta = 1) },", "onAdd = { id -> shop.changeQuantity(id, delta = 1) },")

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'w') as f:
    f.write(content)
