package dev.ssactinium.phone3dsgamepad.protocol

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PacketEncoderTest {
    @Test
    fun buttonPressSerializesCanonicalShape() {
        val json = JSONObject(PacketEncoder.button("A", true))
        assertEquals("button", json.getString("type"))
        assertEquals("A", json.getString("button"))
        assertEquals(1, json.getInt("state"))
    }

    @Test
    fun buttonReleaseSerializesZero() {
        val json = JSONObject(PacketEncoder.button("B", false))
        assertEquals(0, json.getInt("state"))
    }

    @Test
    fun axisUsesNormalizedFloats() {
        val json = JSONObject(PacketEncoder.axis("left", 0.5f, -0.25f))
        assertEquals("axis", json.getString("type"))
        assertEquals("left", json.getString("axis"))
        assertEquals(0.5, json.getDouble("x"), 0.0001)
        assertEquals(-0.25, json.getDouble("y"), 0.0001)
    }

    @Test
    fun helloContainsClientAndVersion() {
        val json = JSONObject(PacketEncoder.hello(1L))
        assertEquals("hello", json.getString("type"))
        assertEquals("hinge-pad", json.getString("client"))
        assertEquals(Protocol.VERSION, json.getString("version"))
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
