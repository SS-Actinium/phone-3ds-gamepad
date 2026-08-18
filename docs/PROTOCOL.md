# UDP JSON protocol

Application-level protocol between the Android controller and the Windows Python server.

- Transport: **UDP**
- Encoding: **UTF-8 JSON**, one object per datagram
- Max datagram: **2048 bytes** (server drops larger packets)
- Default port: **26760**
- This is **not** Cemuhook/DSU. Port 26760 is only a convenient default.

## Connection model

UDP is connectionless. “Connected” is an application state:

1. Phone sends `hello`.
2. Server replies `hello_ack` to the source address.
3. Phone sends `heartbeat` every 250 ms and a `state_sync` every 400 ms.
4. Server replies `heartbeat_ack`.
5. If the server sees no valid packet from the active client for `CLIENT_TIMEOUT` (default 1.5 s), it **resets** the virtual controller (all buttons released, sticks centered).
6. Phone sends `disconnect` on an explicit disconnect. Server resets immediately.

A new source address that sends a valid packet becomes the active client. The previous client’s state is cleared first.

## Common fields

| Field | Type | Required | Notes |
|-------|------|----------|--------|
| `type` | string | yes (canonical) | Message kind |
| `ts` | number | no | Client monotonic or epoch ms. Ignored for control, used for logs. |
| `seq` | integer | no | Monotonic packet id. Optional. |

Legacy packets without `type` are accepted (see below) so a one-line test sender still works.

## Message types

### `hello`

Phone → PC. Starts or refreshes a session.

```json
{"type":"hello","client":"hinge-pad","version":"0.1.0","ts":1710000000000}
```

### `hello_ack`

PC → Phone.

```json
{"type":"hello_ack","server":"hinge-pad","version":"0.1.0","ok":true}
```

### `heartbeat`

Phone → PC. Keeps the watchdog from resetting the pad.

```json
{"type":"heartbeat","ts":1710000000250}
```

### `heartbeat_ack`

PC → Phone. The phone treats a recent ack as “connected”.

```json
{"type":"heartbeat_ack","ok":true}
```

### `button`

Phone → PC. Digital press/release.

```json
{"type":"button","button":"A","state":1}
```

- `state`: `1` pressed, `0` released. Booleans are also accepted.
- Unknown button names are ignored (logged at DEBUG).

### `axis`

Phone → PC. Analog stick.

```json
{"type":"axis","axis":"left","x":0.5,"y":-0.2}
```

- `axis`: `left` (Circle Pad) or `right`
- `x`, `y`: floats, ideally in `[-1, 1]`. Server clamps.
- Screen-up on the phone is sent as **positive Y**. Server applies optional invert.

### `trigger`

Phone → PC. Analog shoulder triggers (ZL / ZR).

```json
{"type":"trigger","trigger":"left","value":1.0}
```

- `trigger`: `left` or `right`
- `value`: `0.0`–`1.0` (server clamps)

Digital `ZL` / `ZR` buttons are also accepted via `button` and map to a full trigger press.

### `state_sync`

Phone → PC. Full snapshot. Recovers from a dropped release packet.

```json
{
  "type": "state_sync",
  "buttons": {"A": 1, "B": 0, "L": 1},
  "axes": {"left": [0.12, -0.40], "right": [0.0, 0.0]},
  "triggers": {"left": 0.0, "right": 0.0}
}
```

Omitted keys are left unchanged. Send every held button you care about; the server also has a watchdog reset.

### `disconnect`

Phone → PC.

```json
{"type":"disconnect"}
```

## Legacy packets (still valid)

```json
{"button":"A","state":1}
{"joystick":[0.5,-0.2]}
```

These are normalized to `button` / `axis` (`left`) on the server.

## Button names

| Name | Xbox 360 / XInput |
|------|-------------------|
| `A` | A |
| `B` | B |
| `X` | X |
| `Y` | Y |
| `L` | Left shoulder |
| `R` | Right shoulder |
| `ZL` | Left trigger (full) |
| `ZR` | Right trigger (full) |
| `START` | Start |
| `SELECT` or `BACK` | Back |
| `HOME` or `GUIDE` | Guide |
| `DUP` `DDOWN` `DLEFT` `DRIGHT` | D-pad |
| `LSTICK` | Left thumb click |
| `RSTICK` | Right thumb click |

Default 3DS-style mapping is **same letter → same Xbox letter**. There is no silent Nintendo/Xbox diamond swap.

## Axes

| Name | Xbox |
|------|------|
| `left` | Left thumbstick (Circle Pad) |
| `right` | Right thumbstick |

Dead zone, clamp, and float→XInput conversion happen **on the PC**.

## Safety

| Risk | Mitigation |
|------|------------|
| Dropped `state=0` | Watchdog reset + periodic `state_sync` |
| Malformed JSON | Drop packet, keep serving |
| Oversized datagram | Drop |
| Unexpected client gone | `CLIENT_TIMEOUT` reset |
| LAN spoofing | Residual: no auth in 0.1.0. Use a trusted Wi-Fi. Optional shared-secret HMAC is a planned follow-up. |

## Rate guidance (Android)

- Buttons: send immediately on edge only (no repeats).
- Stick: max 60 Hz, skip if both axes changed by less than `0.012` while inside the dead zone after release.
- Heartbeat: 250 ms.
- State sync: 400 ms while “connected”.
