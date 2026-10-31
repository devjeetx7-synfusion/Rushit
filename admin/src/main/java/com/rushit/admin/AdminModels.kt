package com.rushit.admin

import android.content.SharedPreferences
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

const val PRODUCT_META_MARKER = " ⟐ "
const val ADMIN_PREFS_NAME = "rushit_admin_demo"

@Immutable
data class ProductMetadata(
    val sku: String = "",
    val description: String = "",
    val salePrice: String = "",
    val stockQuantity: String = "",
)

@Immutable
data class HomeSectionConfig(
    val title: String,
    val type: String,
    val subtitle: String,
    val layout: String,
    val surface: String,
    val motion: String,
    val visibility: String,
) {
    fun encode(): String = listOf(title, type, subtitle, layout, surface, motion, visibility)
        .map(::cleanStoredField)
        .joinToString("|")
}

@Immutable
data class MoreDestination(
    val route: String,
    val label: String,
    val detail: String,
    val icon: ImageVector,
    val required: (AdminPermissions) -> Boolean,
)

fun visibleProductRecord(value: String): String = value.substringBefore(PRODUCT_META_MARKER)

fun cleanStoredField(value: String): String = value.replace('|', ' ').replace("##", " ").trim()

fun formatRupees(value: Double): String =
    String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')

fun productMetadata(value: String): ProductMetadata = runCatching {
    val encoded = value.substringAfter(PRODUCT_META_MARKER)
    if (encoded.isBlank() || !value.contains(PRODUCT_META_MARKER)) return ProductMetadata()
    val decoded = String(android.util.Base64.decode(encoded, android.util.Base64.DEFAULT), Charsets.UTF_8)
        .split('\u001f')
    ProductMetadata(
        sku = decoded.getOrElse(0) { "" },
        description = decoded.getOrElse(1) { "" },
        salePrice = decoded.getOrElse(2) { "" },
        stockQuantity = decoded.getOrElse(3) { "" },
    )
}.getOrDefault(ProductMetadata())

fun withProductMetadata(record: String, metadata: ProductMetadata): String {
    val packed = listOf(
        metadata.sku,
        metadata.description,
        metadata.salePrice,
        metadata.stockQuantity,
    ).joinToString("\u001f")
    return visibleProductRecord(record) + PRODUCT_META_MARKER +
        android.util.Base64.encodeToString(packed.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
}

fun parseHomeSection(value: String): HomeSectionConfig {
    val fields = value.split('|')
    if (fields.size >= 7) {
        return HomeSectionConfig(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6])
    }
    val title = fields.getOrNull(0).orEmpty()
    val legacyType = when {
        title.contains("banner", true) -> "Hero / banner"
        title.contains("categor", true) -> "Categories"
        title.contains("festival", true) -> "Festival campaigns"
        title.contains("offer", true) || title.contains("deal", true) -> "Offers"
        title.contains("promo", true) -> "Promotion"
        title.contains("grid", true) -> "Product grid"
        else -> "Featured products"
    }
    return HomeSectionConfig(
        title = title,
        type = legacyType,
        subtitle = fields.getOrNull(1).orEmpty(),
        layout = "Carousel",
        surface = "Soft",
        motion = "Fade",
        visibility = fields.lastOrNull()?.takeIf { it in setOf("Visible", "Hidden") } ?: "Visible",
    )
}

fun categoryGlyph(name: String): String = categoryEmojiBadge(name)

val homeSectionTypes = listOf(
    "Hero / banner",
    "Promotion",
    "Featured products",
    "Product grid",
    "Categories",
    "Festival campaigns",
    "Offers",
    "Custom promotion",
)

val initialProducts = listOf(
    "Tomato · ₹24/kg · In stock · Vegetables · Visible",
    "Banana · ₹32/kg · In stock · Fruits · Visible",
    "Milk · ₹56/L · In stock · Dairy · Visible",
    "Bread · ₹32/piece · Low stock · Bakery · Visible",
    "Apple · ₹120/kg · In stock · Fruits · Visible",
)

