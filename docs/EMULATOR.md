# Emulator setup

The server’s job is to present a **standard Xbox 360 / XInput** device. Button names inside a given emulator can differ. Remap there if a 3DS game expects Nintendo positions.

## Prove Windows sees the pad first

1. Start `python server.py` (not `--dry-run`).
2. Win+R → `joy.cpl` → **Xbox 360 Controller for Windows**.
3. Properties → Test. From the repo:

   ```powershell
   python test_client.py tap A
   python test_client.py stick 0.8 0
   python test_client.py center
   ```

4. Only then open the emulator.

## Typical emulators

| Emulator | Where to bind |
|----------|----------------|
| Dolphin | Controllers → Standard Controller → XInput/0 |
| Azahar Plus / Azahar / Citra | Emulation → Configure → Controls. Start Hinge Pad **before** Azahar. On the phone choose **3DS / Azahar**. Then Auto Map, or bind A on the phone to 3DS A. |
| lime3ds / similar 3DS | Input → Gamepad |
| RetroArch | Settings → Input → Port 1 → XInput |
| RPCS3 / yuzu-class | Player 1 → SDL/XInput device 0 |
| Batman Arkham Origins | Use the **Xbox** preset. Steam/XInput picks up the virtual 360 pad. |

## Azahar Plus (why it failed before)

Azahar Auto Map treats the virtual pad as an **Xbox** diamond (A south, B east). Hinge Pad labels follow a **3DS** diamond (A east, B south). Letter-match made in-game A/B feel swapped or dead.

1. Double-click `Start-HingePad.bat` and wait until the log says Listening.
2. Confirm `joy.cpl` shows Xbox 360 Controller.
3. **Then** start Azahar Plus.
4. On the phone: **3DS / Azahar** → Connect.
5. Azahar → Emulation → Configure → Controls → **Auto Map** (or click 3DS A and press A on the phone).
6. Save a profile named `hinge-3ds`.
7. If the pad is missing: close Azahar, leave the server running, open Azahar again.

C-Stick is the smaller right pad (New 3DS camera). Circle Pad is the large left pad.

## Test path

```
Android button
    → UDP JSON
    → Python server
    → vgamepad
    → Windows XInput
    → Emulator
```

If `joy.cpl` moves and the emulator does not, the issue is the emulator’s device picker, not this project.
