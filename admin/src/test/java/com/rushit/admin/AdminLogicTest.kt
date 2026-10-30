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
        assertEquals(AdminNavLayout.BottomBar, adminNavLayoutFor(360))
        assertEquals(AdminNavLayout.Rail, adminNavLayoutFor(800))
        assertEquals(AdminNavLayout.Rail, adminNavLayoutFor(600))
        assertEquals(AdminNavLayout.Sidebar, adminNavLayoutFor(840))
        assertEquals(AdminNavLayout.Sidebar, adminNavLayoutFor(1280))
        assertEquals(AdminNavLayout.BottomBar, adminNavLayoutFor(599))
        assertEquals(2, adminKpiColumnsFor(400))
        assertEquals(3, adminKpiColumnsFor(1100))
        assertEquals(1, adminFormColumnsFor(360))
        assertEquals(2, adminFormColumnsFor(720))
    }
}