val initialCategories = listOf(
    "Fruits|Visible",
    "Vegetables|Visible",
    "Dairy|Visible",
    "Snacks|Visible",
    "Beverages|Visible",
    "Bakery|Visible",
    "Personal Care|Visible",
)

val initialCampaigns = listOf(
    "Diwali|2026-11-10|2026-11-20|1|Enabled|Celebrate with lights and festive deals|Coral|09:00|23:59|Sweets, Gifts, Decor",
    "Holi|2027-03-01|2027-03-10|2|Enabled|Colors, sweets and spring collection|Berry|09:00|23:59|Sweets, Snacks",
    "Christmas|2026-12-20|2026-12-26|3|Disabled|Holiday gifting and winter treats|Green|09:00|23:59|Gifts, Bakery",
    "Eid|2027-04-10|2027-04-11|2|Disabled|Festive feast essentials|Indigo|08:00|22:00|Sweets, Fruits",
    "Valentine's Day|2027-02-14|2027-02-14|3|Disabled|Gifts for someone special|Berry|00:00|23:59|Gifts, Decor",
)

val initialOffers = listOf(
    "Welcome offer|WELCOME10|Percentage|10|499|500|2026-10-01|2026-12-31|Enabled",
    "First order|FIRSTORDER|Percentage|20|399|1000|2026-10-01|2027-03-31|Enabled",
    "Flat discount|SOMAI50|Fixed|50|299|500|2026-11-01|2026-12-15|Disabled",
)

val initialOrders = listOf(
    "#RI172805456  ·  Ankit Kumar  ·  ₹542  ·  Preparing",
    "#RI172805455  ·  Priya Singh  ·  ₹386  ·  On the way",
    "#RI172805454  ·  Rohit Sharma  ·  ₹724  ·  Delivered",
    "#RI172805453  ·  Sneha Verma  ·  ₹218  ·  Cancelled",
)

val defaultHomeSections = listOf(
    "Top Banner|Festival Deals|Visible",
    "Category Icons|8 categories|Visible",
    "Flash Deals|Horizontal products|Visible",
    "Best Selling|Product grid|Visible",
    "Recommended|Personalized|Visible",
)

val demoCustomers = listOf(
    "Ankit Kumar · Patna · 5 orders",
    "Priya Singh · Delhi · 12 orders",
    "Rohit Sharma · Mumbai · 8 orders",
    "Sneha Verma · Patna · 3 orders",
    "Aman Gupta · Bengaluru · 6 orders",
)

val orderStatusFlow = listOf(
    "Confirmed",
    "Preparing",
    "Rider Ready",
    "Picked Up",
    "On the way",
    "Almost there",
    "Delivered",
)

fun greetingForHour(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): String = when (hour) {
    in 5..11 -> "Good morning"
    in 12..16 -> "Good afternoon"
    in 17..21 -> "Good evening"
    else -> "Hello"
}

fun todayDisplayDate(): String = SimpleDateFormat("EEE, d MMM yyyy", Locale.US).format(Date())

fun loadPrefList(prefs: SharedPreferences, key: String, fallback: List<String>): List<String> {
    val stored = prefs.getString(key, null)?.split("##")?.filter { it.isNotBlank() }
    return if (stored.isNullOrEmpty()) fallback else stored
}

fun savePrefList(prefs: SharedPreferences, key: String, values: List<String>) {
    prefs.edit().putString(key, values.joinToString("##")).apply()
}

fun replaceList(target: SnapshotStateList<String>, values: List<String>) {
    target.clear()
    target.addAll(values)
}

fun productAvailabilityLabel(stock: Int): String = when {
    stock <= 0 -> "Out of stock"
    stock < 5 -> "Low stock"
    else -> "In stock"
}

fun extractOrderAmount(order: String): Int =
    Regex("₹(\\d+)").find(visibleProductRecord(order))?.groupValues?.getOrNull(1)?.toIntOrNull() ?: 0

