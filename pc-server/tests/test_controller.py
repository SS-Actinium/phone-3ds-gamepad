from __future__ import annotations

import time

from config import Config
from controller import FakeGamepad, VirtualController
from protocol import AxisMessage, ButtonMessage, StateSyncMessage, parse_packet
from server import UdpGamepadServer


def make_controller() -> tuple[VirtualController, FakeGamepad]:
    pad = FakeGamepad()
    ctl = VirtualController(pad, deadzone=0.08)
    return ctl, pad


def test_multiple_button_states() -> None:
    ctl, pad = make_controller()
    ctl.apply(ButtonMessage(button="A", state=1))
    ctl.apply(ButtonMessage(button="L", state=1))
    assert {t.value for t in pad.buttons} == {"A", "LEFT_SHOULDER"}
    ctl.apply(ButtonMessage(button="A", state=0))
    assert {t.value for t in pad.buttons} == {"LEFT_SHOULDER"}


def test_controller_reset() -> None:
    ctl, pad = make_controller()
    ctl.apply(ButtonMessage(button="B", state=1))
    ctl.apply(AxisMessage(axis="left", x=0.8, y=-0.4))
    ctl.reset("test")
    assert pad.buttons == set()
    assert pad.axes["left"] == (0.0, 0.0)
    assert pad.resets >= 1


def test_deadzone_on_controller() -> None:
    ctl, pad = make_controller()
    ctl.apply(AxisMessage(axis="left", x=0.01, y=-0.02))
    assert pad.axes["left"] == (0.0, 0.0)


def test_axis_clamped_before_backend() -> None:
    ctl, pad = make_controller()
    ctl.apply(parse_packet('{"type":"axis","axis":"left","x":5,"y":-5}'))
    assert pad.axes["left"] == (1.0, -1.0)


def test_state_sync_recovers_release() -> None:
    ctl, pad = make_controller()
    ctl.apply(ButtonMessage(button="A", state=1))
    ctl.apply(
        StateSyncMessage(buttons={"A": 0, "B": 1}, axes={"left": (0.0, 0.0)})
    )
    assert {t.value for t in pad.buttons} == {"B"}


def test_watchdog_timeout_resets_pad() -> None:
    ctl, pad = make_controller()
    cfg = Config(client_timeout=0.05, dry_run=True)
    server = UdpGamepadServer(cfg, ctl)
    server._client = ("127.0.0.1", 9999)
    server._last_seen = time.monotonic() - 1.0
    server._idle_reset_done = False
    ctl.apply(ButtonMessage(button="A", state=1))
    assert pad.buttons
    server._check_watchdog()
    assert pad.buttons == set()
    assert pad.axes["left"] == (0.0, 0.0)
    assert server._client is None
