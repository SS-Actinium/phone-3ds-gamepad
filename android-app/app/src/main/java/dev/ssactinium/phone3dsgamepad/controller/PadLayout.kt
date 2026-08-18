package dev.ssactinium.phone3dsgamepad.controller

import org.json.JSONObject

data class WidgetSpot(
    val id: String,
    val x: Float,
    val y: Float,
)

object PadLayout {
    val defaults: List<WidgetSpot> = listOf(
        WidgetSpot("ZL", 0.07f, 0.04f),
        WidgetSpot("L", 0.18f, 0.04f),
        WidgetSpot("R", 0.72f, 0.04f),
        WidgetSpot("ZR", 0.86f, 0.04f),
        WidgetSpot("circle", 0.14f, 0.36f),
        WidgetSpot("dpad", 0.14f, 0.74f),
        WidgetSpot("cstick", 0.72f, 0.76f),
        WidgetSpot("Y", 0.70f, 0.28f),
        WidgetSpot("X", 0.84f, 0.40f),
        WidgetSpot("A", 0.70f, 0.52f),
        WidgetSpot("B", 0.56f, 0.40f),
        WidgetSpot("SELECT", 0.42f, 0.62f),
        WidgetSpot("START", 0.52f, 0.62f),
        WidgetSpot("HOME", 0.47f, 0.74f),
    )

    fun toJson(spots: List<WidgetSpot>): String {
        val root = JSONObject()
        spots.forEach { spot ->
            root.put(spot.id, JSONObject().put("x", spot.x.toDouble()).put("y", spot.y.toDouble()))
        }
        return root.toString()
    }

    fun fromJson(raw: String?): List<WidgetSpot> {
        if (raw.isNullOrBlank()) return defaults
        return try {
            val root = JSONObject(raw)
            defaults.map { base ->
                if (!root.has(base.id)) base
                else {
                    val obj = root.getJSONObject(base.id)
                    WidgetSpot(
                        base.id,
                        obj.optDouble("x", base.x.toDouble()).toFloat().coerceIn(0.02f, 0.92f),
                        obj.optDouble("y", base.y.toDouble()).toFloat().coerceIn(0.02f, 0.88f),
                    )
                }
            }
        } catch (_: Exception) {
            defaults
        }
    }

    fun move(spots: List<WidgetSpot>, id: String, x: Float, y: Float): List<WidgetSpot> {
        return spots.map {
            if (it.id != id) it
            else it.copy(x = x.coerceIn(0.02f, 0.92f), y = y.coerceIn(0.02f, 0.88f))
        }
    }
}
