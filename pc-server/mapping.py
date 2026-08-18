"""Central 3DS-style control names → Xbox 360 / XInput targets.

Do not swap Nintendo diamond positions to Xbox positions. A means A.
"""

from __future__ import annotations

from enum import Enum


class XboxTarget(str, Enum):
    A = "A"
    B = "B"
    X = "X"
    Y = "Y"
    LEFT_SHOULDER = "LEFT_SHOULDER"
    RIGHT_SHOULDER = "RIGHT_SHOULDER"
    START = "START"
    BACK = "BACK"
    GUIDE = "GUIDE"
    DPAD_UP = "DPAD_UP"
    DPAD_DOWN = "DPAD_DOWN"
    DPAD_LEFT = "DPAD_LEFT"
    DPAD_RIGHT = "DPAD_RIGHT"
    LEFT_THUMB = "LEFT_THUMB"
    RIGHT_THUMB = "RIGHT_THUMB"
    LEFT_TRIGGER = "LEFT_TRIGGER"
    RIGHT_TRIGGER = "RIGHT_TRIGGER"


# 3DS / app button name → Xbox target.
BUTTON_MAP: dict[str, XboxTarget] = {
    "A": XboxTarget.A,
    "B": XboxTarget.B,
    "X": XboxTarget.X,
    "Y": XboxTarget.Y,
    "L": XboxTarget.LEFT_SHOULDER,
    "R": XboxTarget.RIGHT_SHOULDER,
    "ZL": XboxTarget.LEFT_TRIGGER,
    "ZR": XboxTarget.RIGHT_TRIGGER,
    "START": XboxTarget.START,
    "SELECT": XboxTarget.BACK,
    "BACK": XboxTarget.BACK,
    "HOME": XboxTarget.GUIDE,
    "GUIDE": XboxTarget.GUIDE,
    "DUP": XboxTarget.DPAD_UP,
    "DDOWN": XboxTarget.DPAD_DOWN,
    "DLEFT": XboxTarget.DPAD_LEFT,
    "DRIGHT": XboxTarget.DPAD_RIGHT,
    "LSTICK": XboxTarget.LEFT_THUMB,
    "RSTICK": XboxTarget.RIGHT_THUMB,
}

TRIGGER_BUTTONS = frozenset({XboxTarget.LEFT_TRIGGER, XboxTarget.RIGHT_TRIGGER})

XUSB_NAME: dict[XboxTarget, str] = {
    XboxTarget.A: "XUSB_GAMEPAD_A",
    XboxTarget.B: "XUSB_GAMEPAD_B",
    XboxTarget.X: "XUSB_GAMEPAD_X",
    XboxTarget.Y: "XUSB_GAMEPAD_Y",
    XboxTarget.LEFT_SHOULDER: "XUSB_GAMEPAD_LEFT_SHOULDER",
    XboxTarget.RIGHT_SHOULDER: "XUSB_GAMEPAD_RIGHT_SHOULDER",
    XboxTarget.START: "XUSB_GAMEPAD_START",
    XboxTarget.BACK: "XUSB_GAMEPAD_BACK",
    XboxTarget.GUIDE: "XUSB_GAMEPAD_GUIDE",
    XboxTarget.DPAD_UP: "XUSB_GAMEPAD_DPAD_UP",
    XboxTarget.DPAD_DOWN: "XUSB_GAMEPAD_DPAD_DOWN",
    XboxTarget.DPAD_LEFT: "XUSB_GAMEPAD_DPAD_LEFT",
    XboxTarget.DPAD_RIGHT: "XUSB_GAMEPAD_DPAD_RIGHT",
    XboxTarget.LEFT_THUMB: "XUSB_GAMEPAD_LEFT_THUMB",
    XboxTarget.RIGHT_THUMB: "XUSB_GAMEPAD_RIGHT_THUMB",
    XboxTarget.LEFT_TRIGGER: "LEFT_TRIGGER",
    XboxTarget.RIGHT_TRIGGER: "RIGHT_TRIGGER",
}


def map_button(name: str) -> XboxTarget:
    try:
        return BUTTON_MAP[name.upper()]
    except KeyError as exc:
        raise KeyError(f"no mapping for button {name!r}") from exc
