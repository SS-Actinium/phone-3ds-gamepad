from __future__ import annotations

import pytest

from mapping import BUTTON_MAP, InputMapper, XboxTarget, map_button


def test_default_3ds_letters_are_not_swapped() -> None:
    assert map_button("A") is XboxTarget.A
    assert map_button("B") is XboxTarget.B
    assert map_button("X") is XboxTarget.X
    assert map_button("Y") is XboxTarget.Y


def test_shoulders() -> None:
    assert map_button("L") is XboxTarget.LEFT_SHOULDER
    assert map_button("R") is XboxTarget.RIGHT_SHOULDER
    assert map_button("ZL") is XboxTarget.LEFT_TRIGGER
    assert map_button("ZR") is XboxTarget.RIGHT_TRIGGER


def test_start_select_home() -> None:
    assert map_button("START") is XboxTarget.START
    assert map_button("SELECT") is XboxTarget.BACK
    assert map_button("BACK") is XboxTarget.BACK
    assert map_button("HOME") is XboxTarget.GUIDE


def test_dpad() -> None:
    assert map_button("DUP") is XboxTarget.DPAD_UP
    assert map_button("DDOWN") is XboxTarget.DPAD_DOWN
    assert map_button("DLEFT") is XboxTarget.DPAD_LEFT
    assert map_button("DRIGHT") is XboxTarget.DPAD_RIGHT


def test_unknown_mapping() -> None:
    with pytest.raises(KeyError):
        map_button("NOPE")


def test_map_is_centralized() -> None:
    # Guard against someone adding a one-off mapping elsewhere.
    assert set(BUTTON_MAP) >= {"A", "B", "X", "Y", "L", "R"}


def test_3ds_profile_swaps_face_diamond() -> None:
    assert map_button("A", "3ds") is XboxTarget.B
    assert map_button("B", "3ds") is XboxTarget.A
    assert map_button("X", "3ds") is XboxTarget.Y
    assert map_button("Y", "3ds") is XboxTarget.X
    assert map_button("L", "3ds") is XboxTarget.LEFT_SHOULDER


def test_xbox_profile_keeps_letters() -> None:
    assert map_button("A", "xbox") is XboxTarget.A
    assert map_button("B", "xbox") is XboxTarget.B


def test_custom_remap_overrides_profile() -> None:
    assert map_button("A", "xbox", {"A": "X"}) is XboxTarget.X
    mapper = InputMapper("3ds")
    mapper.set_remap({"A": "START"})
    assert mapper.resolve("A") is XboxTarget.START
    assert mapper.resolve("B") is XboxTarget.A
