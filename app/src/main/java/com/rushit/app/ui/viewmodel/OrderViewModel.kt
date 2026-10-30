package com.rushit.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.order.OrderDraft
import com.rushit.app.data.order.OrderRepository
import com.rushit.app.domain.JourneyScene
import com.rushit.app.domain.canCancel
import com.rushit.app.domain.sceneFor
import com.rushit.app.domain.shouldCelebrate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for ordering and the delivery journey.
 *
 * Business state is [order] (loaded from the repository, changed only through declared
 * transitions). Everything else — the confirmation flash, the celebration flag, the demo
 * auto-play toggle — is presentation state and can never write a status by itself.
 */
data class OrderUiState(
    val order: Order? = null,
    val loading: Boolean = false,
    val placing: Boolean = false,
    val transitioning: Boolean = false,
    val ordersLoading: Boolean = false,
    val ordersError: String? = null,
    val error: String? = null,
    val transitionError: String? = null,
    /** True only immediately after a successful placement, to play the confirmation once. */
    val justPlaced: Boolean = false,
    val celebrationPlayed: Boolean = false,
    val demoAutoPlay: Boolean = false,
    val riderAssigned: Boolean = false,
    val orders: List<Order> = emptyList(),
) {
    val scene: JourneyScene? get() = order?.let { sceneFor(it.status, riderAssigned) }
    val canCancel: Boolean get() = order?.let(::canCancel) == true
    /** Delivered and not yet celebrated: the one condition that may start the celebration. */
    val showCelebration: Boolean get() = order != null && !celebrationPlayed && shouldCelebrate(order)
    val isDemo: Boolean get() = order?.isDemo ?: true
    val editable: Boolean get() = !placing && order == null
}

