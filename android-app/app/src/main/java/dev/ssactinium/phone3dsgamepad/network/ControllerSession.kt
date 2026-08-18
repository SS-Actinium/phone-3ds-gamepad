package dev.ssactinium.phone3dsgamepad.network

import android.util.Log
import dev.ssactinium.phone3dsgamepad.controller.ControllerState
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.protocol.PacketEncoder
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

enum class LinkStatus {
    Idle,
    Connecting,
    Connected,
    Degraded,
    Error,
}

data class SessionUiState(
    val status: LinkStatus = LinkStatus.Idle,
    val host: String = "",
    val port: Int = Protocol.DEFAULT_PORT,
    val detail: String = "Disconnected",
)

class ControllerSession {
    private val transport = UdpTransport()
    private val state = ControllerState()
    private val outbox = ConcurrentLinkedQueue<String>()
    private val running = AtomicBoolean(false)
    private val lastAckAt = AtomicLong(0L)
    private val lastUi = AtomicReference(SessionUiState())
    private var worker: Thread? = null
    @Volatile private var targetHost: String = ""
    @Volatile private var targetPort: Int = Protocol.DEFAULT_PORT
    @Volatile private var profile: ControlProfile = ControlProfile.Xbox
    @Volatile private var remap: Map<String, String> = emptyMap()
    @Volatile private var invertStick: Boolean = false
    private val lastAxisSentAt = java.util.concurrent.ConcurrentHashMap<String, Long>()
    private val pendingAxis = java.util.concurrent.ConcurrentHashMap<String, StickSample>()
    @Volatile var uiState: SessionUiState = SessionUiState()
        private set
    var onUi: ((SessionUiState) -> Unit)? = null

    fun test(host: String, port: Int): Boolean {
        val probe = UdpTransport()
        return try {
            probe.bindAndConnect(host.trim(), port)
            probe.send(PacketEncoder.hello(profile.wire))
            val deadline = System.currentTimeMillis() + 800
            var ok = false
            while (System.currentTimeMillis() < deadline) {
                val reply = probe.receiveOrNull() ?: continue
                if (PacketEncoder.parseType(reply) == "hello_ack") {
                    ok = true
                    break
                }
            }
            ok
        } catch (exc: Exception) {
            Log.w(TAG, "test failed", exc)
            false
        } finally {
            probe.close()
        }
    }

    fun connect(
        host: String,
        port: Int,
        profile: ControlProfile,
        remap: Map<String, String>,
        invertStick: Boolean,
    ) {
        targetHost = host.trim()
        targetPort = port
        this.profile = profile
        this.remap = remap
        this.invertStick = invertStick
        stopWorker()
        publish(LinkStatus.Connecting, "Connecting to $host:$port")
        running.set(true)
        worker = thread(name = "hinge-udp", isDaemon = true, priority = Thread.MAX_PRIORITY) {
            ioLoop()
        }
    }

    fun disconnect() {
        outbox.clear()
        outbox.offer(PacketEncoder.disconnect())
        try {
            Thread.sleep(40)
        } catch (_: InterruptedException) {
            // ignore
        }
        stopWorker()
        state.releaseAll()
        publish(LinkStatus.Idle, "Disconnected")
    }

    fun shutdown() {
        disconnect()
    }

    fun press(button: PadButton) {
        if (!state.setButton(button, true)) return
        enqueue(PacketEncoder.button(button.wire, true))
    }

    fun release(button: PadButton) {
        if (!state.setButton(button, false)) return
        enqueue(PacketEncoder.button(button.wire, false))
    }

    fun moveStick(axis: String, sample: StickSample) {
        if (!state.setStick(axis, sample) && sample.x == 0f && sample.y == 0f) return
        val now = System.currentTimeMillis()
        val minGap = 1000L / Protocol.AXIS_MAX_HZ
        val last = lastAxisSentAt[axis] ?: 0L
        val centered = sample.nearlyEquals(StickSample(0f, 0f), 0.001f)
        if (!centered && now - last < minGap) {
            pendingAxis[axis] = sample
            return
        }
        pendingAxis.remove(axis)
        lastAxisSentAt[axis] = now
        enqueue(PacketEncoder.axis(axis, sample.x, sample.y))
    }

