package dev.ssactinium.phone3dsgamepad.protocol

import org.json.JSONObject

object PacketEncoder {
    fun hello(profile: String, nowMs: Long = System.currentTimeMillis()): String {
        return """{"type":"hello","client":"${Protocol.CLIENT_NAME}","version":"${Protocol.VERSION}","profile":"$profile","ts":$nowMs}"""
    }

    fun heartbeat(): String = """{"t":"h"}"""

    fun disconnect(): String = """{"type":"disconnect"}"""

    fun button(name: String, pressed: Boolean): String {
        return """{"t":"b","b":"$name","s":${if (pressed) 1 else 0}}"""
    }

    fun axis(axis: String, x: Float, y: Float): String {
        return """{"t":"a","a":"$axis","x":${fmt(x)},"y":${fmt(y)}}"""
    }

    fun profile(name: String): String = """{"type":"profile","profile":"$name"}"""

    fun remap(map: Map<String, String>): String {
        val body = map.entries.joinToString(",") { (k, v) -> "\"$k\":\"$v\"" }
        return """{"type":"remap","map":{$body}}"""
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
        val key = "\"type\":\""
        val i = json.indexOf(key)
        if (i < 0) {
            if (json.contains("\"t\":\"h\"")) return "heartbeat"
            return null
        }
        val start = i + key.length
        val end = json.indexOf('"', start)
        if (end <= start) return null
        return json.substring(start, end)
    }

    private fun fmt(value: Float): String {
        val clamped = value.coerceIn(-1f, 1f)
        return ((clamped * 1000f).toInt() / 1000f).toString()
    }
}
