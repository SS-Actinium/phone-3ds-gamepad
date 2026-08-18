"""Apply validated messages to a virtual Xbox 360 controller."""

from __future__ import annotations

import logging
from typing import Protocol, runtime_checkable

from mapping import TRIGGER_BUTTONS, InputMapper, XboxTarget
from protocol import (
    AxisMessage,
    ButtonMessage,
    Message,
    StateSyncMessage,
    TriggerMessage,
    apply_deadzone,
)

log = logging.getLogger("hingepad.controller")


@runtime_checkable
class GamepadBackend(Protocol):
    def press(self, target: XboxTarget) -> None: ...

    def release(self, target: XboxTarget) -> None: ...

    def set_axis(self, axis: str, x: float, y: float) -> None: ...

    def set_trigger(self, side: str, value: float) -> None: ...

    def reset(self) -> None: ...

    def update(self) -> None: ...


class FakeGamepad:
    """In-memory pad used by tests and --dry-run (no ViGEmBus)."""

    def __init__(self) -> None:
        self.buttons: set[XboxTarget] = set()
        self.axes: dict[str, tuple[float, float]] = {
            "left": (0.0, 0.0),
            "right": (0.0, 0.0),
        }
        self.triggers: dict[str, float] = {"left": 0.0, "right": 0.0}
        self.updates = 0
        self.resets = 0

    def press(self, target: XboxTarget) -> None:
        self.buttons.add(target)

    def release(self, target: XboxTarget) -> None:
        self.buttons.discard(target)

    def set_axis(self, axis: str, x: float, y: float) -> None:
        self.axes[axis] = (x, y)

    def set_trigger(self, side: str, value: float) -> None:
        self.triggers[side] = value

    def reset(self) -> None:
        self.buttons.clear()
        self.axes = {"left": (0.0, 0.0), "right": (0.0, 0.0)}
        self.triggers = {"left": 0.0, "right": 0.0}
        self.resets += 1

    def update(self) -> None:
        self.updates += 1


class VGamepadBackend:
    """yannbouteiller/vgamepad VX360Gamepad adapter."""

    def __init__(self) -> None:
        try:
            import vgamepad as vg  # type: ignore
        except ImportError as exc:
            raise RuntimeError(
                "vgamepad is not installed. Run: pip install -r requirements.txt"
            ) from exc

        try:
            self._vg = vg
            self._pad = vg.VX360Gamepad()
        except Exception as exc:  # ViGEmBus missing or access denied
            raise RuntimeError(
                "Could not create a virtual Xbox 360 controller. "
                "Install ViGEmBus (pip install vgamepad runs the installer) "
                "and reboot if Windows just installed the driver. "
                "Use --dry-run to test the server without a virtual pad."
            ) from exc

        self._button_enum = {
            XboxTarget.A: vg.XUSB_BUTTON.XUSB_GAMEPAD_A,
            XboxTarget.B: vg.XUSB_BUTTON.XUSB_GAMEPAD_B,
            XboxTarget.X: vg.XUSB_BUTTON.XUSB_GAMEPAD_X,
            XboxTarget.Y: vg.XUSB_BUTTON.XUSB_GAMEPAD_Y,
            XboxTarget.LEFT_SHOULDER: vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER,
            XboxTarget.RIGHT_SHOULDER: vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER,
            XboxTarget.START: vg.XUSB_BUTTON.XUSB_GAMEPAD_START,
            XboxTarget.BACK: vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK,
            XboxTarget.GUIDE: vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE,
            XboxTarget.DPAD_UP: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP,
            XboxTarget.DPAD_DOWN: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN,
            XboxTarget.DPAD_LEFT: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT,
            XboxTarget.DPAD_RIGHT: vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT,
            XboxTarget.LEFT_THUMB: vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB,
            XboxTarget.RIGHT_THUMB: vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB,
        }

    def press(self, target: XboxTarget) -> None:
        if target in TRIGGER_BUTTONS:
            self.set_trigger("left" if target is XboxTarget.LEFT_TRIGGER else "right", 1.0)
            return
        self._pad.press_button(button=self._button_enum[target])

    def release(self, target: XboxTarget) -> None:
        if target in TRIGGER_BUTTONS:
            self.set_trigger("left" if target is XboxTarget.LEFT_TRIGGER else "right", 0.0)
            return
        self._pad.release_button(button=self._button_enum[target])

    def set_axis(self, axis: str, x: float, y: float) -> None:
        if axis == "right":
            self._pad.right_joystick_float(x_value_float=x, y_value_float=y)
        else:
            self._pad.left_joystick_float(x_value_float=x, y_value_float=y)

    def set_trigger(self, side: str, value: float) -> None:
        if side == "right":
            self._pad.right_trigger_float(value_float=value)
        else:
            self._pad.left_trigger_float(value_float=value)

    def reset(self) -> None:
        self._pad.reset()

    def update(self) -> None:
        self._pad.update()


