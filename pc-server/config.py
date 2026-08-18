"""Runtime configuration. Env vars override defaults; CLI overrides env."""

from __future__ import annotations

import os
from dataclasses import dataclass


def _env(name: str, default: str) -> str:
    value = os.environ.get(name)
    return default if value is None or value == "" else value


def _env_float(name: str, default: float) -> float:
    raw = os.environ.get(name)
    if raw is None or raw == "":
        return default
    return float(raw)


def _env_bool(name: str, default: bool) -> bool:
    raw = os.environ.get(name)
    if raw is None or raw == "":
        return default
    return raw.strip().lower() in {"1", "true", "yes", "on"}


@dataclass(frozen=True)
class Config:
    host: str = "0.0.0.0"
    port: int = 26760
    deadzone: float = 0.08
    log_level: str = "INFO"
    client_timeout: float = 1.5
    max_packet_bytes: int = 2048
    invert_left_y: bool = False
    invert_right_y: bool = False
    dry_run: bool = False
    axis_log_min_delta: float = 0.08
    axis_log_interval: float = 0.35

    @classmethod
    def from_env(cls) -> "Config":
        return cls(
            host=_env("SERVER_HOST", "0.0.0.0"),
            port=int(_env("SERVER_PORT", "26760")),
            deadzone=_env_float("DEADZONE", 0.08),
            log_level=_env("LOG_LEVEL", "INFO").upper(),
            client_timeout=_env_float("CLIENT_TIMEOUT", 1.5),
            max_packet_bytes=int(_env("MAX_PACKET_BYTES", "2048")),
            invert_left_y=_env_bool("INVERT_LEFT_Y", False),
            invert_right_y=_env_bool("INVERT_RIGHT_Y", False),
            dry_run=_env_bool("DRY_RUN", False),
            axis_log_min_delta=_env_float("AXIS_LOG_MIN_DELTA", 0.08),
            axis_log_interval=_env_float("AXIS_LOG_INTERVAL", 0.35),
        )

    def with_overrides(
        self,
        *,
        host: str | None = None,
        port: int | None = None,
        deadzone: float | None = None,
        log_level: str | None = None,
        client_timeout: float | None = None,
        dry_run: bool | None = None,
        invert_left_y: bool | None = None,
    ) -> "Config":
        return Config(
            host=self.host if host is None else host,
            port=self.port if port is None else port,
            deadzone=self.deadzone if deadzone is None else deadzone,
            log_level=self.log_level if log_level is None else log_level.upper(),
            client_timeout=self.client_timeout if client_timeout is None else client_timeout,
            max_packet_bytes=self.max_packet_bytes,
            invert_left_y=self.invert_left_y if invert_left_y is None else invert_left_y,
            invert_right_y=self.invert_right_y,
            dry_run=self.dry_run if dry_run is None else dry_run,
            axis_log_min_delta=self.axis_log_min_delta,
            axis_log_interval=self.axis_log_interval,
        )


# Names expected by the brief (import-friendly defaults).
SERVER_HOST = "0.0.0.0"
SERVER_PORT = 26760
DEADZONE = 0.08
LOG_LEVEL = "INFO"
CLIENT_TIMEOUT = 1.5
