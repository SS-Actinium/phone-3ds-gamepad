"""UDP listener: parse → validate → map → virtual Xbox 360 pad."""

from __future__ import annotations

import argparse
import logging
import signal
import socket
import sys
import time
from typing import TYPE_CHECKING

from config import Config
from controller import FakeGamepad, VGamepadBackend, VirtualController
from protocol import (
    DisconnectMessage,
    HeartbeatMessage,
    HelloMessage,
    ProtocolError,
    heartbeat_ack,
    hello_ack,
    parse_packet,
)

if TYPE_CHECKING:
    from controller import GamepadBackend

log = logging.getLogger("hingepad")


class UdpGamepadServer:
    def __init__(self, config: Config, controller: VirtualController) -> None:
        self.config = config
        self.controller = controller
        self._sock: socket.socket | None = None
        self._running = False
        self._client: tuple[str, int] | None = None
        self._last_seen = 0.0
        self._idle_reset_done = True
        self._last_axis_log = 0.0
        self._last_axis_note = ""

    def start(self) -> None:
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        sock.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        sock.bind((self.config.host, self.config.port))
        sock.settimeout(0.05)
        self._sock = sock
        self._running = True
        log.info("[SERVER] Listening on %s:%s", self.config.host, self.config.port)
        if self.config.dry_run:
            log.info("[SERVER] Dry-run: no virtual Xbox controller")

    def stop(self) -> None:
        self._running = False
        self.controller.reset("shutdown")
        if self._sock is not None:
            try:
                self._sock.close()
            except OSError:
                pass
            self._sock = None
        log.info("[SERVER] Stopped")

    def serve_forever(self) -> None:
        assert self._sock is not None
        while self._running:
            self._check_watchdog()
            try:
                payload, addr = self._sock.recvfrom(self.config.max_packet_bytes + 1)
            except socket.timeout:
                continue
            except OSError as exc:
                if self._running:
                    log.warning("[NET] Socket error: %s", exc)
                break
            if len(payload) > self.config.max_packet_bytes:
                log.debug("[NET] Dropped oversized packet from %s", addr)
                continue
            self._handle(payload, addr)

    def _handle(self, payload: bytes, addr: tuple[str, int]) -> None:
        try:
            message = parse_packet(payload)
        except ProtocolError as exc:
            log.debug("[NET] Bad packet from %s: %s", addr, exc)
            return

        now = time.monotonic()
        if self._client is None:
            self._client = addr
            self._idle_reset_done = False
            log.info("[CLIENT] Connected from %s:%s", addr[0], addr[1])
        elif addr != self._client:
            log.info(
                "[CLIENT] Switched %s:%s → %s:%s",
                self._client[0],
                self._client[1],
                addr[0],
                addr[1],
            )
            self.controller.reset("new client")
            self._client = addr
            self._idle_reset_done = False

        self._last_seen = now
        self._idle_reset_done = False

        if isinstance(message, HelloMessage):
            self._reply(hello_ack(), addr)
            log.info(
                "[CLIENT] Hello client=%s version=%s",
                message.client,
                message.version or "?",
            )
            return

        if isinstance(message, HeartbeatMessage):
            self._reply(heartbeat_ack(), addr)
            return

        if isinstance(message, DisconnectMessage):
            log.info("[CLIENT] Disconnect from %s:%s", addr[0], addr[1])
            self.controller.reset("disconnect")
            self._client = None
            self._idle_reset_done = True
            return

        notes = self.controller.apply(message)
        for note in notes:
            self._log_input(note)

    def _log_input(self, note: str) -> None:
        if "STICK" in note or "TRIGGER" in note:
            now = time.monotonic()
            if (
                note == self._last_axis_note
                and now - self._last_axis_log < self.config.axis_log_interval
            ):
                return
            self._last_axis_note = note
            self._last_axis_log = now
        log.info("[INPUT] %s", note)

    def _reply(self, payload: bytes, addr: tuple[str, int]) -> None:
        if self._sock is None:
            return
        try:
            self._sock.sendto(payload, addr)
        except OSError as exc:
            log.debug("[NET] Reply failed: %s", exc)

    def _check_watchdog(self) -> None:
        if self._client is None or self._idle_reset_done:
            return
        idle = time.monotonic() - self._last_seen
        if idle >= self.config.client_timeout:
            log.warning(
                "[CLIENT] Timeout after %.2fs — resetting pad",
                idle,
            )
            self.controller.reset("watchdog")
            self._client = None
            self._idle_reset_done = True


def configure_logging(level: str) -> None:
    logging.basicConfig(
        level=getattr(logging, level, logging.INFO),
        format="%(asctime)s %(message)s",
        datefmt="%H:%M:%S",
    )


def build_backend(config: Config) -> GamepadBackend:
    if config.dry_run:
        return FakeGamepad()
    return VGamepadBackend()


def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Hinge Pad — UDP JSON → virtual Xbox 360 controller",
    )
    parser.add_argument("--host", default=None, help="Bind address (default 0.0.0.0)")
    parser.add_argument("--port", type=int, default=None, help="UDP port (default 26760)")
    parser.add_argument("--deadzone", type=float, default=None, help="Stick dead zone 0-1")
    parser.add_argument(
        "--timeout",
        type=float,
        default=None,
        dest="client_timeout",
        help="Client silence before pad reset (seconds)",
    )
    parser.add_argument(
        "--log-level",
        default=None,
        choices=["DEBUG", "INFO", "WARNING", "ERROR"],
    )
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="Do not create a ViGEm virtual controller",
    )
    parser.add_argument(
        "--invert-left-y",
        action="store_true",
        help="Invert Circle Pad Y if an emulator feels upside-down",
    )
    return parser.parse_args(argv)


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    config = Config.from_env().with_overrides(
        host=args.host,
        port=args.port,
        deadzone=args.deadzone,
        log_level=args.log_level,
        client_timeout=args.client_timeout,
        dry_run=True if args.dry_run else None,
        invert_left_y=True if args.invert_left_y else None,
    )
    configure_logging(config.log_level)

    try:
        backend = build_backend(config)
    except RuntimeError as exc:
        log.error("%s", exc)
        return 1

    controller = VirtualController(
        backend,
        deadzone=config.deadzone,
        invert_left_y=config.invert_left_y,
        invert_right_y=config.invert_right_y,
    )
    server = UdpGamepadServer(config, controller)

    def _shutdown(signum: int, _frame: object) -> None:
        log.info("[SERVER] Signal %s", signum)
        server.stop()

    signal.signal(signal.SIGINT, _shutdown)
    if hasattr(signal, "SIGTERM"):
        signal.signal(signal.SIGTERM, _shutdown)

    try:
        server.start()
        server.serve_forever()
    except OSError as exc:
        log.error("[SERVER] Bind failed on %s:%s — %s", config.host, config.port, exc)
        return 1
    finally:
        if server._running:
            server.stop()
    return 0


if __name__ == "__main__":
    sys.exit(main())
