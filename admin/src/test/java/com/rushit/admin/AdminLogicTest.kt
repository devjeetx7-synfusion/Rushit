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
}
