"""Small control window so the server starts with one double-click."""

from __future__ import annotations

import socket
import subprocess
import sys
import threading
from pathlib import Path

try:
    import tkinter as tk
    from tkinter import messagebox, scrolledtext
except Exception:
    raise SystemExit(2)

ROOT = Path(__file__).resolve().parent


def lan_ips() -> list[str]:
    found: list[str] = []
    try:
        host = socket.gethostname()
        for info in socket.getaddrinfo(host, None, socket.AF_INET):
            ip = info[4][0]
            if not ip.startswith("127.") and ip not in found:
                found.append(ip)
    except OSError:
        pass
    # UDP trick: discover the interface used for LAN without sending data
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        ip = probe.getsockname()[0]
        if ip not in found and not ip.startswith("127."):
            found.insert(0, ip)
    except OSError:
        pass
    finally:
        probe.close()
    return found or ["(run ipconfig)"]


class Launcher:
    def __init__(self) -> None:
        self.proc: subprocess.Popen[str] | None = None
        self.root = tk.Tk()
        self.root.title("Hinge Pad")
        self.root.geometry("520x420")
        self.root.configure(bg="#1A1C1E")

        ips = "\n".join(lan_ips())
        tk.Label(
            self.root,
            text="HINGE PAD",
            fg="#B8954A",
            bg="#1A1C1E",
            font=("Segoe UI", 16, "bold"),
        ).pack(pady=(14, 4))
        tk.Label(
            self.root,
            text="Phone Wi-Fi IP (use one of these)\n" + ips,
            fg="#E8E6E1",
            bg="#1A1C1E",
            font=("Consolas", 11),
            justify="left",
        ).pack(pady=6)
        tk.Label(
            self.root,
            text="UDP port 26760   ·   Azahar: 3DS preset on the phone   ·   Arkham: Xbox preset",
            fg="#8B9098",
            bg="#1A1C1E",
            font=("Segoe UI", 9),
        ).pack()

        row = tk.Frame(self.root, bg="#1A1C1E")
        row.pack(pady=10)
        self.start_btn = tk.Button(row, text="Start server", width=16, command=self.start)
        self.start_btn.pack(side="left", padx=6)
        tk.Button(row, text="Stop", width=10, command=self.stop).pack(side="left", padx=6)

        self.log = scrolledtext.ScrolledText(
            self.root, height=12, bg="#121416", fg="#E8E6E1", insertbackground="#E8E6E1"
        )
        self.log.pack(fill="both", expand=True, padx=12, pady=(4, 12))
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)
        self.start()

    def write(self, line: str) -> None:
        self.log.insert("end", line)
        self.log.see("end")

    def start(self) -> None:
        if self.proc and self.proc.poll() is None:
            return
        cmd = [sys.executable, str(ROOT / "server.py")]
        self.proc = subprocess.Popen(
            cmd,
            cwd=str(ROOT),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
        )
        threading.Thread(target=self._pump, daemon=True).start()
        self.write("Starting server…\n")

    def _pump(self) -> None:
        assert self.proc and self.proc.stdout
        for line in self.proc.stdout:
            self.root.after(0, self.write, line)

    def stop(self) -> None:
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        self.write("Server stopped.\n")

    def on_close(self) -> None:
        self.stop()
        self.root.destroy()

    def run(self) -> None:
        self.root.mainloop()


if __name__ == "__main__":
    Launcher().run()
