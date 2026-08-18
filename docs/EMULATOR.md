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
| Citra / Azahar / PabloMK7 | Emulation → Configure → Controls → SDL/XInput |
| lime3ds / similar 3DS | Input → Gamepad |
| RetroArch | Settings → Input → Port 1 → XInput |
| RPCS3 / yuzu-class | Player 1 → SDL/XInput device 0 |

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
