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

Azahar does **not** poll XInput like Arkham Origins. It uses **SDL2**. Until you bind a profile, Azahar only reads the **keyboard**. A working `joy.cpl` pad is still invisible until Auto Map.

Auto Map asks you to press the **A (right / east)** face button. The **first** button it sees picks the table:

| You press first | Azahar table | Hinge Pad preset |
|-----------------|--------------|------------------|
| Phone **A** (right, red) with preset **3DS / Azahar** | Xbox positional (east = 3DS A) | **3DS / Azahar** |
| Phone **A** with preset **Xbox games** (south Xbox A) | Nintendo letter (A = A) | **Xbox games** |

Use **one** swap layer. 3DS preset + Auto Map on the **right** A is the usual 3DS setup.

### Bind steps

1. Double-click `Start-HingePad.bat`. Wait for Listening.
2. Win+R → `joy.cpl` → Xbox 360 Controller. Test A.
3. Quit Steam or disable **Steam Input** for Azahar (Steam can hide the pad from SDL).
4. **Then** open Azahar Plus.
5. Phone: **3DS / Azahar** → Connect. Keep the pad connected.
6. Azahar → **Emulation → Configure → Controls**.
7. Mapping type: **All controllers** (not GUID+port).
8. **New** profile → name `hinge-3ds` → **Clear All**.
9. **Auto Map** → OK → press phone **A** (right) and **release** it. Auto Map reads the **release**.
10. Confirm Circle Pad = left stick, C-Stick = right stick, ZL/ZR = LT/RT.
11. **OK**. Start the game with Azahar focused.

If inputs die after you restart the server: reopen Controls or restart Azahar with the server already running.

C-Stick is the smaller right pad (New 3DS camera). Enable **New 3DS** in Azahar System or C-Stick is unused. Circle Pad is the large left pad.

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
