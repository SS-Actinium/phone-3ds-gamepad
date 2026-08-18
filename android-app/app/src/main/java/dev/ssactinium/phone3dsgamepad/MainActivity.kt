package dev.ssactinium.phone3dsgamepad

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.ssactinium.phone3dsgamepad.ui.connect.ConnectScreen
import dev.ssactinium.phone3dsgamepad.ui.pad.GamepadScreen
import dev.ssactinium.phone3dsgamepad.ui.pad.RemapSheet
import dev.ssactinium.phone3dsgamepad.ui.theme.HingePadTheme

class MainActivity : ComponentActivity() {
    private val model: PadViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= 30) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
        hideSystemBars()
        setContent {
            HingePadTheme {
                HingePadRoot(model)
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            hide(WindowInsetsCompat.Type.systemBars())
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }
}

@Composable
private fun HingePadRoot(model: PadViewModel) {
    if (!model.onPad) {
        ConnectScreen(
            host = model.host,
            port = model.port,
            link = model.link,
            busy = model.busy,
            notice = model.notice,
            profile = model.profile,
            onHost = model::updateHost,
            onPort = model::updatePort,
            onProfile = model::applyProfile,
            onConnect = model::connect,
            onTest = model::test,
        )
        return
    }
    if (model.showRemap) {
        RemapSheet(
            profile = model.profile,
            remap = model.remap,
            invertStick = model.invertStick,
            onPick = model::setRemapEntry,
            onClear = model::clearRemap,
            onToggleInvert = model::toggleInvertStick,
            onPreset = model::applyProfile,
            onClose = model::toggleRemapSheet,
        )
        return
    }
    GamepadScreen(
        link = model.link,
        profile = model.profile,
        arrange = model.arrange,
        spots = model.spots,
        onPress = model::press,
        onRelease = model::release,
        onStick = model::stick,
        onStickRelease = model::stickRelease,
        onMoveWidget = model::moveWidget,
        onToggleProfile = model::toggleProfile,
        onToggleArrange = model::toggleArrange,
        onOpenRemap = model::toggleRemapSheet,
        onDisconnect = model::disconnect,
    )
}
