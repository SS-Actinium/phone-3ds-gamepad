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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.ssactinium.phone3dsgamepad.protocol.StickSample
import dev.ssactinium.phone3dsgamepad.ui.theme.PadRing
import dev.ssactinium.phone3dsgamepad.ui.theme.Rubber
import dev.ssactinium.phone3dsgamepad.ui.theme.Thumb
import kotlin.math.hypot
import kotlin.math.min

@Composable
fun CirclePad(
    onMove: (StickSample) -> Unit,
    onRelease: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 168.dp,
    deadzone: Float = 0.10f,
) {
    var knob by remember { mutableStateOf(Offset.Zero) }
    var trackingId by remember { mutableStateOf<Long?>(null) }

    Box(
        modifier
            .size(size)
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
            drawCircle(Rubber, outer, c)
            drawCircle(PadRing.copy(alpha = 0.55f), outer * 0.92f, c, style = Stroke(3f))
            drawCircle(Thumb, outer * 0.32f, c + knob)
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
    if (hypot(x, y) < deadzone) {
        x = 0f
        y = 0f
    }
    return StickSample(x.coerceIn(-1f, 1f), y.coerceIn(-1f, 1f))
}