class VirtualController:
    def __init__(
        self,
        backend: GamepadBackend,
        *,
        deadzone: float = 0.08,
        invert_left_y: bool = False,
        invert_right_y: bool = False,
    ) -> None:
        self.backend = backend
        self.deadzone = deadzone
        self.invert_left_y = invert_left_y
        self.invert_right_y = invert_right_y
        self.mapper = InputMapper()
        self.user_invert = False
        self.pressed: set[XboxTarget] = set()
        self.axes: dict[str, tuple[float, float]] = {
            "left": (0.0, 0.0),
            "right": (0.0, 0.0),
        }
        self.triggers: dict[str, float] = {"left": 0.0, "right": 0.0}

    def apply(self, message: Message) -> list[str]:
        notes: list[str] = []
        if isinstance(message, ButtonMessage):
            notes.extend(self._set_button(message.button, message.state))
        elif isinstance(message, AxisMessage):
            notes.extend(self._set_axis(message.axis, message.x, message.y))
        elif isinstance(message, TriggerMessage):
            notes.extend(self._set_trigger(message.trigger, message.value))
        elif isinstance(message, StateSyncMessage):
            if message.buttons:
                for name, state in message.buttons.items():
                    notes.extend(self._set_button(name, state))
            if message.axes:
                for name, (x, y) in message.axes.items():
                    notes.extend(self._set_axis(name, x, y))
            if message.triggers:
                for name, value in message.triggers.items():
                    notes.extend(self._set_trigger(name, value))
        else:
            return notes
        self.backend.update()
        return notes

    def reset(self, reason: str = "reset") -> None:
        self.pressed.clear()
        self.axes = {"left": (0.0, 0.0), "right": (0.0, 0.0)}
        self.triggers = {"left": 0.0, "right": 0.0}
        self.backend.reset()
        self.backend.update()
        log.info("[PAD] Reset (%s)", reason)

    def _set_button(self, name: str, state: int) -> list[str]:
        target = self.mapper.resolve(name)
        if state:
            if target in self.pressed and target not in TRIGGER_BUTTONS:
                return []
            self.pressed.add(target)
            self.backend.press(target)
            return [f"{target.value} DOWN"]
        if target not in self.pressed and target not in TRIGGER_BUTTONS:
            return []
        self.pressed.discard(target)
        self.backend.release(target)
        return [f"{target.value} UP"]

    def apply_profile_defaults(self, profile: str) -> None:
        if self.user_invert:
            return
        # Azahar/SDL Circle Pad is inverted vs phone screen-up unless we flip Y.
        flip = profile == "3ds"
        self.invert_left_y = flip
        self.invert_right_y = flip

    def apply_options(self, invert_left_y: bool | None, invert_right_y: bool | None) -> None:
        if invert_left_y is None and invert_right_y is None:
            return
        self.user_invert = True
        if invert_left_y is not None:
            self.invert_left_y = invert_left_y
        if invert_right_y is not None:
            self.invert_right_y = invert_right_y

    def _set_axis(self, axis: str, x: float, y: float) -> list[str]:
        invert = self.invert_left_y if axis == "left" else self.invert_right_y
        if invert:
            y = -y
        nx, ny = apply_deadzone(x, y, self.deadzone)
        previous = self.axes.get(axis, (0.0, 0.0))
        self.axes[axis] = (nx, ny)
        self.backend.set_axis(axis, nx, ny)
        if previous == (nx, ny):
            return []
        return [f"{axis.upper()}_STICK x={nx:.2f} y={ny:.2f}"]

    def _set_trigger(self, side: str, value: float) -> list[str]:
        previous = self.triggers.get(side, 0.0)
        self.triggers[side] = value
        self.backend.set_trigger(side, value)
        target = XboxTarget.LEFT_TRIGGER if side == "left" else XboxTarget.RIGHT_TRIGGER
        if value > 0.5:
            self.pressed.add(target)
        else:
            self.pressed.discard(target)
        if abs(previous - value) < 0.02:
            return []
        return [f"{side.upper()}_TRIGGER {value:.2f}"]
