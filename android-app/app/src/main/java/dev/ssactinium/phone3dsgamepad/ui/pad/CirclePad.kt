package dev.ssactinium.phone3dsgamepad.ui.pad

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import dev.ssactinium.phone3dsgamepad.ui.theme.HingeGold
import dev.ssactinium.phone3dsgamepad.ui.theme.PadRing
import dev.ssactinium.phone3dsgamepad.ui.theme.Rubber
import dev.ssactinium.phone3dsgamepad.ui.theme.Thumb
import dev.ssactinium.phone3dsgamepad.ui.theme.ThumbLit
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun CirclePad(
    onMove: (StickSample) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 196.dp,
    deadzone: Float = 0.08f,
) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    var trackingId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier
            .size(size)
            .semantics { contentDescription = "Circle Pad" }
            .pointerInput(deadzone) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val maxR = min(this.size.width, this.size.height) / 2f * 0.78f

                        if (trackingId == null) {
                            val down = event.changes.firstOrNull { it.pressed && !it.previousPressed }
                            if (down != null) {
                                trackingId = down.id.value
                                val sample = sampleFrom(down.position, center, maxR, deadzone)
                                knob = Offset(sample.x * maxR, -sample.y * maxR)
                                onMove(sample)
                                down.consume()
                            }
                        } else {
                            val tracked = event.changes.firstOrNull { it.id.value == trackingId }
                            if (tracked == null) continue
                            if (tracked.changedToUpIgnoreConsumed() || !tracked.pressed) {
                                trackingId = null
                                knob = Offset.Zero
                                onRelease()
                                tracked.consume()
                            } else {
                                val sample = sampleFrom(tracked.position, center, maxR, deadzone)
                                knob = Offset(sample.x * maxR, -sample.y * maxR)
                                onMove(sample)
                                tracked.consume()
                            }
                        }
                    }
                }
            },
    ) {
        Canvas(Modifier.matchParentSize()) {
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            val outer = min(this.size.width, this.size.height) / 2f
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF2A2E33), Rubber),
                    center = c,
                    radius = outer,
                ),
                radius = outer,
                center = c,
            )
            drawCircle(
                color = PadRing.copy(alpha = 0.7f),
                radius = outer * 0.92f,
                center = c,
                style = Stroke(width = 5f),
            )
            drawCircle(
                color = HingeGold.copy(alpha = 0.28f),
                radius = outer * 0.18f,
                center = c,
                style = Stroke(width = 2f),
            )
            val thumbR = outer * 0.34f
            val thumbCenter = c + knob
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(ThumbLit, Thumb),
                    center = thumbCenter + Offset(-thumbR * 0.2f, -thumbR * 0.25f),
                    radius = thumbR * 1.2f,
                ),
                radius = thumbR,
                center = thumbCenter,
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.12f),
                radius = thumbR * 0.55f,
                center = thumbCenter + Offset(-thumbR * 0.12f, -thumbR * 0.16f),
            )
        }
    }
}

private fun sampleFrom(pos: Offset, center: Offset, maxR: Float, deadzone: Float): StickSample {
    val dx = pos.x - center.x
    val dy = pos.y - center.y
    val mag = hypot(dx, dy)
    val clamped = if (mag > maxR && mag > 0f) maxR / mag else 1f
    var x = (dx * clamped) / maxR
    var y = -(dy * clamped) / maxR
    val length = hypot(x, y)
    if (length < deadzone) {
        x = 0f
        y = 0f
    }
    return StickSample(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
}
