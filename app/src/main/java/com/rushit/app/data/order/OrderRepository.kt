package com.rushit.app.data.order

import android.content.Context
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderLine
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.domain.OrderTime
import com.rushit.app.domain.orderSubtotal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** Snapshot of everything checkout needs to turn a cart into an order. */
data class OrderDraft(
    val items: List<OrderLine>,
    val mode: ShoppingMode,
    val addressSummary: String?,
    val platformFeePaise: Long,
    val deliveryFeePaise: Long,
    val discountPaise: Long = 0,
    val paymentMethod: String = "COD",
)

interface OrderRepository {
    suspend fun placeOrder(draft: OrderDraft): Result<Order>
    suspend fun orders(): List<Order>
    suspend fun order(id: String): Order?
    suspend fun updateStatus(id: String, next: OrderStatus): Result<Order>
    suspend fun markCelebrated(id: String, timestamp: String): Result<Order>
}

internal interface OrderStore {
    fun read(): String?
    fun write(value: String)
}

internal class PreferencesOrderStore(context: Context) : OrderStore {
    private val prefs = context.applicationContext.getSharedPreferences("rushit_orders", Context.MODE_PRIVATE)
    override fun read(): String? = prefs.getString(KEY, null)
    override fun write(value: String) { prefs.edit().putString(KEY, value).apply() }

    private companion object { const val KEY = "orders.v1" }
}

/**
 * Device-local order book for the sample store.
 *
 * It is deliberately small and honest: there is no backend, so orders never leave the device and
 * no payment, stock reservation or fulfilment is implied. All status changes go through
 * [OrderStatus.canTransitionTo], which makes invalid or backward transitions impossible.
 */
