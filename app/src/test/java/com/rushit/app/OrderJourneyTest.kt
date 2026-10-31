package com.rushit.app

import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderLine
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.domain.DeliveryPricing
import com.rushit.app.domain.DeliveryStage
import com.rushit.app.domain.JourneyScene
import com.rushit.app.domain.canCancel
import com.rushit.app.domain.orderLinesFor
import com.rushit.app.domain.orderSubtotal
import com.rushit.app.domain.sceneFor
import com.rushit.app.domain.shouldCelebrate
import com.rushit.app.domain.stageFor
import com.rushit.app.domain.stageIndex
import com.rushit.app.data.model.Product
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderJourneyTest {

    private fun order(status: OrderStatus, celebratedAt: String? = null) = Order(
        id = "RI123456789",
        status = status,
        items = listOf(OrderLine(productId = "tomato", name = "Tomato", unit = "1 kg", quantity = 2, unitPricePaise = 3_200)),
        subtotalPaise = 6_400,
        platformFeePaise = 500,
        totalPaise = 6_900,
        createdAt = "2026-10-29T10:15:00+0530",
        celebratedAt = celebratedAt,
    )

    @Test fun `legal transitions move forward only`() {
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.PREPARING))
        assertTrue(OrderStatus.OUT_FOR_DELIVERY.canTransitionTo(OrderStatus.DELIVERED))
        assertTrue(OrderStatus.ARRIVING.canTransitionTo(OrderStatus.DELIVERED))
        assertTrue(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.CANCELLED))
        assertFalse("statuses cannot go backwards", OrderStatus.DELIVERED.canTransitionTo(OrderStatus.OUT_FOR_DELIVERY))
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CONFIRMED))
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CANCELLED))
        assertFalse(OrderStatus.CONFIRMED.canTransitionTo(OrderStatus.DELIVERED))
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CONFIRMED))
        assertFalse(OrderStatus.PICKED_UP.canTransitionTo(OrderStatus.CANCELLED))
    }

    @Test fun `demo path visits every stage once and terminates`() {
        val visited = mutableListOf(OrderStatus.PENDING)
        var current: OrderStatus? = OrderStatus.PENDING
        while (true) {
            current = current?.demoNext() ?: break
            visited += current
        }
        assertEquals(OrderStatus.DELIVERED, visited.last())
        visited.zipWithNext().forEach { (from, to) -> assertTrue("$from -> $to", from.canTransitionTo(to)) }
        assertEquals(visited.size, visited.toSet().size)
    }

    @Test fun `timeline stage follows the order status`() {
        assertEquals(DeliveryStage.CONFIRMED, stageFor(OrderStatus.CONFIRMED))
        assertEquals(DeliveryStage.PREPARING, stageFor(OrderStatus.READY_FOR_PICKUP))
        assertEquals(DeliveryStage.ON_THE_WAY, stageFor(OrderStatus.ARRIVING))
        assertEquals(DeliveryStage.DELIVERED, stageFor(OrderStatus.DELIVERED))
        assertNull(stageFor(OrderStatus.CANCELLED))
        assertNull(stageFor(OrderStatus.FAILED))
        assertEquals(-1, stageIndex(OrderStatus.CANCELLED))
        assertEquals(3, stageIndex(OrderStatus.DELIVERED))
    }

    @Test fun `scenes match status and rider assignment`() {
        assertEquals(JourneyScene.AWAITING_CONFIRMATION, sceneFor(OrderStatus.PENDING, false))
        assertEquals(JourneyScene.PACKING, sceneFor(OrderStatus.CONFIRMED, false))
        assertEquals(JourneyScene.PACKING, sceneFor(OrderStatus.PREPARING, true))
        assertEquals(JourneyScene.FINDING_RIDER, sceneFor(OrderStatus.READY_FOR_PICKUP, false))
        assertEquals(JourneyScene.RIDER_READY, sceneFor(OrderStatus.READY_FOR_PICKUP, true))
        assertEquals(JourneyScene.ON_THE_WAY, sceneFor(OrderStatus.OUT_FOR_DELIVERY, true))
        assertEquals(JourneyScene.ALMOST_THERE, sceneFor(OrderStatus.ARRIVING, true))
        assertEquals(JourneyScene.DELIVERED, sceneFor(OrderStatus.DELIVERED, true))
        assertEquals(JourneyScene.CANCELLED, sceneFor(OrderStatus.CANCELLED, false))
        assertEquals(JourneyScene.FAILED, sceneFor(OrderStatus.FAILED, false))
    }

    @Test fun `celebration needs a delivered order that has not celebrated`() {
        assertTrue(shouldCelebrate(order(OrderStatus.DELIVERED)))
        assertFalse("never replay a seen celebration", shouldCelebrate(order(OrderStatus.DELIVERED, celebratedAt = "2026-10-29T10:30:00+0530")))
        assertFalse(shouldCelebrate(order(OrderStatus.ARRIVING)))
        assertFalse(shouldCelebrate(order(OrderStatus.CANCELLED)))
        assertFalse(shouldCelebrate(order(OrderStatus.FAILED)))
    }

    @Test fun `cancellation is only offered before pickup`() {
        assertTrue(canCancel(order(OrderStatus.CONFIRMED)))
        assertTrue(canCancel(order(OrderStatus.PREPARING)))
        assertFalse(canCancel(order(OrderStatus.PICKED_UP)))
        assertFalse(canCancel(order(OrderStatus.DELIVERED)))
        assertFalse(canCancel(order(OrderStatus.CANCELLED)))
    }

    @Test fun `order lines snapshot only positive quantities`() {
        val tomato = Product("tomato", "Tomato", "", "produce", pricePaise = 3_200, unit = "1 kg")
        val milk = Product("milk", "Fresh Milk", "", "dairy", pricePaise = 5_200, unit = "1 L")
        val lines = orderLinesFor(mapOf("tomato" to 2, "milk" to 0, "missing" to 3), listOf(tomato, milk))
        assertEquals(1, lines.size)
        assertEquals("tomato", lines.first().productId)
        assertEquals(2, lines.first().quantity)
        assertEquals(6_400, orderSubtotal(lines))
    }

    @Test fun `pricing adds platform fee and free delivery`() {
        assertEquals(0L, DeliveryPricing.deliveryFee(11_200))
        assertEquals(500L, DeliveryPricing.platformFee())
        assertEquals(11_700L, DeliveryPricing.total(11_200))
        assertEquals(0L, DeliveryPricing.deliveryFee(0))
    }

    @Test fun `prices in the storyboard example add up`() {
        // Tomato ₹32 + Banana ₹28 + Milk ₹52 = ₹112, plus ₹5 platform fee, delivery FREE.
        val lines = listOf(
            OrderLine(productId = "tomato", name = "Tomato", unit = "1 kg", quantity = 1, unitPricePaise = 3_200),
            OrderLine(productId = "banana", name = "Banana", unit = "1 kg", quantity = 1, unitPricePaise = 2_800),
            OrderLine(productId = "milk", name = "Fresh Milk", unit = "1 L", quantity = 1, unitPricePaise = 5_200),
        )
        assertEquals(11_200, orderSubtotal(lines))
        assertEquals(11_700, DeliveryPricing.total(orderSubtotal(lines)))
        assertEquals(500L, DeliveryPricing.platformFee())
    }
}

class OrderTimeTest {
    @Test fun `timestamps round-trip through the storage format`() {
        val stamp = com.rushit.app.domain.OrderTime.now()
        // Same default time zone for writing and reading, so the date part must match exactly.
        assertEquals(stamp.substring(0, 10), com.rushit.app.domain.OrderTime.display(stamp, "yyyy-MM-dd"))
        assertTrue(com.rushit.app.domain.OrderTime.displayWithYear(stamp).contains(stamp.substring(0, 4)))
    }

    @Test fun `colon offsets and millisecond timestamps are accepted`() {
        val colon = com.rushit.app.domain.OrderTime.display("2026-10-29T10:15:00+05:30", "yyyy-MM-dd")
        val millis = com.rushit.app.domain.OrderTime.display("2026-10-29T10:15:00.500+0530", "yyyy-MM-dd")
        assertTrue(colon.startsWith("2026-10-29"))
        assertEquals("2026-10-29", millis)
    }

    @Test fun `unparseable timestamps are shown verbatim instead of crashing`() {
        assertEquals("not-a-date", com.rushit.app.domain.OrderTime.display("not-a-date"))
        assertEquals("", com.rushit.app.domain.OrderTime.display(""))
    }
}
