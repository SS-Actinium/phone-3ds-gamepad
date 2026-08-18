"""Send example UDP packets without the Android app.

Examples:
  python test_client.py --host 127.0.0.1 tap A
  python test_client.py --host 192.168.1.10 hold A
  python test_client.py --host 127.0.0.1 stick 0.5 -0.25
  python test_client.py --host 127.0.0.1 hello
  python test_client.py --host 127.0.0.1 raw "{\"button\":\"A\",\"state\":1}"
"""

from __future__ import annotations

import argparse
import json
import socket
import sys
import time


def send(
    host: str,
    port: int,
    payload: dict | str,
    wait_ack: bool = False,
    sock: socket.socket | None = None,
) -> socket.socket:
    data = payload if isinstance(payload, str) else json.dumps(payload, separators=(",", ":"))
    own = sock is None
    if sock is None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.settimeout(0.8)
    print(f"→ {host}:{port}  {data}")
    sock.sendto(data.encode("utf-8"), (host, port))
    if wait_ack:
        try:
            reply, addr = sock.recvfrom(2048)
            print(f"← {addr[0]}:{addr[1]}  {reply.decode('utf-8', errors='replace')}")
        except socket.timeout:
            print("← (no ack)")
    if own:
        sock.close()
    return sock


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Hinge Pad UDP test sender")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=26760)
    sub = parser.add_subparsers(dest="cmd", required=True)

    hello = sub.add_parser("hello")
    hello.add_argument("--version", default="0.1.0")

    sub.add_parser("heartbeat")
    sub.add_parser("disconnect")

    tap = sub.add_parser("tap")
    tap.add_argument("button")
    tap.add_argument("--hold", type=float, default=0.2)

    hold = sub.add_parser("hold")
    hold.add_argument("button")

    release = sub.add_parser("release")
    release.add_argument("button")

    stick = sub.add_parser("stick")
    stick.add_argument("x", type=float)
    stick.add_argument("y", type=float)
    stick.add_argument("--axis", default="left")

    center = sub.add_parser("center")
    center.add_argument("--axis", default="left")

    raw = sub.add_parser("raw")
    raw.add_argument("json_text")

    demo = sub.add_parser("demo", help="A press, stick move, then reset")

    args = parser.parse_args(argv)
    host, port = args.host, args.port
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    sock.settimeout(0.8)
    try:
        if args.cmd == "hello":
            send(host, port, {"type": "hello", "client": "test_client", "version": args.version}, True, sock)
        elif args.cmd == "heartbeat":
            send(host, port, {"type": "heartbeat", "ts": time.time() * 1000}, True, sock)
        elif args.cmd == "disconnect":
            send(host, port, {"type": "disconnect"}, sock=sock)
        elif args.cmd == "tap":
            send(host, port, {"type": "button", "button": args.button.upper(), "state": 1}, sock=sock)
            time.sleep(args.hold)
            send(host, port, {"type": "button", "button": args.button.upper(), "state": 0}, sock=sock)
        elif args.cmd == "hold":
            send(host, port, {"type": "button", "button": args.button.upper(), "state": 1}, sock=sock)
        elif args.cmd == "release":
            send(host, port, {"type": "button", "button": args.button.upper(), "state": 0}, sock=sock)
        elif args.cmd == "stick":
            send(host, port, {"type": "axis", "axis": args.axis, "x": args.x, "y": args.y}, sock=sock)
        elif args.cmd == "center":
            send(host, port, {"type": "axis", "axis": args.axis, "x": 0.0, "y": 0.0}, sock=sock)
        elif args.cmd == "raw":
            send(host, port, args.json_text, sock=sock)
        elif args.cmd == "demo":
            send(host, port, {"type": "hello", "client": "test_client", "version": "0.1.0"}, True, sock)
            send(host, port, {"type": "button", "button": "A", "state": 1}, sock=sock)
            time.sleep(0.25)
            send(host, port, {"type": "button", "button": "A", "state": 0}, sock=sock)
            send(host, port, {"type": "axis", "axis": "left", "x": 0.5, "y": -0.25}, sock=sock)
            time.sleep(0.3)
            send(host, port, {"type": "axis", "axis": "left", "x": 0.0, "y": 0.0}, sock=sock)
            send(host, port, {"type": "disconnect"}, sock=sock)
    finally:
        sock.close()
    return 0


if __name__ == "__main__":
    sys.exit(main())
