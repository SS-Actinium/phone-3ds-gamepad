# Changelog

## 0.3.1 — 2026-08-18

### Fixed

- Circle Pad last position no longer dropped by the 90 Hz limiter (pending flush per stick)
- ViGEm axis values clamped to [-1, 1]
- Default PC dead zone 0.04 so Azahar does not stack a second large gate

## 0.3.0 — 2026-08-18

### Fixed

- Azahar Circle Pad: invert Y on 3DS preset, radial dead-zone rescale, no stacked phone dead zone
- Move-mode drag no longer stutters (local offset, play input disabled while arranging)
- Batch file no longer leaves Ctrl stuck (VBS/`pythonw` launch + key-up)
- Remap is a two-tap phone → Xbox flow instead of tiny chips

### Added

- Invert Circle Pad Y toggle on the Map screen

## 0.2.1 — 2026-08-18

### Fixed

- Connect screen on small landscape phones: Xbox/3DS, Test, and Connect stay on screen (scroll + pinned actions)
- Keyboard no longer covers Connect (`adjustResize`)

### Changed

- PC starter is a real window: large IP, port 26760, Copy buttons, and a teal **Server started** status
- Double-click `Start-HingePad.bat` opens the app with no extra PowerShell window after first setup

## 0.2.0 — 2026-08-18

### Added

- One-click `Start-HingePad.bat` + desktop window (no PowerShell typing)
- Xbox vs 3DS / Azahar control presets
- Custom per-button remap sheet
- Drag-to-move layout on the phone (`Move` then `Done`)
- Right-stick C-Stick pad for New 3DS camera
- Compact UDP packets and a single I/O thread

### Fixed

- “Send failed” spam from using DatagramSocket on two threads
- Input delay from recomposing the whole pad on every heartbeat
- Azahar Plus Auto Map mismatch (Nintendo diamond vs Xbox diamond)
- Heavy shadows/gradients that stuttered on Helio P35 / PowerVR GE8320

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
- Sideloadable debug APK and `scripts/build-apk.ps1`

### Fixed

- Android ViewModel setter name clash that blocked `assembleDebug`
