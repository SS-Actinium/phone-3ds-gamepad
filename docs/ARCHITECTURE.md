# Architecture

```
Android Compose UI
        │
 Controller state (edges + stick sample)
        │
 Protocol encoder (UTF-8 JSON)
        │
 UDP transport (background thread / IO dispatcher)
        │
     Wi-Fi LAN
        │
 Python UDP server
        │
 Parser → validator (untrusted input)
        │
 Central 3DS → Xbox map
        │
 vgamepad.VX360Gamepad.update()
        │
 ViGEmBus → Windows XInput
        │
 Emulator / game
```

The phone is a **controller surface only**. The PC does not stream video.

## Python modules

| File | Job |
|------|-----|
| `config.py` | Host, port, dead zone, timeout, log level |
| `protocol.py` | Parse/validate JSON; clamp; dead zone helper |
| `mapping.py` | Single button dictionary |
| `controller.py` | Backend protocol + vgamepad / fake pad |
| `server.py` | Socket loop, watchdog, acks, shutdown |
| `test_client.py` | CLI sender for debugging |

## Android packages

| Package | Job |
|---------|-----|
| `ui.connect` / `ui.pad` | Landscape Compose surfaces |
| `controller` | Press map + last stick |
| `protocol` | JSON encode |
| `network` | DatagramSocket + heartbeat/sync |

## Disconnect safety

UDP can drop a release. Two independent failsafes:

1. **Watchdog (PC):** no valid packet for `CLIENT_TIMEOUT` (1.5 s) → reset pad.
2. **State sync (phone):** every 400 ms while linked, send the full button/stick snapshot.

Heartbeats (250 ms) keep the watchdog quiet without spamming axis packets.

Tradeoff: a 1.5 s stuck button is possible if Wi-Fi dies mid-hold. That is safer than a button that stays down forever, and it does not add per-packet latency.

## Scale claim

Single LAN session, one active phone at a time. This is not a multi-instance cloud service.
