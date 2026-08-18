package dev.ssactinium.phone3dsgamepad

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ssactinium.phone3dsgamepad.network.ControllerSession
import dev.ssactinium.phone3dsgamepad.network.LinkStatus
import dev.ssactinium.phone3dsgamepad.network.SessionUiState
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PadViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("hinge_pad", Context.MODE_PRIVATE)
    private val session = ControllerSession()

    var host by mutableStateOf(prefs.getString(KEY_HOST, "") ?: "")
        private set
    var port by mutableStateOf(prefs.getString(KEY_PORT, Protocol.DEFAULT_PORT.toString()) ?: Protocol.DEFAULT_PORT.toString())
        private set
    var link by mutableStateOf(SessionUiState())
        private set
    var notice by mutableStateOf("")
        private set
    var busy by mutableStateOf(false)
        private set
    var onPad by mutableStateOf(false)
        private set

    init {
        session.onUi = { next ->
            viewModelScope.launch(Dispatchers.Main) {
                link = next
                if (next.status == LinkStatus.Connected) {
                    onPad = true
                    notice = ""
                }
            }
        }
    }

    fun setHost(value: String) {
        host = value.filter { it.isDigit() || it == '.' }
    }

    fun setPort(value: String) {
        port = value.filter { it.isDigit() }.take(5)
    }

    fun test() {
        val parsed = parsedTarget() ?: return
        persist()
        busy = true
        notice = "Testing…"
        viewModelScope.launch {
            val ok = session.test(parsed.first, parsed.second)
            busy = false
            notice = if (ok) "PC answered on ${parsed.first}:${parsed.second}" else "No hello_ack. Check IP, port, firewall, and that the server is running."
            link = if (ok) {
                SessionUiState(LinkStatus.Connected, parsed.first, parsed.second, "PC reachable")
            } else {
                SessionUiState(LinkStatus.Error, parsed.first, parsed.second, "No reply")
            }
        }
    }

    fun connect() {
        val parsed = parsedTarget() ?: return
        persist()
        busy = true
        notice = ""
        session.connect(parsed.first, parsed.second)
        onPad = true
        busy = false
    }

    fun disconnect() {
        session.disconnect()
        onPad = false
        link = SessionUiState(detail = "Disconnected")
    }

    fun press(button: PadButton) = session.press(button)

    fun release(button: PadButton) = session.release(button)

    fun stick(sample: StickSample) = session.moveStick(sample)

    fun stickRelease() = session.moveStick(StickSample(0f, 0f))

    override fun onCleared() {
        session.shutdown()
        super.onCleared()
    }

    private fun parsedTarget(): Pair<String, Int>? {
        val ip = host.trim()
        val p = port.toIntOrNull()
        if (ip.isBlank() || !ip.matches(Regex("""\d{1,3}(\.\d{1,3}){3}"""))) {
            notice = "Enter a LAN IPv4 address such as 192.168.1.10"
            return null
        }
        if (p == null || p !in 1..65535) {
            notice = "Port must be 1–65535 (default 26760)"
            return null
        }
        return ip to p
    }

    private fun persist() {
        prefs.edit().putString(KEY_HOST, host).putString(KEY_PORT, port).apply()
    }

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
    }
}
