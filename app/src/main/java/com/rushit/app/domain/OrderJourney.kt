package com.rushit.app.domain

import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderLine
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.Product
import com.rushit.app.data.model.SavedAddress
import com.rushit.app.data.model.ShoppingMode

/** Illustrative charges for the sample store; no payment is ever taken. */
object DeliveryPricing {
    const val PLATFORM_FEE_PAISE = 500L
    const val DELIVERY_FEE_PAISE = 0L
    const val FREE_DELIVERY_THRESHOLD_PAISE = 19_900L

    fun platformFee(): Long = PLATFORM_FEE_PAISE

    fun deliveryFee(itemsPaise: Long): Long =
        if (itemsPaise >= FREE_DELIVERY_THRESHOLD_PAISE) 0L else DELIVERY_FEE_PAISE

    fun total(itemsPaise: Long, discountPaise: Long = 0): Long =
        (itemsPaise + deliveryFee(itemsPaise) + platformFee() - discountPaise).coerceAtLeast(0)
}

/** The four customer-facing stages of the storyboard timeline. */
enum class DeliveryStage(val label: String) {
    CONFIRMED("Confirmed"),
    PREPARING("Preparing"),
    ON_THE_WAY("On the way"),
    DELIVERED("Delivered"),
}

/** Which scene the journey screen renders. Purely derived from [OrderStatus]; never persisted. */
enum class JourneyScene {
    AWAITING_CONFIRMATION,
    PACKING,
    FINDING_RIDER,
    RIDER_READY,
    ON_THE_WAY,
    ALMOST_THERE,
    DELIVERED,
    CANCELLED,
    FAILED,
}

/**
 * Stage shown as active for a status. Terminal failures map to the stage reached before failing so
 * the timeline never claims progress that did not happen.
 */
fun stageFor(status: OrderStatus): DeliveryStage? = when (status) {
    OrderStatus.PENDING, OrderStatus.CONFIRMED -> DeliveryStage.CONFIRMED
    OrderStatus.PREPARING, OrderStatus.READY_FOR_PICKUP -> DeliveryStage.PREPARING
    OrderStatus.PICKED_UP, OrderStatus.OUT_FOR_DELIVERY, OrderStatus.ARRIVING -> DeliveryStage.ON_THE_WAY
    OrderStatus.DELIVERED -> DeliveryStage.DELIVERED
    OrderStatus.CANCELLED, OrderStatus.FAILED -> null
}

/** Index of the active stage (0..3), or -1 for cancelled/failed orders. */
fun stageIndex(status: OrderStatus): Int = stageFor(status)?.ordinal ?: -1

/**
 * Scene for the current status. [riderAssigned] only affects the pre-pickup stage: without a real
 * delivery partner the app must say it is still finding one instead of inventing a rider.
 */
fun sceneFor(status: OrderStatus, riderAssigned: Boolean): JourneyScene = when (status) {
    OrderStatus.PENDING -> JourneyScene.AWAITING_CONFIRMATION
    OrderStatus.CONFIRMED, OrderStatus.PREPARING -> JourneyScene.PACKING
    OrderStatus.READY_FOR_PICKUP -> if (riderAssigned) JourneyScene.RIDER_READY else JourneyScene.FINDING_RIDER
    OrderStatus.PICKED_UP -> JourneyScene.RIDER_READY
    OrderStatus.OUT_FOR_DELIVERY -> JourneyScene.ON_THE_WAY
    OrderStatus.ARRIVING -> JourneyScene.ALMOST_THERE
    OrderStatus.DELIVERED -> JourneyScene.DELIVERED
    OrderStatus.CANCELLED -> JourneyScene.CANCELLED
    OrderStatus.FAILED -> JourneyScene.FAILED
}

/** Whether the celebration may run: delivered, and never shown for this order before. */
fun shouldCelebrate(order: Order): Boolean =
    order.status == OrderStatus.DELIVERED && order.celebratedAt == null

/** Builds the immutable order lines for the products currently in a cart. */
fun orderLinesFor(cart: Map<String, Int>, products: List<Product>): List<OrderLine> =
    cart.mapNotNull { (key, quantity) ->
        if (quantity <= 0) return@mapNotNull null
        val (productId, variantId) = parseCartKey(key)
        val product = products.firstOrNull { it.id == productId } ?: return@mapNotNull null
        val variant = product.variants.find { it.id == variantId }
        val price = variant?.pricePaise ?: product.pricePaise
        val variantName = variant?.attributes?.entries?.joinToString(", ") { "${it.key}: ${it.value}" }
        OrderLine(
            productId = product.id,
            variantId = variantId,
            variantName = variantName,
            name = product.name,
            unit = product.unit,
            quantity = quantity,
            unitPricePaise = price,
            imageRes = product.imageRes,
            imageUrl = product.imageUrl,
        )
    }

fun orderSubtotal(lines: List<OrderLine>): Long = lines.sumOf { it.lineTotalPaise }

/** Human-readable address snapshot; blank parts are dropped instead of leaving stray commas. */
fun addressSummary(address: SavedAddress?): String? =
    address?.summary?.takeIf { it.isNotBlank() }

fun modeDisplayName(mode: ShoppingMode): String = mode.label

/** Cancellation is only possible while the order has not left the store. */
fun canCancel(order: Order): Boolean = order.status in OrderStatus.cancellableStatuses
