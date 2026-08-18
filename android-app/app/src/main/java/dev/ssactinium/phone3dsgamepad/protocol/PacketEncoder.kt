package dev.ssactinium.phone3dsgamepad.protocol

import org.json.JSONObject

object PacketEncoder {
    fun hello(nowMs: Long = System.currentTimeMillis()): String {
        return JSONObject()
            .put("type", "hello")
            .put("client", Protocol.CLIENT_NAME)
            .put("version", Protocol.VERSION)
            .put("ts", nowMs)
            .toString()
    }

    fun heartbeat(nowMs: Long = System.currentTimeMillis()): String {
        return JSONObject()
            .put("type", "heartbeat")
            .put("ts", nowMs)
            .toString()
    }

    fun disconnect(): String = JSONObject().put("type", "disconnect").toString()

    fun button(name: String, pressed: Boolean): String {
        return JSONObject()
            .put("type", "button")
            .put("button", name)
            .put("state", if (pressed) 1 else 0)
            .toString()
    }

    fun axis(axis: String, x: Float, y: Float): String {
        return JSONObject()
            .put("type", "axis")
            .put("axis", axis)
            .put("x", x.toDouble())
            .put("y", y.toDouble())
            .toString()
    }

    fun trigger(side: String, value: Float): String {
        return JSONObject()
            .put("type", "trigger")
            .put("trigger", side)
            .put("value", value.toDouble())
            .toString()
    }

    fun stateSync(
        buttons: Map<String, Boolean>,
        left: StickSample,
        right: StickSample = StickSample(0f, 0f),
    ): String {
        val buttonObj = JSONObject()
        buttons.forEach { (name, down) -> buttonObj.put(name, if (down) 1 else 0) }
        val axes = JSONObject()
            .put("left", org.json.JSONArray().put(left.x.toDouble()).put(left.y.toDouble()))
            .put("right", org.json.JSONArray().put(right.x.toDouble()).put(right.y.toDouble()))
        return JSONObject()
            .put("type", "state_sync")
            .put("buttons", buttonObj)
            .put("axes", axes)
            .toString()
    }

    fun parseType(json: String): String? {
        return try {
            JSONObject(json).optString("type").ifBlank { null }
        } catch (_: Exception) {
            null
        }
    }
}
