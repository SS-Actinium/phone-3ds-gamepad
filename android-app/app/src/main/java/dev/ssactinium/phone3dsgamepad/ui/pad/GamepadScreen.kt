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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
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
                Arrangeable(
                    id = spot.id,
                    xFrac = spot.x,
                    yFrac = spot.y,
                    parentW = w,
                    parentH = h,
                    arrange = arrange,
                    onMove = { x, y -> onMoveWidget(spot.id, x, y) },
                ) {
                    PadWidget(
                        id = spot.id,
                        live = !arrange,
                        onPress = onPress,
                        onRelease = onRelease,
                        onStick = onStick,
                        onStickRelease = onStickRelease,
                    )
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
private fun Arrangeable(
    id: String,
    xFrac: Float,
    yFrac: Float,
    parentW: Float,
    parentH: Float,
    arrange: Boolean,
    onMove: (Float, Float) -> Unit,
    content: @Composable () -> Unit,
) {
    var drag by remember(id) { mutableStateOf(Offset.Zero) }
    var originX by remember(id) { mutableStateOf(xFrac) }
    var originY by remember(id) { mutableStateOf(yFrac) }
    LaunchedEffect(xFrac, yFrac) {
        if (drag == Offset.Zero) {
            originX = xFrac
            originY = yFrac
        }
    }
    val px = (originX * parentW + drag.x).roundToInt()
    val py = (originY * parentH + drag.y).roundToInt()
    Box(
        Modifier
            .offset { IntOffset(px, py) }
            .then(
                if (arrange) {
                    Modifier
                        .border(1.dp, HingeGold.copy(alpha = 0.85f), RoundedCornerShape(8.dp))
                        .pointerInput(id, parentW, parentH) {
                            detectDragGestures(
                                onDragEnd = {
                                    val nx = (originX + drag.x / parentW).coerceIn(0.02f, 0.90f)
                                    val ny = (originY + drag.y / parentH).coerceIn(0.02f, 0.86f)
                                    originX = nx
                                    originY = ny
                                    onMove(nx, ny)
                                    drag = Offset.Zero
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    drag += amount
                                },
                            )
                        }
                } else {
                    Modifier
                },
            ),
    ) { content() }
}

@Composable
private fun PadWidget(
    id: String,
    live: Boolean,
    onPress: (PadButton) -> Unit,
    onRelease: (PadButton) -> Unit,
    onStick: (String, StickSample) -> Unit,
    onStickRelease: (String) -> Unit,
) {
    when (id) {
        "circle" -> CirclePad(
            onMove = { onStick("left", it) },
            onRelease = { onStickRelease("left") },
            size = 158.dp,
            enabled = live,
        )
        "cstick" -> CirclePad(
            onMove = { onStick("right", it) },
            onRelease = { onStickRelease("right") },
            size = 96.dp,
            enabled = live,
        )
        "dpad" -> DPad(onPressed = onPress, onReleased = onRelease, size = 108.dp, enabled = live)
        "A" -> GameButton("A", FaceA, { onPress(PadButton.A) }, { onRelease(PadButton.A) }, size = 70.dp, enabled = live)
        "B" -> GameButton("B", FaceB, { onPress(PadButton.B) }, { onRelease(PadButton.B) }, size = 70.dp, enabled = live)
        "X" -> GameButton("X", FaceX, { onPress(PadButton.X) }, { onRelease(PadButton.X) }, size = 70.dp, enabled = live)
        "Y" -> GameButton("Y", FaceY, { onPress(PadButton.Y) }, { onRelease(PadButton.Y) }, size = 70.dp, enabled = live)
        "L" -> ShoulderButton("L", { onPress(PadButton.L) }, { onRelease(PadButton.L) }, width = 78.dp, enabled = live)
        "R" -> ShoulderButton("R", { onPress(PadButton.R) }, { onRelease(PadButton.R) }, width = 78.dp, enabled = live)
        "ZL" -> ShoulderButton("ZL", { onPress(PadButton.ZL) }, { onRelease(PadButton.ZL) }, width = 58.dp, compact = true, enabled = live)
        "ZR" -> ShoulderButton("ZR", { onPress(PadButton.ZR) }, { onRelease(PadButton.ZR) }, width = 58.dp, compact = true, enabled = live)
        "START" -> TinyButton("START", { onPress(PadButton.START) }, { onRelease(PadButton.START) }, enabled = live)
        "SELECT" -> TinyButton("SELECT", { onPress(PadButton.SELECT) }, { onRelease(PadButton.SELECT) }, enabled = live)
        "HOME" -> TinyButton("HOME", { onPress(PadButton.HOME) }, { onRelease(PadButton.HOME) }, enabled = live)
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
            if (arrange) "Drag a control. Tap Done when it feels right." else link.detail,
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