class OrderViewModel internal constructor(
    private val repository: OrderRepository,
    private val now: () -> String,
) : ViewModel() {

    constructor(repository: OrderRepository) : this(repository, { com.rushit.app.domain.OrderTime.now() })

    private val _state = MutableStateFlow(OrderUiState())
    val state = _state.asStateFlow()
    private var demoJob: Job? = null
    private var placementHandled = false
    private var openRequest = 0

    /** Consume navigation/audio once per successful placement, including across recreation. */
    fun consumePlacement(id: String): Boolean {
        if (placementHandled || !_state.value.justPlaced || _state.value.order?.id != id) return false
        placementHandled = true
        return true
    }

    /** Fresh checkout session: clears the previous order view without touching stored orders. */
    fun beginCheckout() {
        if (_state.value.placing) return
        openRequest++
        placementHandled = false
        demoJob?.cancel()
        demoJob = null
        _state.update {
            it.copy(order = null, loading = false, justPlaced = false, celebrationPlayed = false, error = null, transitionError = null, demoAutoPlay = false, riderAssigned = false)
        }
    }

    /** Guards against double taps and repeated submissions before the repository answers. */
    fun placeOrder(draft: OrderDraft) {
        val snapshot = _state.value
        if (snapshot.placing || snapshot.order != null) return
        _state.update { it.copy(placing = true, error = null, transitionError = null) }
        viewModelScope.launch {
            val result = try {
                repository.placeOrder(draft)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                Result.failure(IllegalStateException("The order could not be created."))
            }
            result.fold(
                onSuccess = { order ->
                    _state.update {
                        it.copy(order = order, placing = false, justPlaced = true, loading = false, error = null)
                    }
                },
                onFailure = { failure ->
                    _state.update {
                        it.copy(
                            placing = false,
                            error = failure.message?.takeIf { message -> message.isNotBlank() }
                                ?: "We couldn't create your order. Please try again.",
                        )
                    }
                },
            )
        }
    }

    /** Reopens a stored order at its last persisted status; never replays the celebration. */
    fun open(orderId: String) {
        if (_state.value.order?.id == orderId) return
        demoJob?.cancel()
        demoJob = null
        val request = ++openRequest
        _state.update { it.copy(loading = true, error = null, transitionError = null, order = null, demoAutoPlay = false, riderAssigned = false, justPlaced = false) }
        viewModelScope.launch {
            val result = runCatching { repository.order(orderId) }
            if (request != openRequest) return@launch
            result.fold(
                onSuccess = { order ->
                    _state.update {
                        it.copy(
                            loading = false,
                            order = order,
                            justPlaced = false,
                            celebrationPlayed = order?.celebratedAt != null,
                            transitionError = if (order == null) "That order could not be found on this device." else null,
                            error = null,
                        )
                    }
                },
                onFailure = {
                    _state.update { it.copy(loading = false, error = "We couldn't load this order. Please try again.") }
                },
            )
        }
    }

    /** Loads stored orders for the Orders list and the tracking entry point. */
    fun refreshOrders() {
        if (_state.value.ordersLoading) return
        _state.update { it.copy(ordersLoading = true, ordersError = null) }
        viewModelScope.launch {
            try {
                val stored = repository.orders()
                _state.update { it.copy(orders = stored, ordersLoading = false, ordersError = null) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                _state.update { it.copy(ordersLoading = false, ordersError = "Couldn't load orders stored on this device.") }
            }
        }
    }

    fun clearConfirmationFlag() {
        if (_state.value.justPlaced) _state.update { it.copy(justPlaced = false) }
    }

    /**
     * Moves the demo order exactly one legal step. Only demo orders may be advanced locally; a real
     * backend order would be refused here, which keeps animation previews away from real state.
     */
    fun advanceDemo() {
        val current = _state.value.order ?: return
        if (!current.isDemo) {
            _state.update { it.copy(transitionError = "Only demo orders can be advanced from this screen.") }
            return
        }
        val next = current.status.demoNext() ?: return
        applyTransition(current.id, next)
    }

    fun cancelOrder() {
        val current = _state.value.order ?: return
        if (!current.isDemo || !canCancel(current)) {
            _state.update { it.copy(transitionError = "This order can no longer be cancelled.") }
            return
        }
        applyTransition(current.id, OrderStatus.CANCELLED)
    }

    private fun applyTransition(orderId: String, next: OrderStatus) {
        if (_state.value.transitioning) return
        _state.update { it.copy(transitionError = null, transitioning = true) }
        viewModelScope.launch {
            val result = try {
                repository.updateStatus(orderId, next)
            } catch (cancelled: CancellationException) {
                _state.update { it.copy(transitioning = false) }
                throw cancelled
            } catch (_: Exception) {
                Result.failure(IllegalStateException("That status change isn't allowed."))
            }
            _state.update { it.copy(transitioning = false) }
            result.fold(
                onSuccess = { updated ->
                    _state.update { current ->
                        if (current.order?.id != orderId) return@update current
                        current.copy(
                            order = updated,
                            justPlaced = false,
                            // The local demo has an illustrated rider; real orders still wait for assignment.
                            riderAssigned = if (updated.isDemo && updated.status == OrderStatus.READY_FOR_PICKUP) true else current.riderAssigned,
                        )
                    }
                },
                onFailure = { failure ->
                    _state.update { if (it.order?.id == orderId) it.copy(transitionError = failure.message ?: "That status change isn't allowed.") else it }
                },
            )
        }
    }

    /** Demo-only rider assignment, so the journey can show the "finding rider" state honestly. */
    fun assignDemoRider() {
        if (_state.value.order?.isDemo != true) return
        _state.update { it.copy(riderAssigned = true) }
    }

    /** Explicit opt-in preview that steps through demo stages; disabled for real orders. */
    fun setDemoAutoPlay(enabled: Boolean) {
        demoJob?.cancel()
        demoJob = null
        _state.update { it.copy(demoAutoPlay = enabled) }
        if (!enabled) return
        if (_state.value.order?.isDemo != true) {
            _state.update { it.copy(demoAutoPlay = false) }
            return
        }
        demoJob = viewModelScope.launch {
            while (true) {
                delay(DEMO_STEP_MILLIS)
                val current = _state.value
                val order = current.order ?: break
                if (!current.demoAutoPlay || order.status.isTerminal) {
                    _state.update { it.copy(demoAutoPlay = false) }
                    break
                }
                if (order.status == OrderStatus.READY_FOR_PICKUP) assignDemoRider()
                advanceDemo()
            }
        }
    }

    /** Marks the celebration as seen so it cannot replay when the screen is reopened. */
    fun onCelebrationShown() {
        val order = _state.value.order ?: return
        if (_state.value.celebrationPlayed) return
        _state.update { it.copy(celebrationPlayed = true) }
        if (!order.isDemo) return
        viewModelScope.launch {
            val result = try {
                repository.markCelebrated(order.id, now())
            } catch (_: Exception) {
                Result.failure(IllegalStateException("Could not record the celebration."))
            }
            result.onSuccess { updated -> _state.update { if (it.order?.id == order.id) it.copy(order = updated) else it } }
        }
    }

    companion object {
        const val DEMO_STEP_MILLIS = 3_200L

        fun factory(repository: OrderRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(OrderViewModel::class.java))
                return OrderViewModel(repository) as T
            }
        }
    }
}
