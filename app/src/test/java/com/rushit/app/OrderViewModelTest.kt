package com.rushit.app

import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderLine
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.data.order.OrderDraft
import com.rushit.app.data.order.OrderRepository
import com.rushit.app.domain.JourneyScene
import com.rushit.app.ui.viewmodel.OrderViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OrderViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before fun setup() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private data class Call(val id: String, val status: OrderStatus)

    private class FakeOrderRepository(
        var failPlacement: Boolean = false,
        var placeCalls: Int = 0,
    ) : OrderRepository {
        val stored = linkedMapOf<String, Order>()
        val transitions = mutableListOf<Call>()
        var nextId = 0
        var celebratedCalls = 0

        override suspend fun placeOrder(draft: OrderDraft): Result<Order> {
            placeCalls++
            if (failPlacement) return Result.failure(IllegalStateException("Payment provider unavailable"))
            val id = "RI${1000 + nextId++}"
            val subtotal = draft.items.sumOf { it.lineTotalPaise }
            val order = Order(
                id = id,
                status = OrderStatus.CONFIRMED,
                items = draft.items,
                subtotalPaise = subtotal,
                platformFeePaise = draft.platformFeePaise,
                deliveryFeePaise = draft.deliveryFeePaise,
                totalPaise = subtotal + draft.platformFeePaise + draft.deliveryFeePaise,
                createdAt = "2026-10-29T10:15:00+0530",
                mode = draft.mode,
                addressSummary = draft.addressSummary,
            )
            stored[id] = order
            return Result.success(order)
        }

        override suspend fun orders(): List<Order> = stored.values.toList()

        override suspend fun order(id: String): Order? = stored[id]

        override suspend fun updateStatus(id: String, next: OrderStatus): Result<Order> {
            val current = stored[id] ?: return Result.failure(IllegalArgumentException("missing"))
            transitions += Call(id, next)
            if (!current.status.canTransitionTo(next)) return Result.failure(IllegalStateException("illegal transition"))
            val updated = current.copy(status = next)
            stored[id] = updated
            return Result.success(updated)
        }

        override suspend fun markCelebrated(id: String, timestamp: String): Result<Order> {
            celebratedCalls++
            val current = stored[id] ?: return Result.failure(IllegalArgumentException("missing"))
            val updated = current.copy(celebratedAt = current.celebratedAt ?: timestamp)
            stored[id] = updated
            return Result.success(updated)
        }
    }

    private fun draft() = OrderDraft(
        items = listOf(OrderLine("tomato", "Tomato", "1 kg", 1, 3_200)),
        mode = ShoppingMode.GROCERY,
        addressSummary = "12 Main Road, Patna, 800001",
        platformFeePaise = 500,
        deliveryFeePaise = 0,
    )

    @Test fun `placing an order exposes the confirmation once and stops duplicate taps`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        vm.placeOrder(draft()) // ignored while the first submission is in flight
        advanceUntilIdle()

        assertEquals(1, repo.placeCalls)
        assertTrue(vm.state.value.justPlaced)
        assertEquals(OrderStatus.CONFIRMED, vm.state.value.order?.status)
        assertEquals(JourneyScene.PACKING, vm.state.value.scene)

        vm.clearConfirmationFlag()
        assertFalse(vm.state.value.justPlaced)
        vm.placeOrder(draft()) // a second order is still refused while one is open
        advanceUntilIdle()
        assertEquals(1, repo.placeCalls)
    }

    @Test fun `failed order creation shows an error and no order`() = runTest {
        val repo = FakeOrderRepository(failPlacement = true)
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        advanceUntilIdle()

        assertNull(vm.state.value.order)
        assertNull(vm.state.value.scene)
        assertFalse(vm.state.value.justPlaced)
        assertTrue(vm.state.value.error.orEmpty().contains("Payment provider unavailable"))
    }

    @Test fun `demo stages advance only through legal transitions`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        advanceUntilIdle()

        repeat(6) { vm.advanceDemo(); advanceUntilIdle() }
        assertEquals(OrderStatus.DELIVERED, vm.state.value.order?.status)
        assertEquals(JourneyScene.DELIVERED, vm.state.value.scene)
        // further advances do nothing once terminal
        vm.advanceDemo()
        advanceUntilIdle()
        assertEquals(OrderStatus.DELIVERED, vm.state.value.order?.status)
        assertTrue(repo.transitions.all { it.id == vm.state.value.order?.id })
    }

    @Test fun `celebration is marked once and never replayed`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        advanceUntilIdle()
        while (vm.state.value.order?.status != OrderStatus.DELIVERED) {
            vm.advanceDemo()
            advanceUntilIdle()
        }
        assertTrue(vm.state.value.showCelebration)
        vm.onCelebrationShown()
        advanceUntilIdle()
        assertFalse(vm.state.value.showCelebration)
        assertEquals(1, repo.celebratedCalls)

        vm.onCelebrationShown()
        advanceUntilIdle()
        assertEquals("marking twice must be a no-op", 1, repo.celebratedCalls)
    }

    @Test fun `reopening a delivered order does not celebrate again`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        advanceUntilIdle()
        val id = vm.state.value.order!!.id
        while (vm.state.value.order?.status != OrderStatus.DELIVERED) {
            vm.advanceDemo()
            advanceUntilIdle()
        }
        vm.onCelebrationShown()
        advanceUntilIdle()

        val reopened = OrderViewModel(repo)
        reopened.open(id)
        advanceUntilIdle()
        assertEquals(OrderStatus.DELIVERED, reopened.state.value.order?.status)
        assertTrue(reopened.state.value.celebrationPlayed)
        assertFalse("reopening must not replay the celebration", reopened.state.value.showCelebration)
    }

    @Test fun `cancelling before pickup is terminal and blocks the journey`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        vm.placeOrder(draft())
        advanceUntilIdle()
        vm.cancelOrder()
        advanceUntilIdle()

        assertEquals(OrderStatus.CANCELLED, vm.state.value.order?.status)
        assertEquals(JourneyScene.CANCELLED, vm.state.value.scene)
        assertFalse(vm.state.value.showCelebration)
        assertFalse(vm.state.value.canCancel)

        vm.advanceDemo()
        advanceUntilIdle()
        assertEquals("a cancelled order must not move forward", OrderStatus.CANCELLED, vm.state.value.order?.status)
    }

    @Test fun `non demo orders cannot be advanced locally`() = runTest {
        val repo = FakeOrderRepository()
        val real = Order(
            id = "REAL1",
            status = OrderStatus.OUT_FOR_DELIVERY,
            items = emptyList(),
            subtotalPaise = 0,
            totalPaise = 0,
            createdAt = "2026-10-29T10:15:00+0530",
            isDemo = false,
        )
        repo.stored[real.id] = real
        val vm = OrderViewModel(repo)
        vm.open(real.id)
        advanceUntilIdle()
        vm.advanceDemo()
        advanceUntilIdle()

        assertEquals(OrderStatus.OUT_FOR_DELIVERY, vm.state.value.order?.status)
        assertTrue(vm.state.value.transitionError.orEmpty().contains("demo"))
        assertTrue(repo.transitions.isEmpty())
    }

    @Test fun `auto play is refused for non demo orders`() = runTest {
        val repo = FakeOrderRepository()
        repo.stored["REAL1"] = Order(
            id = "REAL1", status = OrderStatus.OUT_FOR_DELIVERY, items = emptyList(),
            subtotalPaise = 0, totalPaise = 0, createdAt = "2026-10-29T10:15:00+0530", isDemo = false,
        )
        val vm = OrderViewModel(repo)
        vm.open("REAL1")
        advanceUntilIdle()
        vm.setDemoAutoPlay(true)
        advanceUntilIdle()
        assertFalse(vm.state.value.demoAutoPlay)
        assertTrue(repo.transitions.isEmpty())
    }
    @Test fun `placement side effects are consumed only once after success`() = runTest {
        val repo = FakeOrderRepository()
        val vm = OrderViewModel(repo)
        assertFalse(vm.consumePlacement("missing"))
        vm.placeOrder(draft())
        advanceUntilIdle()
        val id = vm.state.value.order!!.id
        assertFalse(vm.consumePlacement("another-order"))
        assertTrue(vm.consumePlacement(id))
        assertFalse(vm.consumePlacement(id))
    }

    @Test fun `ready for pickup starts without a demo rider`() = runTest {
        val vm = OrderViewModel(FakeOrderRepository())
        vm.placeOrder(draft())
        advanceUntilIdle()
        repeat(2) { vm.advanceDemo(); advanceUntilIdle() }
        assertEquals(OrderStatus.READY_FOR_PICKUP, vm.state.value.order?.status)
        assertEquals(JourneyScene.FINDING_RIDER, vm.state.value.scene)
        vm.assignDemoRider()
        assertEquals(JourneyScene.RIDER_READY, vm.state.value.scene)
    }

    @Test fun `real orders cannot be cancelled through demo controls`() = runTest {
        val repo = FakeOrderRepository()
        repo.stored["REAL"] = Order(id = "REAL", status = OrderStatus.CONFIRMED, items = emptyList(),
            subtotalPaise = 0, totalPaise = 0, createdAt = "2026-10-29T10:15:00+0530", isDemo = false)
        val vm = OrderViewModel(repo)
        vm.open("REAL")
        advanceUntilIdle()
        vm.cancelOrder()
        advanceUntilIdle()
        assertTrue(repo.transitions.isEmpty())
        assertEquals(OrderStatus.CONFIRMED, vm.state.value.order?.status)
    }

}
