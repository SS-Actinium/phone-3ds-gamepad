package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.controller.WidgetSpot
import dev.ssactinium.phone3dsgamepad.network.LinkStatus
import dev.ssactinium.phone3dsgamepad.network.SessionUiState
import dev.ssactinium.phone3dsgamepad.protocol.ControlProfile
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceA
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceB
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceX
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceY
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing
import dev.ssactinium.phone3dsgamepad.ui.theme.HudMono
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute
import dev.ssactinium.phone3dsgamepad.ui.theme.LedDead
import dev.ssactinium.phone3dsgamepad.ui.theme.LedLive
import dev.ssactinium.phone3dsgamepad.ui.theme.LedWait
import kotlin.math.roundToInt

@Composable
fun GamepadScreen(
    link: SessionUiState,
    profile: ControlProfile,
    arrange: Boolean,
    spots: List<WidgetSpot>,
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
    onStick: (String, StickSample) -> Unit,
    onStickRelease: (String) -> Unit,
    onMoveWidget: (String, Float, Float) -> Unit,
    onToggleProfile: () -> Unit,
    onToggleArrange: () -> Unit,
    onOpenRemap: () -> Unit,
    onDisconnect: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Housing)) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .padding(top = 34.dp),
        ) {
            val w = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val h = constraints.maxHeight.toFloat().coerceAtLeast(1f)
            spots.forEach { spot ->
                val px = (spot.x * w).roundToInt()
                val py = (spot.y * h).roundToInt()
                Box(
                    Modifier
                        .offset { IntOffset(px, py) }
                        .then(
                            if (arrange) {
                                Modifier
                                    .border(1.dp, HingeGold.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                                    .pointerInput(spot.id, spot.x, spot.y, w, h) {
                                        detectDragGestures { change, drag ->
                                            change.consume()
                                            val nx = (spot.x * w + drag.x) / w
                                            val ny = (spot.y * h + drag.y) / h
                                            onMoveWidget(spot.id, nx, ny)
                                        }
                                    }
                            } else Modifier,
                        ),
                ) {
                    when (spot.id) {
                        "circle" -> CirclePad(
                            onMove = { onStick("left", it) },
                            onRelease = { onStickRelease("left") },
                            size = 150.dp,
                        )
                        "cstick" -> CirclePad(
                            onMove = { onStick("right", it) },
                            onRelease = { onStickRelease("right") },
                            size = 92.dp,
                        )
                        "dpad" -> DPad(onPressed = onPress, onReleased = onRelease, size = 108.dp)
                        "A" -> GameButton("A", FaceA, { onPress(PadButton.A) }, { onRelease(PadButton.A) }, size = 66.dp)
                        "B" -> GameButton("B", FaceB, { onPress(PadButton.B) }, { onRelease(PadButton.B) }, size = 66.dp)
                        "X" -> GameButton("X", FaceX, { onPress(PadButton.X) }, { onRelease(PadButton.X) }, size = 66.dp)
                        "Y" -> GameButton("Y", FaceY, { onPress(PadButton.Y) }, { onRelease(PadButton.Y) }, size = 66.dp)
                        "L" -> ShoulderButton("L", { onPress(PadButton.L) }, { onRelease(PadButton.L) }, width = 78.dp)
                        "R" -> ShoulderButton("R", { onPress(PadButton.R) }, { onRelease(PadButton.R) }, width = 78.dp)
                        "ZL" -> ShoulderButton("ZL", { onPress(PadButton.ZL) }, { onRelease(PadButton.ZL) }, width = 58.dp, compact = true)
                        "ZR" -> ShoulderButton("ZR", { onPress(PadButton.ZR) }, { onRelease(PadButton.ZR) }, width = 58.dp, compact = true)
                        "START" -> TinyButton("START", { onPress(PadButton.START) }, { onRelease(PadButton.START) })
                        "SELECT" -> TinyButton("SELECT", { onPress(PadButton.SELECT) }, { onRelease(PadButton.SELECT) })
                        "HOME" -> TinyButton("HOME", { onPress(PadButton.HOME) }, { onRelease(PadButton.HOME) })
                    }
                }
            }
        }
        StatusBar(
            link = link,
            profile = profile,
            arrange = arrange,
            onToggleProfile = onToggleProfile,
            onToggleArrange = onToggleArrange,
            onOpenRemap = onOpenRemap,
            onDisconnect = onDisconnect,
        )
    }
}

@Composable
private fun StatusBar(
    link: SessionUiState,
    profile: ControlProfile,
    arrange: Boolean,
    onToggleProfile: () -> Unit,
    onToggleArrange: () -> Unit,
    onOpenRemap: () -> Unit,
    onDisconnect: () -> Unit,
) {
    val led = when (link.status) {
        LinkStatus.Connected -> LedLive
        LinkStatus.Connecting, LinkStatus.Degraded -> LedWait
        else -> LedDead
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(led))
        Spacer(Modifier.width(6.dp))
        Text(
            link.detail,
            style = HudMono.copy(color = Ink, fontSize = 11.sp),
            modifier = Modifier.weight(1f),
            maxLines = 1,
        )
        TextButton(onClick = onToggleProfile) {
            Text(if (profile == ControlProfile.N3ds) "3DS" else "XBOX", color = HingeGold, fontSize = 12.sp)
        }
        TextButton(onClick = onOpenRemap) { Text("Map", color = Ink, fontSize = 12.sp) }
        TextButton(onClick = onToggleArrange) {
            Text(if (arrange) "Done" else "Move", color = if (arrange) HingeGold else Ink, fontSize = 12.sp)
        }
        TextButton(onClick = onDisconnect) { Text("Leave", color = InkMute, fontSize = 12.sp) }
    }
}
