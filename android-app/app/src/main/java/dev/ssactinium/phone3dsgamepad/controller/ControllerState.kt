package dev.ssactinium.phone3dsgamepad.controller

import dev.ssactinium.phone3dsgamepad.protocol.PadButton
import dev.ssactinium.phone3dsgamepad.protocol.StickSample

data class ControllerSnapshot(
    val buttons: Map<String, Boolean> = emptyMap(),
    val leftStick: StickSample = StickSample(0f, 0f),
    val rightStick: StickSample = StickSample(0f, 0f),
)

class ControllerState {
    private val buttons = mutableMapOf<String, Boolean>()
    @Volatile var leftStick: StickSample = StickSample(0f, 0f)
        private set
    @Volatile var rightStick: StickSample = StickSample(0f, 0f)
        private set

    fun setButton(button: PadButton, pressed: Boolean): Boolean {
        val previous = buttons[button.wire] == true
        if (previous == pressed) return false
        buttons[button.wire] = pressed
        return true
    }

    fun setStick(axis: String, sample: StickSample): Boolean {
        val current = if (axis == "right") rightStick else leftStick
        if (current.nearlyEquals(sample)) {
            if (axis == "right") rightStick = sample else leftStick = sample
            return false
        }
        if (axis == "right") rightStick = sample else leftStick = sample
        return true
    }

    fun snapshot(): ControllerSnapshot {
        return ControllerSnapshot(
            buttons = buttons.toMap(),
            leftStick = leftStick,
            rightStick = rightStick,
        )
    }

    fun releaseAll() {
        buttons.keys.toList().forEach { buttons[it] = false }
        leftStick = StickSample(0f, 0f)
        rightStick = StickSample(0f, 0f)
    }
}
