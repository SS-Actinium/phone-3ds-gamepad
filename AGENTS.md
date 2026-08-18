# Hinge Pad — product notes

## Stack

- PC: Python 3.10+, stdlib UDP, `vgamepad` / ViGEmBus, XInput virtual pad
- Phone: Kotlin, Jetpack Compose, DatagramSocket on IO dispatcher
- Protocol: UTF-8 JSON over UDP, default port 26760 (not DSU)

## Standing orders

- Do not replace UDP JSON with WebSockets, TCP, Bluetooth, USB HID, or DSU as the primary path.
- Keep 3DS letter → Xbox letter mapping centralized in `pc-server/mapping.py`. Do not swap the diamond.
- Treat every datagram as untrusted. Never crash the server on bad JSON.
- Reset the virtual pad on shutdown, disconnect, client switch, and watchdog timeout.
- Secrets: none in 0.1.0. If pairing is added, put the shared secret in env, never in git.

## Scale

One LAN session. File/process local. Do not call this multi-instance HA.
