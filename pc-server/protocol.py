"""Parse and validate untrusted UDP JSON into typed messages."""

from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Any, Mapping

PROTOCOL_VERSION = "0.1.0"
MAX_JSON_CHARS = 2048

BUTTON_NAMES = frozenset(
    {
        "A",
        "B",
        "X",
        "Y",
        "L",
        "R",
        "ZL",
        "ZR",
        "START",
        "SELECT",
        "BACK",
        "HOME",
        "GUIDE",
        "DUP",
        "DDOWN",
        "DLEFT",
        "DRIGHT",
        "LSTICK",
        "RSTICK",
    }
)
AXIS_NAMES = frozenset({"left", "right"})
TRIGGER_NAMES = frozenset({"left", "right"})


class ProtocolError(ValueError):
    """Packet is not a usable controller message."""


@dataclass(frozen=True)
class HelloMessage:
    kind: str = "hello"
    client: str = "hinge-pad"
    version: str = ""
    ts: float | None = None


@dataclass(frozen=True)
class HeartbeatMessage:
    kind: str = "heartbeat"
    ts: float | None = None


@dataclass(frozen=True)
class DisconnectMessage:
    kind: str = "disconnect"


@dataclass(frozen=True)
class ButtonMessage:
    kind: str = "button"
    button: str = "A"
    state: int = 0


@dataclass(frozen=True)
class AxisMessage:
    kind: str = "axis"
    axis: str = "left"
    x: float = 0.0
    y: float = 0.0


@dataclass(frozen=True)
class TriggerMessage:
    kind: str = "trigger"
    trigger: str = "left"
    value: float = 0.0


@dataclass(frozen=True)
class StateSyncMessage:
    kind: str = "state_sync"
    buttons: dict[str, int] | None = None
    axes: dict[str, tuple[float, float]] | None = None
    triggers: dict[str, float] | None = None


Message = (
    HelloMessage
    | HeartbeatMessage
    | DisconnectMessage
    | ButtonMessage
    | AxisMessage
    | TriggerMessage
    | StateSyncMessage
)


def clamp(value: float, lo: float, hi: float) -> float:
    return lo if value < lo else hi if value > hi else value


def apply_deadzone(x: float, y: float, deadzone: float) -> tuple[float, float]:
    if deadzone <= 0:
        return (clamp(x, -1.0, 1.0), clamp(y, -1.0, 1.0))
    cx = clamp(x, -1.0, 1.0)
    cy = clamp(y, -1.0, 1.0)
    magnitude = (cx * cx + cy * cy) ** 0.5
    if magnitude < deadzone:
        return (0.0, 0.0)
    return (cx, cy)


def _as_mapping(raw: str | bytes | Mapping[str, Any]) -> dict[str, Any]:
    if isinstance(raw, Mapping):
        return dict(raw)
    if isinstance(raw, bytes):
        if len(raw) > MAX_JSON_CHARS:
            raise ProtocolError("packet too large")
        try:
            text = raw.decode("utf-8")
        except UnicodeDecodeError as exc:
            raise ProtocolError("not utf-8") from exc
    else:
        text = raw
    if len(text) > MAX_JSON_CHARS:
        raise ProtocolError("packet too large")
    try:
        parsed = json.loads(text)
    except json.JSONDecodeError as exc:
        raise ProtocolError("invalid json") from exc
    if not isinstance(parsed, dict):
        raise ProtocolError("json root must be an object")
    return parsed


def _normalize_legacy(data: dict[str, Any]) -> dict[str, Any]:
    if "type" in data:
        return data
    if "button" in data and "state" in data:
        out = dict(data)
        out["type"] = "button"
        return out
    if "joystick" in data:
        stick = data["joystick"]
        if not isinstance(stick, (list, tuple)) or len(stick) != 2:
            raise ProtocolError("joystick must be [x, y]")
        return {
            "type": "axis",
            "axis": "left",
            "x": stick[0],
            "y": stick[1],
            "ts": data.get("ts"),
        }
    raise ProtocolError("missing type")


def _parse_state(value: Any) -> int:
    if value is True or value == 1 or value == "1":
        return 1
    if value is False or value == 0 or value == "0":
        return 0
    raise ProtocolError("state must be 0 or 1")


