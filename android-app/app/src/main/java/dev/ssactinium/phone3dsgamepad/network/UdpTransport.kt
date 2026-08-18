package dev.ssactinium.phone3dsgamepad.network

import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class UdpTransport {
    private var socket: DatagramSocket? = null
    private val open = AtomicBoolean(false)

    val isOpen: Boolean get() = open.get()

    fun bind() {
        if (open.get()) return
        val sock = DatagramSocket(null)
        sock.reuseAddress = true
        sock.bind(InetSocketAddress(0))
        sock.soTimeout = 80
        socket = sock
        open.set(true)
    }

    fun close() {
        open.set(false)
        socket?.close()
        socket = null
    }

    fun send(host: String, port: Int, json: String) {
        val sock = socket ?: return
        val bytes = json.toByteArray(Charsets.UTF_8)
        if (bytes.size > Protocol.MAX_PACKET_BYTES) return
        val address = InetAddress.getByName(host)
        val packet = DatagramPacket(bytes, bytes.size, address, port)
        sock.send(packet)
    }

    fun receiveOrNull(): String? {
        val sock = socket ?: return null
        val buffer = ByteArray(Protocol.MAX_PACKET_BYTES)
        val packet = DatagramPacket(buffer, buffer.size)
        return try {
            sock.receive(packet)
            String(packet.data, 0, packet.length, Charsets.UTF_8)
        } catch (_: SocketTimeoutException) {
            null
        }
    }

    suspend fun sendAndWaitAck(
        host: String,
        port: Int,
        json: String,
        expectedType: String,
        timeoutMs: Long = 800,
    ): Boolean = withContext(Dispatchers.IO) {
        if (!open.get()) bind()
        send(host, port, json)
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            val reply = receiveOrNull() ?: continue
            if (reply.contains("\"type\":\"$expectedType\"")) return@withContext true
        }
        false
    }
}
