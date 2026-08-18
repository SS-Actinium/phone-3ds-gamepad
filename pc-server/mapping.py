"""3DS-style control names → Xbox 360 / XInput.

Profiles
--------
xbox  Letter match. Phone A → Xbox A. Use for Arkham Origins and most PC games.
3ds   Positional Nintendo diamond so Azahar / Citra Auto Map lines up:
      3DS A (east)  → Xbox B (east)
      3DS B (south) → Xbox A (south)
      3DS X (north) → Xbox Y (north)
      3DS Y (west)  → Xbox X (west)
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


# Letter-match (default, and the 3DS wire names themselves).
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

# Azahar / Citra Auto Map assumes an Xbox diamond.
PROFILE_3DS_FACE: dict[str, XboxTarget] = {
    "A": XboxTarget.B,
    "B": XboxTarget.A,
    "X": XboxTarget.Y,
    "Y": XboxTarget.X,
}

PROFILES = frozenset({"xbox", "3ds"})
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

_ALIAS_TO_TARGET: dict[str, XboxTarget] = {
    **BUTTON_MAP,
    "LB": XboxTarget.LEFT_SHOULDER,
    "RB": XboxTarget.RIGHT_SHOULDER,
    "LT": XboxTarget.LEFT_TRIGGER,
    "RT": XboxTarget.RIGHT_TRIGGER,
}


def normalize_profile(name: str | None) -> str:
    raw = (name or "xbox").strip().lower()
    if raw in {"n3ds", "nintendo", "azahar", "citra"}:
        return "3ds"
    if raw in PROFILES:
        return raw
    raise KeyError(f"unknown profile {name!r}")


def _lookup_target(token: str) -> XboxTarget:
    key = token.strip().upper()
    if key in XboxTarget.__members__:
        return XboxTarget[key]
    if key in _ALIAS_TO_TARGET:
        return _ALIAS_TO_TARGET[key]
    raise KeyError(f"no mapping for button {token!r}")


def map_button(
    name: str,
    profile: str = "xbox",
    remap: dict[str, str] | None = None,
) -> XboxTarget:
    key = name.strip().upper()
    if remap and key in remap:
        return _lookup_target(remap[key])
    mode = normalize_profile(profile)
    if mode == "3ds" and key in PROFILE_3DS_FACE:
        return PROFILE_3DS_FACE[key]
    return _lookup_target(key)


class InputMapper:
    """Mutable profile + per-button override used by the live server."""

    def __init__(self, profile: str = "xbox") -> None:
        self.profile = normalize_profile(profile)
        self.remap: dict[str, str] = {}

    def set_profile(self, profile: str) -> str:
        self.profile = normalize_profile(profile)
        return self.profile

    def set_remap(self, remap: dict[str, str] | None) -> None:
        cleaned: dict[str, str] = {}
        if remap:
            for src, dst in remap.items():
                src_key = str(src).strip().upper()
                dst_key = str(dst).strip().upper()
                if not src_key or not dst_key:
                    continue
                _lookup_target(src_key)
                _lookup_target(dst_key)
                cleaned[src_key] = dst_key
        self.remap = cleaned

    def resolve(self, name: str) -> XboxTarget:
        return map_button(name, self.profile, self.remap)
