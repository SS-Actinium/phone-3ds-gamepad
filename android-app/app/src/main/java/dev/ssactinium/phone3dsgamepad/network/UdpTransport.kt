package dev.ssactinium.phone3dsgamepad.network

import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * All methods must be called from the single I/O thread.
 * DatagramSocket is not safe to send+receive from two threads.
 */
class UdpTransport {
    private var socket: DatagramSocket? = null
    private val open = AtomicBoolean(false)
    private val recvBuf = ByteArray(Protocol.MAX_PACKET_BYTES)

    val isOpen: Boolean get() = open.get()

    fun bindAndConnect(host: String, port: Int) {
        close()
        val sock = DatagramSocket(null)
        sock.reuseAddress = true
        sock.trafficClass = 0x10 // IPTOS_LOWDELAY
        sock.bind(InetSocketAddress(0))
        sock.soTimeout = 5
        val address = InetAddress.getByName(host)
        sock.connect(InetSocketAddress(address, port))
        socket = sock
        open.set(true)
    }

    fun close() {
        open.set(false)
        try {
            socket?.close()
        } catch (_: Exception) {
            // already closed
        }
        socket = null
    }

    fun send(payload: String): Boolean {
        val sock = socket ?: return false
        return try {
            val bytes = payload.toByteArray(Charsets.UTF_8)
            if (bytes.size > Protocol.MAX_PACKET_BYTES) return false
            sock.send(DatagramPacket(bytes, bytes.size))
            true
        } catch (_: Exception) {
            false
        }
    }

    fun receiveOrNull(): String? {
        val sock = socket ?: return null
        return try {
            val packet = DatagramPacket(recvBuf, recvBuf.size)
            sock.receive(packet)
            String(packet.data, 0, packet.length, Charsets.UTF_8)
        } catch (_: SocketTimeoutException) {
            null
        } catch (_: Exception) {
            null
        }
    }
}
