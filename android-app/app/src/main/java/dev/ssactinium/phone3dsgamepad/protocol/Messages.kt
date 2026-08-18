package dev.ssactinium.phone3dsgamepad.protocol

object Protocol {
    const val VERSION = "0.1.0"
    const val DEFAULT_PORT = 26760
    const val CLIENT_NAME = "hinge-pad"
    const val MAX_PACKET_BYTES = 2048
    const val HEARTBEAT_MS = 250L
    const val STATE_SYNC_MS = 400L
    const val ACK_STALE_MS = 1500L
    const val AXIS_SEND_MIN_DELTA = 0.012f
    const val AXIS_MAX_HZ = 60
}

enum class PadButton(val wire: String) {
    A("A"),
    B("B"),
    X("X"),
    Y("Y"),
    L("L"),
    R("R"),
    ZL("ZL"),
    ZR("ZR"),
    START("START"),
    SELECT("SELECT"),
    HOME("HOME"),
    DUP("DUP"),
    DDOWN("DDOWN"),
    DLEFT("DLEFT"),
    DRIGHT("DRIGHT"),
    LSTICK("LSTICK"),
}

data class StickSample(val x: Float, val y: Float) {
    fun nearlyEquals(other: StickSample, epsilon: Float = Protocol.AXIS_SEND_MIN_DELTA): Boolean {
        return kotlin.math.abs(x - other.x) < epsilon && kotlin.math.abs(y - other.y) < epsilon
    }
}