fun extractOrderId(order: String): String = visibleProductRecord(order).substringBefore(" · ").trim()

fun extractOrderCustomer(order: String): String =
    visibleProductRecord(order).split(" · ").getOrNull(1)?.trim().orEmpty()

fun extractOrderStatus(order: String): String =
    visibleProductRecord(order).substringAfterLast(" · ").trim()

fun nextOrderStatus(current: String): String? {
    val index = orderStatusFlow.indexOfFirst { it.equals(current, ignoreCase = true) }
    return when {
        current.equals("Cancelled", true) || current.equals("Delivered", true) -> null
        index < 0 -> "Preparing"
        index >= orderStatusFlow.lastIndex -> null
        else -> orderStatusFlow[index + 1]
    }
}

fun canCancelOrder(status: String): Boolean =
    status in setOf("Confirmed", "Preparing", "Rider Ready")

fun moreDestinations(): List<MoreDestination> = listOf(
    MoreDestination("designer", "Home Designer", "Compose local Home layout sections", AdminIcons.Layout) { it.canManageContent },
    MoreDestination("festivals", "Festival Manager", "Seasonal collections & banners", AdminIcons.Sparkles) { it.canManageCampaigns },
    MoreDestination("schedule", "Campaign Scheduler", "Schedule local campaign windows", AdminIcons.Calendar) { it.canManageCampaigns },
    MoreDestination("customers", "Customers", "Demo customer directory & notes", AdminIcons.Users) { it.canManageCustomers },
    MoreDestination("offers", "Offers & Coupons", "Local discount codes", AdminIcons.Tag) { it.canManageOffers },
    MoreDestination("notifications", "Notifications", "Local draft previews · not delivered", AdminIcons.Bell) { it.canManageNotifications },
    MoreDestination("settings", "App Settings", "Store identity & local preferences", AdminIcons.Settings) { it.canManageSettings },
    MoreDestination("roles", "Admin Roles", "Role-aware UI preview (local demo)", AdminIcons.Shield) { true },
    MoreDestination("activity_logs", "Activity Logs", "Local device history of admin actions", AdminIcons.Activity) { true },
    MoreDestination("profile", "Admin Profile", "Demo account, role & session", AdminIcons.Users) { true },
    MoreDestination(AdminRoutes.PAYMENTS, "Payment & Refund History", "View customer payments and refunds", AdminIcons.Activity) { true },
    MoreDestination(AdminRoutes.PAYOUTS, "Settlements & Payouts", "Seller settlements and payouts summary", AdminIcons.Layout) { true },
)

fun parseNotesMap(raw: String?): MutableMap<String, String> {
    val map = mutableMapOf<String, String>()
    raw?.split("##")?.filter { it.isNotBlank() }?.forEach { row ->
        val key = row.substringBefore('|')
        val value = row.substringAfter('|', "")
        if (key.isNotBlank()) map[key] = value
    }
    return map
}

fun encodeNotesMap(map: Map<String, String>): String =
    map.entries.joinToString("##") { "${cleanStoredField(it.key)}|${cleanStoredField(it.value)}" }

fun parseTimeline(raw: String?): MutableMap<String, MutableList<String>> {
    val map = mutableMapOf<String, MutableList<String>>()
    raw?.split("##")?.filter { it.isNotBlank() }?.forEach { row ->
        val parts = row.split('|')
        if (parts.size >= 3) {
            val id = parts[0]
            map.getOrPut(id) { mutableListOf() }.add("${parts[1]}|${parts[2]}")
        }
    }
    return map
}

fun encodeTimeline(map: Map<String, List<String>>): String =
    map.entries.flatMap { (id, events) ->
        events.map { event ->
            val status = event.substringBefore('|')
            val stamp = event.substringAfter('|')
            "${cleanStoredField(id)}|${cleanStoredField(status)}|$stamp"
        }
    }.joinToString("##")
