package dev.ssactinium.phone3dsgamepad.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketEncoderTest {
    @Test
    fun buttonPressSerializesCompactShape() {
        val json = JSONObject(PacketEncoder.button("A", true))
        assertEquals("b", json.getString("t"))
        assertEquals("A", json.getString("b"))
        assertEquals(1, json.getInt("s"))
    }

    @Test
    fun buttonReleaseSerializesZero() {
        val json = JSONObject(PacketEncoder.button("B", false))
        assertEquals(0, json.getInt("s"))
    }

    @Test
    fun axisUsesNormalizedFloats() {
        val json = JSONObject(PacketEncoder.axis("left", 0.5f, -0.25f))
        assertEquals("a", json.getString("t"))
        assertEquals("left", json.getString("a"))
        assertEquals(0.5, json.getDouble("x"), 0.0001)
        assertEquals(-0.25, json.getDouble("y"), 0.0001)
    }

    @Test
    fun helloContainsClientVersionAndProfile() {
        val json = JSONObject(PacketEncoder.hello("3ds", 1L))
        assertEquals("hello", json.getString("type"))
        assertEquals("hinge-pad", json.getString("client"))
        assertEquals(Protocol.VERSION, json.getString("version"))
        assertEquals("3ds", json.getString("profile"))
    }

    @Test
    fun parseTypeReadsAck() {
        assertEquals("hello_ack", PacketEncoder.parseType("""{"type":"hello_ack","ok":true}"""))
    }

    @Test
    fun stateSyncIncludesHeldButtons() {
        val raw = PacketEncoder.stateSync(mapOf("A" to true, "L" to false), StickSample(0.1f, 0f))
        val json = JSONObject(raw)
        assertEquals("state_sync", json.getString("type"))
        assertEquals(1, json.getJSONObject("buttons").getInt("A"))
        assertTrue(json.getJSONObject("axes").has("left"))
    }
}
