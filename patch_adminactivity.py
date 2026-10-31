import re

with open('admin/src/main/java/com/rushit/admin/AdminActivity.kt', 'r') as f:
    content = f.read()

new_routes = """                    composable("activity_logs") {
                        ActivityLogsScreen(logs = activityLogs, onClearLogs = {
                            activityLogs.clear()
                            activityLogs.addAll(defaultSeedActivityLogs())
                            preferences.edit().putString("activity_logs", activityLogs.joinToString("##") { it.encode() }).apply()
                            snackbar = "Local activity log reset to sample events"
                        })
                    }
                    composable(AdminRoutes.PAYMENTS) {
                        AdminPaymentsScreen(onBack = { nav.popBackStack() })
                    }
                    composable(AdminRoutes.PAYOUTS) {
                        AdminPayoutsScreen(onBack = { nav.popBackStack() })
                    }"""

content = re.sub(
    r'                    composable\("activity_logs"\) \{\n                        ActivityLogsScreen.*?\}\n                    \}',
    new_routes,
    content,
    flags=re.DOTALL
)

with open('admin/src/main/java/com/rushit/admin/AdminActivity.kt', 'w') as f:
    f.write(content)
