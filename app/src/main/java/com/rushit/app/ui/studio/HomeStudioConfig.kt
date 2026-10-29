package com.rushit.app.ui.studio

import android.content.Context
import org.json.JSONObject

/** Only explicitly supported, local values are interpreted; this is not an executable remote format. */
enum class Festival(val label: String, val heading: String, val subtitle: String, val color: Long) {
    NORMAL("Normal", "", "", 0xFFFFEEE2),
    DIWALI("Diwali", "Diwali Delights", "Celebrate at your doorstep", 0xFF7D1732),
    PARTY("Party", "It’s Party Time!", "Make every moment special", 0xFF122D69),
    CHRISTMAS("Christmas", "Merry Christmas!", "A little festive magic", 0xFF175639),
    HOLI("Holi", "Holi Specials", "Colors, sweets & more", 0xFFAA2868),
    NEW_YEAR("New Year", "Happy New Year!", "Celebrate a fresh start", 0xFF303465),
    CUSTOM("Custom Festival", "Your celebration", "Make it your own", 0xFF6C3D6E),
}

data class TopBladeConfig(
    val festival: Festival = Festival.NORMAL,
    val heading: String = "",
    val subtitle: String = "",
    val cta: String = "Shop Now",
    val searchPlaceholder: String = "",
    val animate: Boolean = true,
    val durationMs: Int = 210,
)
data class BottomSectionConfig(
    val showModes: Boolean = true,
    val showCategories: Boolean = true,
    val showProducts: Boolean = true,
    val showCart: Boolean = true,
    val groceryTitle: String = "Bestsellers",
)
data class HomeStudioConfig(
    val id: String = "local-draft",
    val version: Int = 1,
    val top: TopBladeConfig = TopBladeConfig(),
    val bottom: BottomSectionConfig = BottomSectionConfig(),
) {
    fun validated() = copy(
        id = "local-draft", version = 1,
        top = top.copy(heading = top.heading.take(80), subtitle = top.subtitle.take(120),
            cta = top.cta.take(30),
            searchPlaceholder = top.searchPlaceholder.take(80), durationMs = top.durationMs.coerceIn(0, 1000)),
        bottom = bottom.copy(groceryTitle = bottom.groceryTitle.take(60)),
    )
}

object HomeStudioCodec {
    fun encode(value: HomeStudioConfig): String {
        val c = value.validated()
        return JSONObject().put("version", c.version).put("id", c.id)
            .put("top", JSONObject().put("festival", c.top.festival.name).put("heading", c.top.heading)
                .put("subtitle", c.top.subtitle).put("cta", c.top.cta).put("search", c.top.searchPlaceholder)
                .put("animate", c.top.animate).put("duration", c.top.durationMs))
            .put("bottom", JSONObject().put("modes", c.bottom.showModes).put("categories", c.bottom.showCategories)
                .put("products", c.bottom.showProducts).put("cart", c.bottom.showCart).put("title", c.bottom.groceryTitle))
            .toString()
    }

    fun decode(raw: String?): HomeStudioConfig = runCatching {
        if (raw.isNullOrBlank()) return HomeStudioConfig()
        val json = JSONObject(raw)
        require(json.optInt("version", -1) == 1)
        val top = json.optJSONObject("top") ?: JSONObject()
        val bottom = json.optJSONObject("bottom") ?: JSONObject()
        HomeStudioConfig(
            top = TopBladeConfig(
                festival = Festival.entries.firstOrNull { it.name == top.optString("festival") } ?: Festival.NORMAL,
                heading = top.optString("heading", ""), subtitle = top.optString("subtitle", ""),
                cta = top.optString("cta", "Shop Now"), searchPlaceholder = top.optString("search", ""),
                animate = top.optBoolean("animate", true), durationMs = top.optInt("duration", 210),
            ),
            bottom = BottomSectionConfig(bottom.optBoolean("modes", true), bottom.optBoolean("categories", true),
                bottom.optBoolean("products", true), bottom.optBoolean("cart", true), bottom.optString("title", "Bestsellers")),
        ).validated()
    }.getOrDefault(HomeStudioConfig())
}

/** Draft is local and isolated; neither production Home nor a remote service reads it. */
class HomeStudioStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("home_studio_draft", Context.MODE_PRIVATE)
    fun load(): HomeStudioConfig = HomeStudioCodec.decode(runCatching { preferences.getString("draft", null) }.getOrNull())
    fun save(config: HomeStudioConfig) { preferences.edit().putString("draft", HomeStudioCodec.encode(config)).apply() }
    fun reset() { preferences.edit().remove("draft").apply() }
}
