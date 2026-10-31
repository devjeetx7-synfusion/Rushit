import re

with open('admin/src/main/java/com/rushit/admin/AdminModels.kt', 'r') as f:
    content = f.read()

more_dest = """    MoreDestination("profile", "Admin Profile", "Demo account, role & session", AdminIcons.Users) { true },
    MoreDestination(AdminRoutes.PAYMENTS, "Payment & Refund History", "View customer payments and refunds", AdminIcons.Activity) { true },
    MoreDestination(AdminRoutes.PAYOUTS, "Settlements & Payouts", "Seller settlements and payouts summary", AdminIcons.Layout) { true },"""

content = re.sub(
    r'    MoreDestination\("profile", "Admin Profile", "Demo account, role & session", AdminIcons\.Users\) \{ true \},',
    more_dest,
    content
)

with open('admin/src/main/java/com/rushit/admin/AdminModels.kt', 'w') as f:
    f.write(content)