def _parse_float(value: Any, field: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise ProtocolError(f"{field} must be a number")
    if value != value:  # NaN
        raise ProtocolError(f"{field} is not a finite number")
    as_float = float(value)
    if as_float in (float("inf"), float("-inf")):
        raise ProtocolError(f"{field} is not a finite number")
    return as_float


def _parse_button_name(value: Any) -> str:
    if not isinstance(value, str) or not value:
        raise ProtocolError("button name required")
    name = value.strip().upper()
    if name not in BUTTON_NAMES:
        raise ProtocolError(f"unknown button: {value}")
    return name


def parse_packet(raw: str | bytes | Mapping[str, Any]) -> Message:
    data = _normalize_legacy(_as_mapping(raw))
    kind = data.get("type")
    if not isinstance(kind, str) or not kind:
        raise ProtocolError("type required")
    kind = kind.strip().lower()
    ts = data.get("ts")
    ts_val = None if ts is None else _parse_float(ts, "ts")

    if kind == "hello":
        client = data.get("client", "hinge-pad")
        version = data.get("version", "")
        if not isinstance(client, str) or not isinstance(version, str):
            raise ProtocolError("hello fields invalid")
        return HelloMessage(client=client[:64], version=version[:32], ts=ts_val)

    if kind == "heartbeat":
        return HeartbeatMessage(ts=ts_val)

    if kind == "disconnect":
        return DisconnectMessage()

    if kind == "button":
        return ButtonMessage(
            button=_parse_button_name(data.get("button")),
            state=_parse_state(data.get("state")),
        )

    if kind == "axis":
        axis = data.get("axis", "left")
        if not isinstance(axis, str) or axis.lower() not in AXIS_NAMES:
            raise ProtocolError("unknown axis")
        return AxisMessage(
            axis=axis.lower(),
            x=clamp(_parse_float(data.get("x"), "x"), -1.0, 1.0),
            y=clamp(_parse_float(data.get("y"), "y"), -1.0, 1.0),
        )

    if kind == "trigger":
        trigger = data.get("trigger", "left")
        if not isinstance(trigger, str) or trigger.lower() not in TRIGGER_NAMES:
            raise ProtocolError("unknown trigger")
        return TriggerMessage(
            trigger=trigger.lower(),
            value=clamp(_parse_float(data.get("value"), "value"), 0.0, 1.0),
        )

    if kind == "state_sync":
        buttons = data.get("buttons")
        parsed_buttons: dict[str, int] | None = None
        if buttons is not None:
            if not isinstance(buttons, dict):
                raise ProtocolError("buttons must be an object")
            parsed_buttons = {}
            for key, value in buttons.items():
                parsed_buttons[_parse_button_name(key)] = _parse_state(value)

        axes = data.get("axes")
        parsed_axes: dict[str, tuple[float, float]] | None = None
        if axes is not None:
            if not isinstance(axes, dict):
                raise ProtocolError("axes must be an object")
            parsed_axes = {}
            for key, value in axes.items():
                name = str(key).lower()
                if name not in AXIS_NAMES:
                    raise ProtocolError("unknown axis")
                if not isinstance(value, (list, tuple)) or len(value) != 2:
                    raise ProtocolError("axis value must be [x, y]")
                parsed_axes[name] = (
                    clamp(_parse_float(value[0], "x"), -1.0, 1.0),
                    clamp(_parse_float(value[1], "y"), -1.0, 1.0),
                )

        triggers = data.get("triggers")
        parsed_triggers: dict[str, float] | None = None
        if triggers is not None:
            if not isinstance(triggers, dict):
                raise ProtocolError("triggers must be an object")
            parsed_triggers = {}
            for key, value in triggers.items():
                name = str(key).lower()
                if name not in TRIGGER_NAMES:
                    raise ProtocolError("unknown trigger")
                parsed_triggers[name] = clamp(_parse_float(value, "value"), 0.0, 1.0)

        return StateSyncMessage(
            buttons=parsed_buttons,
            axes=parsed_axes,
            triggers=parsed_triggers,
        )

    if kind in {"hello_ack", "heartbeat_ack"}:
        raise ProtocolError("ack messages are server-to-client only")

    raise ProtocolError(f"unknown type: {kind}")


def encode(payload: Mapping[str, Any]) -> bytes:
    return json.dumps(payload, separators=(",", ":"), ensure_ascii=True).encode("utf-8")


def hello_ack() -> bytes:
    return encode(
        {
            "type": "hello_ack",
            "server": "hinge-pad",
            "version": PROTOCOL_VERSION,
            "ok": True,
        }
    )


def heartbeat_ack() -> bytes:
    return encode({"type": "heartbeat_ack", "ok": True})
