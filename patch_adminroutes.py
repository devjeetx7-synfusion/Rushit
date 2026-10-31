import re

with open('admin/src/main/java/com/rushit/admin/AdminRoutes.kt', 'r') as f:
    content = f.read()

content = content.replace("const val ACTIVITY_LOGS = \"activity_logs\"", "const val ACTIVITY_LOGS = \"activity_logs\"\n    const val PAYMENTS = \"payments\"\n    const val PAYOUTS = \"payouts\"")

with open('admin/src/main/java/com/rushit/admin/AdminRoutes.kt', 'w') as f:
    f.write(content)
