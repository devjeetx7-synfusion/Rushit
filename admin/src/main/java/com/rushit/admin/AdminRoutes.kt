package com.rushit.admin

/**
 * Centralized Admin navigation routes. Always compare against [pattern] values from
 * NavBackStackEntry.destination.route, which is the composable pattern rather than
 * the filled argument string.
 */
object AdminRoutes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val CATALOG = "catalog"
    const val ORDERS = "orders"
    const val THEMES = "themes"
    const val MORE = "more"

    const val DESIGNER = "designer"
    const val SECTION = "section/{index}"
    const val PREVIEW = "preview"
    const val FESTIVALS = "festivals"
    const val FESTIVAL_EDIT = "festival_edit/{index}"
    const val SCHEDULE = "schedule"
    const val PRODUCT_EDIT = "product_edit"
    const val PRODUCT_EDIT_INDEX = "product_edit/{index}"
    const val CATEGORIES = "categories"
    const val ORDER_DETAIL = "order_detail/{index}"
    const val CUSTOMERS = "customers"
    const val CUSTOMER_DETAIL = "customer_detail/{index}"
    const val OFFERS = "offers"
    const val OFFER_EDIT = "offer_edit/{index}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val ROLES = "roles"
    const val ACTIVITY_LOGS = "activity_logs"

    val rootTabs = listOf(HOME, CATALOG, ORDERS, THEMES, MORE)

    fun productEdit(index: Int) = "product_edit/$index"
    fun section(index: Int) = "section/$index"
    fun festivalEdit(index: Int) = "festival_edit/$index"
    fun festivalNew() = "festival_edit/new"
    fun offerEdit(index: Int) = "offer_edit/$index"
    fun offerNew() = "offer_edit/new"
    fun orderDetail(index: Int) = "order_detail/$index"
    fun customerDetail(index: Int) = "customer_detail/$index"

    fun isAuth(route: String?): Boolean = route == SPLASH || route == LOGIN

    fun isImmersiveEditor(route: String?): Boolean = when (route) {
        PRODUCT_EDIT, PRODUCT_EDIT_INDEX, SECTION, FESTIVAL_EDIT, OFFER_EDIT,
        ORDER_DETAIL, CUSTOMER_DETAIL,
        -> true
        else -> false
    }

    fun parentTab(route: String?): String = when (route) {
        null, SPLASH, LOGIN, HOME -> HOME
        CATALOG, PRODUCT_EDIT, PRODUCT_EDIT_INDEX, CATEGORIES -> CATALOG
        ORDERS, ORDER_DETAIL -> ORDERS
        THEMES, DESIGNER, PREVIEW, FESTIVALS, SCHEDULE, SECTION, FESTIVAL_EDIT -> THEMES
        else -> MORE
    }

    fun isRootTab(route: String?): Boolean = route in rootTabs

    fun isNavItemSelected(itemRoute: String, currentRoute: String?): Boolean = when (itemRoute) {
        HOME -> parentTab(currentRoute) == HOME
        CATALOG -> parentTab(currentRoute) == CATALOG
        ORDERS -> parentTab(currentRoute) == ORDERS
        CUSTOMERS -> currentRoute == CUSTOMERS || currentRoute == CUSTOMER_DETAIL
        THEMES -> parentTab(currentRoute) == THEMES &&
            currentRoute !in setOf(FESTIVALS, FESTIVAL_EDIT, SCHEDULE)
        FESTIVALS -> currentRoute in setOf(FESTIVALS, FESTIVAL_EDIT, SCHEDULE)
        MORE -> parentTab(currentRoute) == MORE &&
            currentRoute !in setOf(CUSTOMERS, CUSTOMER_DETAIL, FESTIVALS, FESTIVAL_EDIT, SCHEDULE)
        else -> currentRoute == itemRoute
    }
}