class LocalOrderRepository internal constructor(
    private val store: OrderStore,
    private val now: () -> String = { OrderTime.now() },
    private val newId: () -> String = { "RI" + (100_000_000 + (0..899_999_999).random()).toString() },
) : OrderRepository {

    constructor(context: Context) : this(PreferencesOrderStore(context))

    private val lock = Any()

    override suspend fun placeOrder(draft: OrderDraft): Result<Order> = withContext(Dispatchers.IO) {
        synchronized(lock) { runCatching {
            require(draft.items.isNotEmpty()) { "An order needs at least one item." }
            require(!draft.addressSummary.isNullOrBlank()) { "Add a delivery address before placing a demo order." }
            val timestamp = now()
            val items = draft.items.filter { it.quantity > 0 }
            val subtotal = orderSubtotal(items)
            require(subtotal > 0) { "An order needs a positive subtotal." }
            val order = Order(
                id = newId(),
                status = OrderStatus.CONFIRMED,
                items = items,
                subtotalPaise = subtotal,
                deliveryFeePaise = draft.deliveryFeePaise,
                platformFeePaise = draft.platformFeePaise,
                discountPaise = draft.discountPaise,
                totalPaise = (subtotal + draft.deliveryFeePaise + draft.platformFeePaise - draft.discountPaise).coerceAtLeast(0),
                createdAt = timestamp,
                updatedAt = timestamp,
                mode = draft.mode,
                addressSummary = draft.addressSummary,
                paymentMethod = draft.paymentMethod,
                paymentStatus = if (draft.paymentMethod == "Cash on Delivery" || draft.paymentMethod == "COD") "Pending" else "Success",
                isDemo = true,
            )
            val all = readAll()
            writeAll(listOf(order) + all)
            order
        } }
    }

    override suspend fun orders(): List<Order> = withContext(Dispatchers.IO) { synchronized(lock) { readAll() } }

    override suspend fun order(id: String): Order? = withContext(Dispatchers.IO) { synchronized(lock) { readAll().firstOrNull { it.id == id } } }

    override suspend fun updateStatus(id: String, next: OrderStatus): Result<Order> = withContext(Dispatchers.IO) {
        synchronized(lock) { runCatching {
            val all = readAll()
            val index = all.indexOfFirst { it.id == id }
            require(index >= 0) { "Order $id was not found." }
            val current = all[index]
            require(current.isDemo) { "Only demo orders can be updated locally." }
            require(current.status.canTransitionTo(next)) {
                "Cannot move order $id from ${current.status.wireName} to ${next.wireName}."
            }
            val updated = current.copy(status = next, updatedAt = now())
            writeAll(all.toMutableList().apply { set(index, updated) })
            updated
        } }
    }

    override suspend fun markCelebrated(id: String, timestamp: String): Result<Order> = withContext(Dispatchers.IO) {
        synchronized(lock) { runCatching {
            val all = readAll()
            val index = all.indexOfFirst { it.id == id }
            require(index >= 0) { "Order $id was not found." }
            val updated = all[index].copy(celebratedAt = all[index].celebratedAt ?: timestamp)
            writeAll(all.toMutableList().apply { set(index, updated) })
            updated
        } }
    }

    private fun readAll(): List<Order> {
        val raw = store.read() ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList {
                for (index in 0 until array.length()) {
                    val item = array.optJSONObject(index) ?: continue
                    decode(item)?.let(::add)
                }
            }
        }.getOrElse { throw IllegalStateException("Stored orders could not be read.", it) }
    }

    private fun writeAll(orders: List<Order>) {
        val array = JSONArray()
        orders.forEach { array.put(encode(it)) }
        store.write(array.toString())
    }

    private fun encode(order: Order): JSONObject = JSONObject().apply {
        put("id", order.id)
        put("userId", order.userId)
        put("status", order.status.name)
        put("subtotal", order.subtotalPaise)
        put("deliveryFee", order.deliveryFeePaise)
        put("platformFee", order.platformFeePaise)
        put("discount", order.discountPaise)
        put("total", order.totalPaise)
        put("createdAt", order.createdAt)
        put("updatedAt", order.updatedAt)
        put("mode", order.mode.name)
        put("address", order.addressSummary)
        put("demo", order.isDemo)
        put("paymentMethod", order.paymentMethod)
        put("paymentStatus", order.paymentStatus)
        put("celebratedAt", order.celebratedAt)
        put("items", JSONArray().apply {
            order.items.forEach { line ->
                put(JSONObject().apply {
                    put("productId", line.productId)
                    put("variantId", line.variantId)
                    put("variantName", line.variantName)
                    put("name", line.name)
                    put("unit", line.unit)
                    put("quantity", line.quantity)
                    put("unitPrice", line.unitPricePaise)
                    put("imageRes", line.imageRes)
                    put("imageUrl", line.imageUrl)
                })
            }
        })
    }

    private fun decode(json: JSONObject): Order? = runCatching {
        val id = json.optString("id").takeIf { it.isNotBlank() } ?: return@runCatching null
        val status = OrderStatus.fromNameOrNull(json.optString("status")) ?: OrderStatus.PENDING
        val createdAt = json.optString("createdAt")
        val items = json.optJSONArray("items")?.let { array ->
            buildList {
                for (index in 0 until array.length()) {
                    val line = array.optJSONObject(index) ?: continue
                    add(
                        OrderLine(
                            productId = line.optString("productId"),
                            variantId = line.optString("variantId").takeIf { it.isNotBlank() && it != "null" },
                            variantName = line.optString("variantName").takeIf { it.isNotBlank() && it != "null" },
                            name = line.optString("name"),
                            unit = line.optString("unit"),
                            quantity = line.optInt("quantity"),
                            unitPricePaise = line.optLong("unitPrice"),
                            imageRes = line.optInt("imageRes").takeIf { it != 0 },
                            imageUrl = line.optString("imageUrl").takeIf { it.isNotBlank() },
                        ),
                    )
                }
            }
        }.orEmpty()
        Order(
            id = id,
            userId = json.optString("userId", "local"),
            status = status,
            items = items,
            subtotalPaise = json.optLong("subtotal"),
            deliveryFeePaise = json.optLong("deliveryFee"),
            platformFeePaise = json.optLong("platformFee"),
            discountPaise = json.optLong("discount"),
            totalPaise = json.optLong("total"),
            createdAt = createdAt,
            updatedAt = json.optString("updatedAt", createdAt),
            mode = runCatching { ShoppingMode.valueOf(json.optString("mode")) }.getOrDefault(ShoppingMode.GROCERY),
            addressSummary = json.optString("address").takeIf { it.isNotBlank() && it != "null" },
            paymentMethod = json.optString("paymentMethod", "COD"),
            paymentStatus = json.optString("paymentStatus", "Pending"),
            isDemo = json.optBoolean("demo", true),
            celebratedAt = json.optString("celebratedAt").takeIf { it.isNotBlank() && it != "null" },
        )
    }.getOrNull()

    companion object {
        /** Deterministic id generator for tests. */
        internal fun fixedIds(prefix: String): () -> String {
            var counter = 0
            val uuid = UUID.randomUUID().toString().take(4)
            return { "${prefix}${uuid}${counter++}" }
        }
    }
}
