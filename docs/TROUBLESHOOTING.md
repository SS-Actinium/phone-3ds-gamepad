# Troubleshooting

## Phone cannot reach the PC

- Phone and PC must be on the **same LAN**. Guest Wi-Fi and “AP isolation” / “client isolation” block device-to-device UDP.
- Use the PC’s **IPv4 LAN** address (`192.168.x.x` or `10.x.x.x`), not `127.0.0.1` on the phone, and not a public WAN IP.
- On the PC: `ipconfig` → Ethernet or Wi-Fi adapter → **IPv4 Address**.
- Temporarily disable a third-party firewall to see if that is the blocker.

## Firewall blocking UDP

Windows Defender inbound rule (PowerShell as Administrator):

```powershell
New-NetFirewallRule -DisplayName "Hinge Pad UDP 26760" -Direction Inbound -Protocol UDP -LocalPort 26760 -Action Allow
```

Or: Windows Security → Firewall → Advanced → Inbound Rule → Port → UDP 26760 → Allow.

## Wrong IP or port

Server default is `0.0.0.0:26760`. The app must use the same port. Re-run **Test** after changing either field.

## Python dependency errors

```powershell
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
```

Need Python 3.10+. `vgamepad` is **Windows-only**.

## vgamepad / ViGEmBus driver

`pip install vgamepad` launches the ViGEmBus installer. Accept UAC, finish, **reboot** if the pad does not appear.

Manual driver: [ViGEmBus releases](https://github.com/nefarius/ViGEmBus/releases).

To run the server without a virtual pad (logs only):

```powershell
python server.py --dry-run
```

## Virtual controller not appearing

1. Start the server (not `--dry-run`).
2. Windows Settings → Bluetooth & devices → Devices, or `joy.cpl` (Win+R).
3. You should see **Xbox 360 Controller for Windows**.
4. If not: reinstall ViGEmBus, reboot, run the terminal **not** inside a restricted sandbox.

## Stick inverted

```powershell
python server.py --invert-left-y
```

or set `INVERT_LEFT_Y=1`.

## Buttons mapped incorrectly in an emulator

This stack exposes a standard XInput pad: A is A. Emulators sometimes show Nintendo labels. Remap inside the emulator; do not expect this app to swap diamonds.

## Buttons stuck

A dropped UDP release is possible. After 1.5 s of silence the server resets. Tap **Disconnect** on the phone or stop the server (`Ctrl+C`) to force a reset.

## High latency

- Prefer 5 GHz Wi-Fi or the same router, not a mesh hop if you can avoid it.
- Close VPN clients on either device.
- Do not use a phone hotspot with client isolation.
- Axis traffic is capped at 60 Hz and skips tiny duplicates.

## Wi-Fi isolation

Many hotel / campus APs block LAN. Tether the PC to the phone hotspot (and allow local devices) or use a home router.

## Android lifecycle

If you leave the app, the socket is torn down and a `disconnect` is sent when possible. Re-open and **Connect** again. Keep the screen on (already requested) while playing.
