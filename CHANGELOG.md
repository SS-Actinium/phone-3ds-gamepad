# Changelog

## 0.1.0 — 2026-08-18

### Added

- Windows Python UDP server that drives a virtual Xbox 360 controller via vgamepad
- JSON protocol: hello, heartbeat, button, axis, trigger, state_sync, disconnect, plus legacy packets
- Watchdog reset when the phone goes silent
- `--dry-run` backend for machines without ViGEmBus
- Kotlin / Jetpack Compose Android app (landscape handheld layout)
- Circle Pad, D-pad, A/B/X/Y, L/R/ZL/ZR, Start/Select/Home
- Connection screen with Test / Connect and last-used IP
- `test_client.py` and pytest coverage for parse, map, dead zone, watchdog
- Firewall helper, LAN-IP helper, emulator notes
