package com.rushit.app

import com.rushit.app.data.model.OrderLine
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.data.order.LocalOrderRepository
import com.rushit.app.data.order.OrderDraft
import com.rushit.app.data.order.OrderStore
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OrderRepositoryTest {

    private class MemoryStore(var value: String? = null) : OrderStore {
        override fun read(): String? = value
        override fun write(value: String) { this.value = value }
    }

    private fun draft(quantity: Int = 1) = OrderDraft(
        items = listOf(OrderLine("tomato", "Tomato", "1 kg", quantity, 3_200)),
        mode = ShoppingMode.GROCERY,
        addressSummary = "12 Main Road, Patna, 800001",
        platformFeePaise = 500,
        deliveryFeePaise = 0,
    )

    private fun repository(store: OrderStore = MemoryStore()): Pair<LocalOrderRepository, OrderStore> {
        val order = LocalOrderRepository(store, now = { "2026-10-29T10:15:00+05:30" }, newId = LocalOrderRepository.fixedIds("RI"))
        return order to store
    }

    @Test fun `placed orders are stored with computed totals and survive reload`() = runBlocking {
        val store = MemoryStore()
        val (repo, _) = repository(store)
        val order = repo.placeOrder(draft(quantity = 2)).getOrThrow()
        assertEquals(6_400, order.subtotalPaise)
        assertEquals(6_900, order.totalPaise)
        assertEquals(OrderStatus.CONFIRMED, order.status)
        assertTrue(order.isDemo)

        // A brand new repository over the same storage sees the order: reopening works.
        val (reopened, _) = repository(store)
        val reloaded = reopened.order(order.id)
        assertNotNull(reloaded)
        assertEquals(OrderStatus.CONFIRMED, reloaded!!.status)
        assertEquals(2, reloaded.itemCount)
    }

    @Test fun `rejects empty drafts`() = runBlocking {
        val (repo, _) = repository()
        val result = repo.placeOrder(draft().copy(items = emptyList()))
        assertTrue(result.isFailure)
        assertTrue(repo.orders().isEmpty())
    }

    @Test fun `invalid transitions are refused and leave the order untouched`() = runBlocking {
        val (repo, _) = repository()
        val order = repo.placeOrder(draft()).getOrThrow()
        // CONFIRMED cannot jump straight to DELIVERED
        assertTrue(repo.updateStatus(order.id, OrderStatus.DELIVERED).isFailure)
        assertEquals(OrderStatus.CONFIRMED, repo.order(order.id)!!.status)

        repo.updateStatus(order.id, OrderStatus.PREPARING).getOrThrow()
        repo.updateStatus(order.id, OrderStatus.READY_FOR_PICKUP).getOrThrow()
        repo.updateStatus(order.id, OrderStatus.PICKED_UP).getOrThrow()
        repo.updateStatus(order.id, OrderStatus.OUT_FOR_DELIVERY).getOrThrow()
        assertEquals(OrderStatus.OUT_FOR_DELIVERY, repo.order(order.id)!!.status)

        // No backwards step
        assertTrue(repo.updateStatus(order.id, OrderStatus.PREPARING).isFailure)
        repo.updateStatus(order.id, OrderStatus.DELIVERED).getOrThrow()
        assertEquals(OrderStatus.DELIVERED, repo.order(order.id)!!.status)
    }

    @Test fun `cancelled orders can never be delivered`() = runBlocking {
        val (repo, _) = repository()
        val order = repo.placeOrder(draft()).getOrThrow()
        repo.updateStatus(order.id, OrderStatus.CANCELLED).getOrThrow()
        assertTrue(repo.updateStatus(order.id, OrderStatus.DELIVERED).isFailure)
        assertTrue(repo.updateStatus(order.id, OrderStatus.OUT_FOR_DELIVERY).isFailure)
        assertEquals(OrderStatus.CANCELLED, repo.order(order.id)!!.status)
    }

    @Test fun `celebration is recorded once`() = runBlocking {
        val (repo, _) = repository()
        val order = repo.placeOrder(draft()).getOrThrow()
        val first = repo.markCelebrated(order.id, "2026-10-29T10:30:00+05:30").getOrThrow()
        val second = repo.markCelebrated(order.id, "2026-10-29T11:00:00+05:30").getOrThrow()
        assertEquals("2026-10-29T10:30:00+05:30", first.celebratedAt)
        assertEquals("a second visit must not move the timestamp", first.celebratedAt, second.celebratedAt)
    }

    @Test fun `unknown order ids fail cleanly`() = runBlocking {
        val (repo, _) = repository()
        assertNull(repo.order("RI-does-not-exist"))
        assertTrue(repo.updateStatus("RI-does-not-exist", OrderStatus.PREPARING).isFailure)
        assertTrue(repo.markCelebrated("RI-does-not-exist", "now").isFailure)
    }

    @Test fun `corrupt storage degrades to an empty list`() = runBlocking {
        val store = MemoryStore("{not json")
        val (repo, _) = repository(store)
        assertTrue(repo.orders().isEmpty())
        // and it can still take a new order
        assertTrue(repo.placeOrder(draft()).isSuccess)
    }

    @Test fun `multiple orders keep insertion order`() = runBlocking {
        val (repo, _) = repository()
        val first = repo.placeOrder(draft()).getOrThrow()
        val second = repo.placeOrder(draft(quantity = 3)).getOrThrow()
        val stored = repo.orders()
        assertEquals(listOf(second.id, first.id), stored.map { it.id })
        assertFalse(first.id == second.id)
    }
}
