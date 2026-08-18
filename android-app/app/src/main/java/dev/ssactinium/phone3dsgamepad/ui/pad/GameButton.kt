package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameButton(
    label: String,
    color: Color,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 68.dp,
    lite: Boolean = true,
    enabled: Boolean = true,
) {
    var pressed by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(if (pressed) color.copy(alpha = 0.55f) else color)
            .border(if (lite) 1.dp else 2.dp, Color.White.copy(alpha = if (pressed) 0.45f else 0.16f), CircleShape)
            .pointerInput(label, enabled) {
                if (!enabled) return@pointerInput
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
        Text(label, color = Color.White, fontSize = (size.value * 0.28f).sp)
    }
}
