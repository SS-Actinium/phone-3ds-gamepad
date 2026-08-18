from __future__ import annotations

import json

import pytest

from protocol import (
    AxisMessage,
    ButtonMessage,
    HelloMessage,
    ProtocolError,
    apply_deadzone,
    clamp,
    parse_packet,
)


def test_valid_button_packet() -> None:
    msg = parse_packet('{"type":"button","button":"A","state":1}')
    assert isinstance(msg, ButtonMessage)
    assert msg.button == "A"
    assert msg.state == 1


def test_button_release() -> None:
    msg = parse_packet('{"type":"button","button":"A","state":0}')
    assert isinstance(msg, ButtonMessage)
    assert msg.state == 0


def test_legacy_button_packet() -> None:
    msg = parse_packet('{"button":"A","state":1}')
    assert isinstance(msg, ButtonMessage)
    assert msg.button == "A"
    assert msg.state == 1


def test_legacy_joystick_packet() -> None:
    msg = parse_packet('{"joystick":[0.5,-0.2]}')
    assert isinstance(msg, AxisMessage)
    assert msg.axis == "left"
    assert msg.x == pytest.approx(0.5)
    assert msg.y == pytest.approx(-0.2)


def test_invalid_json() -> None:
    with pytest.raises(ProtocolError):
        parse_packet("{not-json")


def test_unknown_button() -> None:
    with pytest.raises(ProtocolError):
        parse_packet('{"type":"button","button":"Z","state":1}')


def test_invalid_button_state() -> None:
    with pytest.raises(ProtocolError):
        parse_packet('{"type":"button","button":"A","state":2}')


def test_valid_axis_packet() -> None:
    msg = parse_packet('{"type":"axis","axis":"left","x":0.5,"y":-0.25}')
    assert isinstance(msg, AxisMessage)
    assert msg.x == pytest.approx(0.5)
    assert msg.y == pytest.approx(-0.25)


def test_axis_clamping() -> None:
    msg = parse_packet('{"type":"axis","axis":"left","x":4,"y":-9}')
    assert msg.x == 1.0
    assert msg.y == -1.0


def test_deadzone_centers_small_values() -> None:
    x, y = apply_deadzone(0.02, -0.03, 0.08)
    assert (x, y) == (0.0, 0.0)


def test_deadzone_keeps_real_deflection() -> None:
    x, y = apply_deadzone(0.5, 0.0, 0.08)
    assert x == pytest.approx(0.5)
    assert y == 0.0


def test_clamp() -> None:
    assert clamp(2, -1, 1) == 1
    assert clamp(-3, -1, 1) == -1
    assert clamp(0.2, -1, 1) == 0.2


def test_hello_packet() -> None:
    msg = parse_packet('{"type":"hello","client":"hinge-pad","version":"0.1.0"}')
    assert isinstance(msg, HelloMessage)
    assert msg.client == "hinge-pad"


def test_non_object_json() -> None:
    with pytest.raises(ProtocolError):
        parse_packet("[1,2,3]")


def test_oversized_packet() -> None:
    blob = json.dumps({"type": "heartbeat", "pad": "x" * 3000})
    with pytest.raises(ProtocolError):
        parse_packet(blob)


def test_boolean_state() -> None:
    msg = parse_packet('{"type":"button","button":"L","state":true}')
    assert msg.state == 1


def test_nan_rejected() -> None:
    with pytest.raises(ProtocolError):
        parse_packet('{"type":"axis","axis":"left","x":NaN,"y":0}')
