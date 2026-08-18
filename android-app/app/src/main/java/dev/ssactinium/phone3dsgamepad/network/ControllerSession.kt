package dev.ssactinium.phone3dsgamepad.network

import android.util.Log
import dev.ssactinium.phone3dsgamepad.controller.ControllerState
import dev.ssactinium.phone3dsgamepad.protocol.PacketEncoder
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong

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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var pumpJob: Job? = null
    private var lastAckAt = AtomicLong(0L)
    private var lastAxisSentAt = 0L
    @Volatile private var targetHost: String = ""
    @Volatile private var targetPort: Int = Protocol.DEFAULT_PORT
    @Volatile var uiState: SessionUiState = SessionUiState()
        private set
    var onUi: ((SessionUiState) -> Unit)? = null

    val isLive: Boolean
        get() = uiState.status == LinkStatus.Connected || uiState.status == LinkStatus.Degraded

    suspend fun test(host: String, port: Int): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                if (!transport.isOpen) transport.bind()
                transport.sendAndWaitAck(host, port, PacketEncoder.hello(), "hello_ack")
            }
        } catch (exc: Exception) {
            Log.w(TAG, "test failed", exc)
            false
        }
    }

    fun connect(host: String, port: Int) {
        targetHost = host.trim()
        targetPort = port
        publish(LinkStatus.Connecting, "Connecting to $host:$port")
        pumpJob?.cancel()
        pumpJob = scope.launch {
            try {
                transport.bind()
                transport.send(targetHost, targetPort, PacketEncoder.hello())
                lastAckAt.set(0L)
                receiveLoop()
            } catch (exc: Exception) {
                Log.e(TAG, "connect failed", exc)
                publish(LinkStatus.Error, exc.message ?: "Network error")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                if (transport.isOpen && targetHost.isNotBlank()) {
                    transport.send(targetHost, targetPort, PacketEncoder.disconnect())
                }
            } catch (exc: Exception) {
                Log.w(TAG, "disconnect send failed", exc)
            } finally {
                pumpJob?.cancel()
                transport.close()
                state.releaseAll()
                publish(LinkStatus.Idle, "Disconnected")
            }
        }
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }

    fun press(button: PadButton) = emitButton(button, true)

    fun release(button: PadButton) = emitButton(button, false)

    fun moveStick(sample: StickSample) {
        if (!state.setLeftStick(sample) && sample.x == 0f && sample.y == 0f) return
        val now = System.currentTimeMillis()
        val minGap = 1000L / Protocol.AXIS_MAX_HZ
        if (now - lastAxisSentAt < minGap && !sample.nearlyEquals(StickSample(0f, 0f), 0.001f)) {
            return
        }
        lastAxisSentAt = now
        send(PacketEncoder.axis("left", sample.x, sample.y))
    }

    private fun emitButton(button: PadButton, pressed: Boolean) {
        if (!state.setButton(button, pressed)) return
        send(PacketEncoder.button(button.wire, pressed))
    }

    private fun send(json: String) {
        if (!transport.isOpen || targetHost.isBlank()) return
        try {
            transport.send(targetHost, targetPort, json)
        } catch (exc: Exception) {
            Log.w(TAG, "send failed", exc)
            publish(LinkStatus.Error, exc.message ?: "Send failed")
        }
    }

    private suspend fun receiveLoop() {
        var lastHeartbeat = 0L
        var lastSync = 0L
        while (scope.isActive && transport.isOpen) {
            val now = System.currentTimeMillis()
            val reply = transport.receiveOrNull()
            if (reply != null) {
                val type = PacketEncoder.parseType(reply)
                if (type == "hello_ack" || type == "heartbeat_ack") {
                    lastAckAt.set(now)
                    publish(LinkStatus.Connected, "Connected to $targetHost:$targetPort")
                }
            }
            if (now - lastHeartbeat >= Protocol.HEARTBEAT_MS) {
                send(PacketEncoder.heartbeat(now))
                lastHeartbeat = now
            }
            if (now - lastSync >= Protocol.STATE_SYNC_MS && isLive) {
                val snap = state.snapshot()
                send(PacketEncoder.stateSync(snap.buttons, snap.leftStick, snap.rightStick))
                lastSync = now
            }
            val ackAge = now - lastAckAt.get()
            if (lastAckAt.get() == 0L && now - lastHeartbeat > Protocol.ACK_STALE_MS) {
                publish(LinkStatus.Degraded, "No ack from $targetHost:$targetPort")
            } else if (lastAckAt.get() > 0L && ackAge > Protocol.ACK_STALE_MS) {
                publish(LinkStatus.Degraded, "Link stall — still sending")
            }
            delay(16)
        }
    }

    private fun publish(status: LinkStatus, detail: String) {
        val next = SessionUiState(
            status = status,
            host = targetHost,
            port = targetPort,
            detail = detail,
        )
        uiState = next
        onUi?.invoke(next)
    }

    companion object {
        private const val TAG = "HingePad"
    }
}
