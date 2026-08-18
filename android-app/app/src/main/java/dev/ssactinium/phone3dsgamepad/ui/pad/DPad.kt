package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.ui.theme.HousingInset
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.InkMute
import dev.ssactinium.phone3dsgamepad.ui.theme.Shoulder
import dev.ssactinium.phone3dsgamepad.ui.theme.ShoulderPressed

@Composable
fun DPad(
    onPressed: (PadButton) -> Unit,
    onReleased: (PadButton) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp,
    enabled: Boolean = true,
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(size * 0.34f, size)
                .clip(RoundedCornerShape(10.dp))
                .background(HousingInset),
        )
        Box(
            Modifier
                .size(size, size * 0.34f)
                .clip(RoundedCornerShape(10.dp))
                .background(HousingInset),
        )
        Arm(PadButton.DUP, "▲", Modifier.align(Alignment.TopCenter), onPressed, onReleased, size * 0.34f, enabled)
        Arm(PadButton.DDOWN, "▼", Modifier.align(Alignment.BottomCenter), onPressed, onReleased, size * 0.34f, enabled)
        Arm(PadButton.DLEFT, "◀", Modifier.align(Alignment.CenterStart), onPressed, onReleased, size * 0.34f, enabled)
        Arm(PadButton.DRIGHT, "▶", Modifier.align(Alignment.CenterEnd), onPressed, onReleased, size * 0.34f, enabled)
        Box(
            Modifier
                .size(size * 0.22f)
                .clip(RoundedCornerShape(20.dp))
                .background(Shoulder)
                .align(Alignment.Center),
        )
    }
}

@Composable
private fun Arm(
    button: PadButton,
    glyph: String,
    modifier: Modifier,
    onPressed: (PadButton) -> Unit,
    onReleased: (PadButton) -> Unit,
    arm: Dp,
    enabled: Boolean,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier
            .size(arm)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) ShoulderPressed else Shoulder)
            .pointerInput(button, enabled) {
                if (!enabled) return@pointerInput
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val down = event.changes.any { it.pressed }
                        event.changes.forEach { it.consume() }
                        if (down && !pressed) {
                            pressed = true
                            onPressed(button)
                        } else if (!down && pressed) {
                            pressed = false
                            onReleased(button)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(glyph, color = if (pressed) Ink else InkMute, fontSize = 14.sp)
    }
}
