package dev.ssactinium.phone3dsgamepad

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ssactinium.phone3dsgamepad.controller.PadLayout
import dev.ssactinium.phone3dsgamepad.controller.WidgetSpot
import dev.ssactinium.phone3dsgamepad.network.ControllerSession
import dev.ssactinium.phone3dsgamepad.network.LinkStatus
import dev.ssactinium.phone3dsgamepad.network.SessionUiState
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.Protocol
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

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
    var profile by mutableStateOf(readProfile())
        private set
    var remap by mutableStateOf(readRemap())
        private set
    var spots by mutableStateOf(PadLayout.fromJson(prefs.getString(KEY_LAYOUT, null)))
        private set
    var arrange by mutableStateOf(false)
        private set
    var showRemap by mutableStateOf(false)
        private set
    var invertStick by mutableStateOf(prefs.getBoolean(KEY_INVERT, profile == ControlProfile.N3ds))
        private set

    init {
        session.onUi = { next ->
            viewModelScope.launch(Dispatchers.Main.immediate) {
                link = next
                if (next.status == LinkStatus.Connected) {
                    onPad = true
                    notice = ""
                }
            }
        }
    }

    fun updateHost(value: String) {
        host = value.filter { it.isDigit() || it == '.' }
    }

    fun updatePort(value: String) {
        port = value.filter { it.isDigit() }.take(5)
    }

    fun applyProfile(next: ControlProfile) {
        profile = next
        invertStick = next == ControlProfile.N3ds
        persist()
        if (onPad) session.applyMap(profile, remap, invertStick)
    }

    fun toggleProfile() {
        applyProfile(if (profile == ControlProfile.Xbox) ControlProfile.N3ds else ControlProfile.Xbox)
    }

    fun setRemapEntry(src: String, dst: String) {
        remap = remap + (src to dst)
        persist()
        if (onPad) session.applyMap(profile, remap, invertStick)
    }

    fun clearRemap() {
        remap = emptyMap()
        persist()
        if (onPad) session.applyMap(profile, remap, invertStick)
    }

    fun toggleInvertStick() {
        invertStick = !invertStick
        persist()
        if (onPad) session.applyMap(profile, remap, invertStick)
    }

    fun toggleArrange() {
        arrange = !arrange
        if (!arrange) persist()
    }

    fun toggleRemapSheet() {
        showRemap = !showRemap
    }

    fun moveWidget(id: String, x: Float, y: Float) {
        spots = PadLayout.move(spots, id, x, y)
    }

    fun resetLayout() {
        spots = PadLayout.defaults
        persist()
    }

    fun test() {
        val parsed = parsedTarget() ?: return
        persist()
        busy = true
        notice = "Testing…"
        viewModelScope.launch(Dispatchers.IO) {
            val ok = session.test(parsed.first, parsed.second)
            viewModelScope.launch(Dispatchers.Main) {
                busy = false
                notice = if (ok) {
                    "PC answered on ${parsed.first}:${parsed.second}"
                } else {
                    "No hello_ack. Start HingePad.bat on the PC, check IP and firewall."
                }
                link = if (ok) {
                    SessionUiState(LinkStatus.Connected, parsed.first, parsed.second, "PC reachable")
                } else {
                    SessionUiState(LinkStatus.Error, parsed.first, parsed.second, "No reply")
                }
            }
        }
    }

    fun connect() {
        val parsed = parsedTarget() ?: return
        persist()
        session.connect(parsed.first, parsed.second, profile, remap, invertStick)
        onPad = true
    }

    fun disconnect() {
        session.disconnect()
        onPad = false
        arrange = false
        showRemap = false
        link = SessionUiState(detail = "Disconnected")
    }

    fun press(button: PadButton) = session.press(button)

    fun release(button: PadButton) = session.release(button)

    fun stick(axis: String, sample: StickSample) = session.moveStick(axis, sample)

    fun stickRelease(axis: String) = session.moveStick(axis, StickSample(0f, 0f))

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
        prefs.edit()
            .putString(KEY_HOST, host)
            .putString(KEY_PORT, port)
            .putString(KEY_PROFILE, profile.wire)
            .putString(KEY_REMAP, JSONObject().also { obj -> remap.forEach { (k, v) -> obj.put(k, v) } }.toString())
            .putString(KEY_LAYOUT, PadLayout.toJson(spots))
            .putBoolean(KEY_INVERT, invertStick)
            .apply()
    }

    private fun readProfile(): ControlProfile {
        return if (prefs.getString(KEY_PROFILE, "xbox") == "3ds") ControlProfile.N3ds else ControlProfile.Xbox
    }

    private fun readRemap(): Map<String, String> {
        val raw = prefs.getString(KEY_REMAP, null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            buildMap {
                obj.keys().forEach { key -> put(key, obj.getString(key)) }
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    companion object {
        private const val KEY_HOST = "host"
        private const val KEY_PORT = "port"
        private const val KEY_PROFILE = "profile"
        private const val KEY_REMAP = "remap"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_INVERT = "invert_stick"
    }
}
