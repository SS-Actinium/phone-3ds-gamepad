package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
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
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.Ink
import dev.ssactinium.phone3dsgamepad.ui.theme.Shoulder
import dev.ssactinium.phone3dsgamepad.ui.theme.ShoulderPressed

@Composable
fun ShoulderButton(
    label: String,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 92.dp,
    compact: Boolean = false,
) {
    var pressed by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp, topStart = 8.dp, topEnd = 8.dp)
    Box(
        modifier
            .width(width)
            .height(if (compact) 36.dp else 48.dp)
            .clip(shape)
            .background(if (pressed) ShoulderPressed else Shoulder)
            .border(1.dp, HingeGold.copy(alpha = if (pressed) 0.55f else 0.22f), shape)
            .pointerInput(label) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Main)
                        val down = event.changes.any { it.pressed }
                        event.changes.forEach { it.consume() }
                        if (down && !pressed) {
                            pressed = true
                            onPressed()
                        } else if (!down && pressed) {
                            pressed = false
                            onReleased()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = Ink, fontSize = if (compact) 13.sp else 16.sp, letterSpacing = 1.sp)
    }
}
