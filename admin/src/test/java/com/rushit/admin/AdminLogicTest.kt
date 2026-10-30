package com.rushit.admin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminLogicTest {
    @Test
    fun nextOrderStatusFollowsAllowedTransitions() {
        assertEquals("Preparing", nextOrderStatus("Confirmed"))
        assertEquals("Rider Ready", nextOrderStatus("Preparing"))
        assertEquals("Delivered", nextOrderStatus("Almost there"))
        assertNull(nextOrderStatus("Delivered"))
        assertNull(nextOrderStatus("Cancelled"))
    }

    @Test
    fun cancelIsOnlyAllowedBeforeRiderLeaves() {
        assertTrue(canCancelOrder("Confirmed"))
        assertTrue(canCancelOrder("Preparing"))
        assertTrue(canCancelOrder("Rider Ready"))
        assertFalse(canCancelOrder("On the way"))
        assertFalse(canCancelOrder("Delivered"))
    }

    @Test
    fun productAvailabilityUsesStockThresholds() {
        assertEquals("Out of stock", productAvailabilityLabel(0))
        assertEquals("Low stock", productAvailabilityLabel(4))
        assertEquals("In stock", productAvailabilityLabel(5))
    }

    @Test
    fun parseHomeSectionMigratesLegacyRecords() {
        val parsed = parseHomeSection("Top Banner|Festival Deals|Visible")
        assertEquals("Top Banner", parsed.title)
        assertEquals("Hero / banner", parsed.type)
        assertEquals("Visible", parsed.visibility)
    }

    @Test
    fun rolePermissionsStayScoped() {
        val catalog = AdminRoleSpec.CATALOG_MANAGER.permissions
        assertTrue(catalog.canManageProducts)
        assertFalse(catalog.canManageOrders)
        assertFalse(catalog.canManageAdmins)

        val support = AdminRoleSpec.SUPPORT_AGENT.permissions
        assertTrue(support.canManageCustomers)
        assertTrue(support.canViewOrders)
        assertFalse(support.canManageOrders)
        assertFalse(support.canManageProducts)
    }

    @Test
    fun activityLogRoundTripPreservesFields() {
        val original = ActivityLogEntry("Products", "Product updated", "Apple · ₹120/kg", 1_700_000_000_000L)
        val restored = ActivityLogEntry.decode(original.encode())
        assertEquals(original, restored)
    }

    @Test
    fun parentTabMapsNestedRoutesWithoutUsingSplashAsRoot() {
        assertEquals(AdminRoutes.HOME, AdminRoutes.parentTab(AdminRoutes.HOME))
        assertEquals(AdminRoutes.CATALOG, AdminRoutes.parentTab(AdminRoutes.PRODUCT_EDIT_INDEX))
        assertEquals(AdminRoutes.CATALOG, AdminRoutes.parentTab(AdminRoutes.CATEGORIES))
        assertEquals(AdminRoutes.ORDERS, AdminRoutes.parentTab(AdminRoutes.ORDER_DETAIL))
        assertEquals(AdminRoutes.THEMES, AdminRoutes.parentTab(AdminRoutes.DESIGNER))
        assertEquals(AdminRoutes.THEMES, AdminRoutes.parentTab(AdminRoutes.SECTION))
        assertEquals(AdminRoutes.MORE, AdminRoutes.parentTab(AdminRoutes.SETTINGS))
        assertEquals(AdminRoutes.MORE, AdminRoutes.parentTab(AdminRoutes.CUSTOMERS))
        assertFalse(AdminRoutes.isRootTab(AdminRoutes.SPLASH))
        assertTrue(AdminRoutes.isAuth(AdminRoutes.LOGIN))
        assertTrue(AdminRoutes.isAuth(AdminRoutes.SPLASH))
        assertTrue(AdminRoutes.isImmersiveEditor(AdminRoutes.PRODUCT_EDIT_INDEX))
        assertFalse(AdminRoutes.isImmersiveEditor(AdminRoutes.HOME))
    }

    @Test
    fun sidebarSelectionDoesNotHighlightMoreForShortcutRoutes() {
        assertTrue(AdminRoutes.isNavItemSelected(AdminRoutes.CUSTOMERS, AdminRoutes.CUSTOMERS))
        assertTrue(AdminRoutes.isNavItemSelected(AdminRoutes.CUSTOMERS, AdminRoutes.CUSTOMER_DETAIL))
        assertFalse(AdminRoutes.isNavItemSelected(AdminRoutes.MORE, AdminRoutes.CUSTOMERS))
        assertTrue(AdminRoutes.isNavItemSelected(AdminRoutes.FESTIVALS, AdminRoutes.SCHEDULE))
        assertFalse(AdminRoutes.isNavItemSelected(AdminRoutes.THEMES, AdminRoutes.FESTIVALS))
        assertTrue(AdminRoutes.isNavItemSelected(AdminRoutes.CATALOG, AdminRoutes.CATEGORIES))
        assertTrue(AdminRoutes.isNavItemSelected(AdminRoutes.HOME, AdminRoutes.HOME))
    }

    @Test
    fun windowBreakpointsFollowMaterialWidthClasses() {
        assertEquals(AdminNavLayout.BottomBar, adminNavLayoutFor(320))
        assertEquals(AdminNavLayout.BottomBar, adminNavLayoutFor(360))
        assertEquals(AdminNavLayout.BottomBar, adminNavLayoutFor(599))
        assertEquals(AdminNavLayout.Rail, adminNavLayoutFor(600))
        assertEquals(AdminNavLayout.Rail, adminNavLayoutFor(839))
        assertEquals(AdminNavLayout.Sidebar, adminNavLayoutFor(840))
        assertEquals(AdminNavLayout.Sidebar, adminNavLayoutFor(1024))
        assertEquals(AdminNavLayout.Sidebar, adminNavLayoutFor(1280))
    }

    @Test
    fun adaptiveColumnsFollowContentWidth() {
        assertEquals(1, adminKpiColumnsFor(320))
        assertEquals(2, adminKpiColumnsFor(360))
        assertEquals(2, adminKpiColumnsFor(720))
        assertEquals(3, adminKpiColumnsFor(900))
        assertEquals(1, adminFormColumnsFor(360))
        assertEquals(1, adminFormColumnsFor(639))
        assertEquals(2, adminFormColumnsFor(640))
        assertEquals(1, adminListColumnsFor(360))
        assertEquals(2, adminListColumnsFor(640))
        assertEquals(3, adminListColumnsFor(1100))
        assertEquals(1, adminDetailColumnsFor(360))
        assertEquals(2, adminDetailColumnsFor(700))
        val compact = adminWindowFor(320, 640)
        assertEquals(AdminNavLayout.BottomBar, compact.navLayout)
        assertEquals(1, compact.kpiColumns)
        assertEquals(1, compact.listColumns)
        val afterSidebar = adminWindowFor(840, 800).forContentWidth(592, 800)
        assertEquals(AdminNavLayout.Sidebar, afterSidebar.navLayout)
        assertEquals(2, afterSidebar.kpiColumns)
        assertEquals(1, afterSidebar.formColumns)
        assertEquals(1, afterSidebar.listColumns)
    }

    @Test
    fun restrictedRolesCannotOpenUnauthorizedDestinations() {
        val catalog = AdminRoleSpec.CATALOG_MANAGER.permissions
        assertTrue(catalog.allowsDestination(AdminRoutes.CATALOG))
        assertTrue(catalog.allowsDestination(AdminRoutes.PRODUCT_EDIT_INDEX))
        assertFalse(catalog.allowsDestination(AdminRoutes.ORDERS))
        assertFalse(catalog.allowsDestination(AdminRoutes.CUSTOMERS))
        assertFalse(catalog.allowsDestination(AdminRoutes.SETTINGS))
        assertTrue(catalog.allowsDestination(AdminRoutes.MORE))

        val support = AdminRoleSpec.SUPPORT_AGENT.permissions
        assertTrue(support.allowsDestination(AdminRoutes.CUSTOMERS))
        assertTrue(support.allowsDestination(AdminRoutes.ORDERS))
        assertFalse(support.allowsDestination(AdminRoutes.CATALOG))
        assertFalse(support.allowsDestination(AdminRoutes.FESTIVALS))
        assertFalse(support.allowsDestination(AdminRoutes.OFFERS))

        val content = AdminRoleSpec.CONTENT_MANAGER.permissions
        assertTrue(content.allowsDestination(AdminRoutes.THEMES))
        assertTrue(content.allowsDestination(AdminRoutes.DESIGNER))
        assertFalse(content.allowsDestination(AdminRoutes.ORDERS))
    }

    @Test
    fun contentWidthKeepsChromeLayoutFromFullWindow() {
        val full = adminWindowFor(1280, 800)
        assertEquals(AdminNavLayout.Sidebar, full.navLayout)
        val content = full.forContentWidth(1032, 800)
        assertEquals(AdminNavLayout.Sidebar, content.navLayout)
        assertEquals(3, content.kpiColumns)
        assertEquals(2, content.formColumns)
        assertEquals(2, content.listColumns)
    }
}
