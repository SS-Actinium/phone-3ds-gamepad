package dev.ssactinium.phone3dsgamepad.protocol

object Protocol {
    const val VERSION = "0.3.0"
    const val DEFAULT_PORT = 26760
    const val CLIENT_NAME = "hinge-pad"
    const val MAX_PACKET_BYTES = 2048
    const val HEARTBEAT_MS = 400L
    const val STATE_SYNC_MS = 900L
    const val ACK_STALE_MS = 1800L
    const val AXIS_SEND_MIN_DELTA = 0.008f
    const val AXIS_MAX_HZ = 90
}

enum class ControlProfile(val wire: String, val label: String) {
    Xbox("xbox", "Xbox games"),
    N3ds("3ds", "3DS / Azahar"),
}

enum class PadButton(val wire: String, val xboxName: String) {
    A("A", "A"),
    B("B", "B"),
    X("X", "X"),
    Y("Y", "Y"),
    L("L", "LB"),
    R("R", "RB"),
    ZL("ZL", "LT"),
    ZR("ZR", "RT"),
    START("START", "Start"),
    SELECT("SELECT", "Back"),
    HOME("HOME", "Guide"),
    DUP("DUP", "D-Up"),
    DDOWN("DDOWN", "D-Down"),
    DLEFT("DLEFT", "D-Left"),
    DRIGHT("DRIGHT", "D-Right"),
    LSTICK("LSTICK", "L3"),
    RSTICK("RSTICK", "R3"),
}

val REMAP_TARGETS = listOf(
    "A", "B", "X", "Y",
    "L", "R", "ZL", "ZR",
    "START", "SELECT", "HOME",
    "DUP", "DDOWN", "DLEFT", "DRIGHT",
)

data class StickSample(val x: Float, val y: Float) {
    fun nearlyEquals(other: StickSample, epsilon: Float = Protocol.AXIS_SEND_MIN_DELTA): Boolean {
        return kotlin.math.abs(x - other.x) < epsilon && kotlin.math.abs(y - other.y) < epsilon
    }
}
