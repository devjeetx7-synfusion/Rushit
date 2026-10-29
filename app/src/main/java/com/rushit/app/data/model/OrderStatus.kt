package com.rushit.app.data.model

/**
 * The single authoritative order lifecycle.
 *
 * Every status change must come from an explicit operation (currently the local demo flow) — never
 * from an animation or a timer. [allowedNext] encodes the only legal transitions.
 */
enum class OrderStatus(val wireName: String, val label: String) {
    PENDING("pending", "Awaiting confirmation"),
    CONFIRMED("confirmed", "Confirmed"),
    PREPARING("preparing", "Preparing"),
    READY_FOR_PICKUP("ready_for_pickup", "Ready for pickup"),
    PICKED_UP("picked_up", "Picked up"),
    OUT_FOR_DELIVERY("out_for_delivery", "On the way"),
    ARRIVING("arriving", "Arriving"),
    DELIVERED("delivered", "Delivered"),
    CANCELLED("cancelled", "Cancelled"),
    FAILED("failed", "Failed"),
    ;

    val isTerminal: Boolean get() = this == DELIVERED || this == CANCELLED || this == FAILED

    /** True when [next] is a legal forward (or cancel/fail) transition from this status. */
    fun canTransitionTo(next: OrderStatus): Boolean = next in allowedNext.getValue(this)

    /** The next status the local demo preview may move to, or null at the end. */
    fun demoNext(): OrderStatus? = when (this) {
        PENDING -> CONFIRMED
        CONFIRMED -> PREPARING
        PREPARING -> READY_FOR_PICKUP
        READY_FOR_PICKUP -> PICKED_UP
        PICKED_UP -> OUT_FOR_DELIVERY
        OUT_FOR_DELIVERY -> ARRIVING
        ARRIVING -> DELIVERED
        DELIVERED, CANCELLED, FAILED -> null
    }

    companion object {
        fun fromWire(value: String?): OrderStatus? = entries.firstOrNull { it.wireName == value }

        fun fromNameOrNull(value: String?): OrderStatus? = entries.firstOrNull { it.name == value }

        private val cancellable = setOf(CANCELLED, FAILED)

        private val allowedNext: Map<OrderStatus, Set<OrderStatus>> = mapOf(
            PENDING to setOf(CONFIRMED, CANCELLED, FAILED),
            CONFIRMED to setOf(PREPARING, CANCELLED, FAILED),
            PREPARING to setOf(READY_FOR_PICKUP, CANCELLED, FAILED),
            READY_FOR_PICKUP to setOf(PICKED_UP, CANCELLED, FAILED),
            PICKED_UP to setOf(OUT_FOR_DELIVERY, FAILED),
            OUT_FOR_DELIVERY to setOf(ARRIVING, DELIVERED, FAILED),
            ARRIVING to setOf(DELIVERED, FAILED),
            DELIVERED to emptySet(),
            CANCELLED to emptySet(),
            FAILED to emptySet(),
        )

        /** Statuses that can still be cancelled by the customer. */
        val cancellableStatuses: Set<OrderStatus> = allowedNext.filterValues { it.contains(CANCELLED) }.keys
    }
}
