# Audit 0.3.0 — actions taken

| Finding | Severity | Action |
|---------|----------|--------|
| Circle Pad dead on Azahar: phone 10% + server 8% + Azahar 10% stacked; Y not inverted for SDL | P0 | Send raw stick from phone; server radial dead zone + rescale; 3DS profile inverts Y |
| Arrange drag restarts every pixel (`pointerInput` keyed on x/y) | P0 | Local drag offset; persist on finger-up only |
| Play handlers steal arrange pointers | P0 | `enabled=false` on all controls while Move is on |
| Remap is a chip soup | P1 | Two-tap: phone diamond then Xbox diamond |
| `Start-HingePad.bat` `start` + `exit` leaves Ctrl down | P0 | Launch via `wscript` + `pythonw`; force Ctrl/Shift/Alt key-up |
| Stick rate 50 Hz + extra dead zone | P1 | 90 Hz; no client dead zone |

Circle Pad still needs Azahar **Auto Map** with the pad live (SDL, not raw XInput).
