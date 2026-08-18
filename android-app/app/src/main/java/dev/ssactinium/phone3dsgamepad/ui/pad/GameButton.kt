package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.ssactinium.phone3dsgamepad.ui.theme.FaceGlyph
import dev.ssactinium.phone3dsgamepad.ui.theme.Housing

@Composable
fun GameButton(
    label: String,
    color: Color,
    onPressed: () -> Unit,
    onReleased: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 74.dp,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (pressed) 0.92f else 1f, label = "btnScale")

    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .semantics { contentDescription = "Button $label" }
            .shadow(if (pressed) 2.dp else 8.dp, CircleShape)
            .clip(CircleShape)
            .background(
                Brush.verticalGradient(
                    listOf(color.copy(alpha = if (pressed) 0.75f else 1f), color.darken()),
                ),
            )
            .border(2.dp, Color.White.copy(alpha = if (pressed) 0.35f else 0.18f), CircleShape)
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
        Box(
            Modifier
                .size(size * 0.78f)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = if (pressed) 0.06f else 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = FaceGlyph.copy(fontSize = (size.value * 0.28f).sp, color = Color.White),
            )
        }
        Box(
            Modifier
                .matchParentSize()
                .clip(CircleShape)
                .background(if (pressed) Housing.copy(alpha = 0.18f) else Color.Transparent),
        )
    }
}

private fun Color.darken(): Color {
    return Color(
        red = (red * 0.62f).coerceIn(0f, 1f),
        green = (green * 0.62f).coerceIn(0f, 1f),
        blue = (blue * 0.62f).coerceIn(0f, 1f),
        alpha = alpha,
    )
}
