# Hinge Pad

Turn an Android phone into a wireless handheld-style controller for a Windows PC. The PC sees a normal **Xbox 360 / XInput** gamepad.

```
Android touch  →  Wi-Fi UDP JSON  →  Python server  →  vgamepad  →  Xbox 360 pad  →  emulator
```

The phone is the control surface only. The PC does **not** stream its screen.

Version **0.1.0**.

## What you get

- Landscape 3DS-inspired layout: Circle Pad, D-pad, A/B/X/Y diamond, L/R/ZL/ZR, Start/Select/Home
- Multitouch (stick + face buttons at the same time)
- Low-latency UDP JSON on the LAN (default port **26760**)
- Watchdog reset if the phone drops off mid-hold
- `test_client.py` so you can poke the server without the phone

This is **not** Cemuhook/DSU, Bluetooth HID, or a browser pad.

## Requirements

| Side | Need |
|------|------|
| PC | Windows 10/11, Python 3.10+, [ViGEmBus](https://github.com/nefarius/ViGEmBus) (installed by `pip install vgamepad`) |
| Phone | Android 8.0+ (API 26), same Wi-Fi as the PC |
| Build | Android Studio (to compile the app) |

## PC installation

```powershell
cd pc-server
python -m venv .venv
.\.venv\Scripts\activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

`pip install vgamepad` starts the ViGEmBus driver installer. Accept UAC and finish it. Reboot if Windows does not list a new Xbox 360 controller.

Or double-click `scripts\run-server.bat`.

## Find the PC LAN IP

```powershell
ipconfig
```

Use the IPv4 address of the adapter that is on the same Wi-Fi as the phone (`192.168.x.x` or `10.x.x.x`).  
Or run `scripts\find-lan-ip.ps1`.

**Do not** type `127.0.0.1` into the phone. That address is the phone itself.

## Windows Firewall

Allow inbound **UDP 26760** (Administrator PowerShell):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\allow-firewall.ps1
```

## Run the server

```powershell
cd pc-server
.\.venv\Scripts\activate
python server.py
```

Useful flags:

| Flag | Meaning |
|------|---------|
| `--dry-run` | No ViGEm pad; log packets only |
| `--port 26760` | Bind port |
| `--host 0.0.0.0` | Bind all interfaces |
| `--deadzone 0.08` | Stick dead zone |
| `--timeout 1.5` | Silence before pad reset |
| `--log-level DEBUG` | Verbose parse errors |
| `--invert-left-y` | Flip Circle Pad Y |

Environment variables: `SERVER_HOST`, `SERVER_PORT`, `DEADZONE`, `LOG_LEVEL`, `CLIENT_TIMEOUT`.

Expected console:

```
[SERVER] Listening on 0.0.0.0:26760
[CLIENT] Connected from 192.168.1.25:xxxxx
[INPUT] A DOWN
[INPUT] A UP
[INPUT] LEFT_STICK x=0.52 y=-0.18
```

Axis lines are rate-limited so the console does not flood.

## Android installation

### Ready-made APK (sideload)

A debug APK is attached to the [GitHub release](https://github.com/SS-Actinium/phone-3ds-gamepad/releases/tag/v0.1.0) and, if you built locally, at:

`dist/HingePad-0.1.0-debug.apk`

1. Copy the APK to the phone (USB, Drive, Telegram to yourself, etc.).
2. On the phone: Settings → Security → allow **Install unknown apps** for Files / Chrome / Drive.
3. Open the APK and install **Hinge Pad**.
4. Join the **same Wi-Fi** as the PC.
5. Open the app → enter the PC LAN IP and port `26760` → **Test** then **Connect**.

This debug APK is for personal sideload, not Play Store.

### Build from Android Studio

1. Open `android-app` in Android Studio.
2. Sync Gradle (8.10.2 / AGP 8.8.2).
3. Run on a **physical phone** on the same Wi-Fi.
4. Enter **PC IP** and **UDP port** (26760).
5. **Test** (waits for `hello_ack`) then **Connect**.

To rebuild the APK from a machine that already has JDK 17 + Android SDK 35:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build-apk.ps1
```

Status means:

| LED | Meaning |
|-----|---------|
| Teal | Recent `hello_ack` / `heartbeat_ack` |
| Amber | Sending, no recent ack |
| Red | Error or disconnected |

UDP has no real connection. “Connected” = the PC answered recently.

## End-to-end check

1. Start `python server.py`.
2. Confirm `joy.cpl` shows **Xbox 360 Controller for Windows**.
3. `python test_client.py --host 127.0.0.1 tap A`
4. Install the app, enter the LAN IP, **Connect**.
5. Hold **A** on the phone → server logs `A DOWN` → Windows sees A.
6. Release **A** → `A UP`.
7. Drag the Circle Pad → left stick moves; release → stick recenters.
8. Airplane-mode the phone → within ~1.5 s the server resets the pad.

## Tests

```powershell
cd pc-server
pip install -r requirements-dev.txt
python -m pytest tests -q
python test_client.py demo
```

Android unit tests: Android Studio → `PacketEncoderTest`.

## Protocol

Canonical packets:

```json
{"type":"button","button":"A","state":1}
{"type":"axis","axis":"left","x":0.5,"y":-0.2}
{"type":"heartbeat","ts":1234567890}
```

Legacy forms `{"button":"A","state":1}` and `{"joystick":[0.5,-0.2]}` are accepted.

Default map (no silent swap): 3DS **A/B/X/Y/L/R** → Xbox **A/B/X/Y/LB/RB**, Circle Pad → left stick.

Full table: [docs/PROTOCOL.md](docs/PROTOCOL.md).

## Docs

- [Architecture](docs/ARCHITECTURE.md)
- [Protocol](docs/PROTOCOL.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [Emulator setup](docs/EMULATOR.md)

## Residual risk

Packets are unauthenticated LAN JSON. Anyone on the same L2 network can spoof inputs. Use a trusted Wi-Fi. HMAC pairing is listed under future work.

## Future work

Configurable layouts, rumble (vgamepad already exposes a callback), gyro, QR pairing, authenticated packets, binary protocol, per-emulator profiles.

## License

MIT. Nintendo, Xbox, and ViGEm names belong to their owners. This project is unofficial.