    private fun flushPendingAxes() {
        val now = System.currentTimeMillis()
        val minGap = 1000L / Protocol.AXIS_MAX_HZ
        for (axis in pendingAxis.keys.toList()) {
            val last = lastAxisSentAt[axis] ?: 0L
            if (now - last < minGap) continue
            val sample = pendingAxis.remove(axis) ?: continue
            lastAxisSentAt[axis] = now
            enqueue(PacketEncoder.axis(axis, sample.x, sample.y))
        }
    }

    fun applyMap(profile: ControlProfile, remap: Map<String, String>, invertStick: Boolean) {
        this.profile = profile
        this.remap = remap
        this.invertStick = invertStick
        enqueue(PacketEncoder.profile(profile.wire))
        enqueue(PacketEncoder.remap(remap))
        enqueue(PacketEncoder.options(invertStick, invertStick))
    }

    private fun enqueue(packet: String) {
        if (outbox.size > 80) outbox.poll()
        outbox.offer(packet)
    }

    private fun stopWorker() {
        running.set(false)
        worker?.interrupt()
        worker = null
        transport.close()
    }

    private fun ioLoop() {
        try {
            transport.bindAndConnect(targetHost, targetPort)
            enqueue(PacketEncoder.hello(profile.wire))
            enqueue(PacketEncoder.profile(profile.wire))
            enqueue(PacketEncoder.remap(remap))
            enqueue(PacketEncoder.options(invertStick, invertStick))
            var lastHeartbeat = 0L
            var lastSync = 0L
            var fails = 0
            while (running.get()) {
                flushPendingAxes()
                var outgoing = outbox.poll()
                while (outgoing != null) {
                    if (!transport.send(outgoing)) {
                        fails += 1
                    } else {
                        fails = 0
                    }
                    outgoing = outbox.poll()
                }
                val reply = transport.receiveOrNull()
                val now = System.currentTimeMillis()
                if (reply != null) {
                    val type = PacketEncoder.parseType(reply)
                    if (type == "hello_ack" || type == "heartbeat_ack") {
                        lastAckAt.set(now)
                        if (type == "hello_ack") {
                            publish(LinkStatus.Connected, "Connected to $targetHost:$targetPort")
                        } else if (uiState.status != LinkStatus.Connected) {
                            publish(LinkStatus.Connected, "Connected to $targetHost:$targetPort")
                        }
                    }
                }
                if (now - lastHeartbeat >= Protocol.HEARTBEAT_MS) {
                    enqueue(PacketEncoder.heartbeat())
                    lastHeartbeat = now
                }
                if (now - lastSync >= Protocol.STATE_SYNC_MS && uiState.status == LinkStatus.Connected) {
                    val snap = state.snapshot()
                    enqueue(PacketEncoder.stateSync(snap.buttons, snap.leftStick, snap.rightStick))
                    lastSync = now
                }
                val ack = lastAckAt.get()
                if (ack == 0L && now - lastHeartbeat > Protocol.ACK_STALE_MS) {
                    publish(LinkStatus.Degraded, "No ack — still sending")
                } else if (ack > 0L && now - ack > Protocol.ACK_STALE_MS && uiState.status == LinkStatus.Connected) {
                    publish(LinkStatus.Degraded, "Link stall — still sending")
                }
                if (fails >= 40) {
                    publish(LinkStatus.Degraded, "Network drops — still retrying")
                    fails = 0
                }
            }
        } catch (exc: Exception) {
            Log.e(TAG, "io loop", exc)
            publish(LinkStatus.Error, exc.message ?: "Network error")
        } finally {
            transport.close()
        }
    }

    private fun publish(status: LinkStatus, detail: String) {
        val next = SessionUiState(status, targetHost, targetPort, detail)
        val prev = lastUi.get()
        if (prev.status == next.status && prev.detail == next.detail) return
        lastUi.set(next)
        uiState = next
        onUi?.invoke(next)
    }

    companion object {
        private const val TAG = "HingePad"
    }
}
