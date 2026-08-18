"""Hinge Pad desktop app — IP, port, and a clear started state."""

from __future__ import annotations

import ctypes
import socket
import subprocess
import sys
import threading
import tkinter as tk
from pathlib import Path

ROOT = Path(__file__).resolve().parent
PORT = 26760

HOUSING = "#16181B"
RAISED = "#22262B"
INSET = "#101214"
GOLD = "#C4A35A"
GOLD_DIM = "#8A7340"
INK = "#F0EDE6"
MUTE = "#8B9098"
TEAL = "#2EC4B6"
RED = "#CF3E3E"
AMBER = "#E0B44A"


def release_stuck_modifiers() -> None:
    """cmd.exe start/exit can leave Ctrl down. Force key-up."""
    if sys.platform != "win32":
        return
    keyup = 0x0002
    for vk in (0x10, 0x11, 0x12, 0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0x5B, 0x5C):
        try:
            ctypes.windll.user32.keybd_event(vk, 0, keyup, 0)
        except Exception:
            pass


def lan_ips() -> list[str]:
    found: list[str] = []
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(("8.8.8.8", 80))
        ip = probe.getsockname()[0]
        if not ip.startswith("127."):
            found.append(ip)
    except OSError:
        pass
    finally:
        probe.close()
    try:
        for info in socket.getaddrinfo(socket.gethostname(), None, socket.AF_INET):
            ip = info[4][0]
            if not ip.startswith("127.") and ip not in found:
                found.append(ip)
    except OSError:
        pass
    return found


class HingePadApp:
    def __init__(self) -> None:
        self.proc: subprocess.Popen[str] | None = None
        self.ips = lan_ips()
        self.primary = self.ips[0] if self.ips else "—"

        release_stuck_modifiers()
        self.root = tk.Tk()
        self.root.title("Hinge Pad")
        self.root.geometry("560x520")
        self.root.minsize(480, 460)
        self.root.configure(bg=HOUSING)
        self.root.resizable(True, True)

        self._build()
        self.root.protocol("WM_DELETE_WINDOW", self.on_close)
        self.root.bind("<FocusIn>", lambda _e: release_stuck_modifiers())
        self.root.after(50, release_stuck_modifiers)
        self.root.after(200, self.start)

    def _build(self) -> None:
        tk.Frame(self.root, bg=GOLD, height=6).pack(fill="x")

        head = tk.Frame(self.root, bg=HOUSING)
        head.pack(fill="x", padx=22, pady=(16, 8))
        tk.Label(
            head,
            text="HINGE PAD",
            fg=GOLD,
            bg=HOUSING,
            font=("Segoe UI", 22, "bold"),
        ).pack(anchor="w")
        tk.Label(
            head,
            text="Type this on the phone. Same Wi-Fi.",
            fg=MUTE,
            bg=HOUSING,
            font=("Segoe UI", 10),
        ).pack(anchor="w", pady=(2, 0))

        ticket = tk.Frame(self.root, bg=RAISED, highlightbackground=GOLD_DIM, highlightthickness=1)
        ticket.pack(fill="x", padx=22, pady=8)

        ip_block = tk.Frame(ticket, bg=RAISED)
        ip_block.pack(fill="x", padx=18, pady=(16, 6))
        tk.Label(ip_block, text="PC IP ADDRESS", fg=GOLD, bg=RAISED, font=("Segoe UI", 9)).pack(anchor="w")
        ip_row = tk.Frame(ip_block, bg=RAISED)
        ip_row.pack(fill="x")
        self.ip_var = tk.StringVar(value=self.primary)
        tk.Label(
            ip_row,
            textvariable=self.ip_var,
            fg=INK,
            bg=RAISED,
            font=("Consolas", 28, "bold"),
        ).pack(side="left")
        tk.Button(
            ip_row,
            text="Copy IP",
            command=self.copy_ip,
            bg=INSET,
            fg=GOLD,
            activebackground=HOUSING,
            activeforeground=GOLD,
            relief="flat",
            padx=10,
            pady=4,
            cursor="hand2",
        ).pack(side="right", pady=8)

        if len(self.ips) > 1:
            others = "Also: " + "   ".join(self.ips[1:])
            tk.Label(ip_block, text=others, fg=MUTE, bg=RAISED, font=("Consolas", 9)).pack(anchor="w")

        port_block = tk.Frame(ticket, bg=RAISED)
        port_block.pack(fill="x", padx=18, pady=(4, 16))
        tk.Label(port_block, text="UDP PORT", fg=GOLD, bg=RAISED, font=("Segoe UI", 9)).pack(anchor="w")
        port_row = tk.Frame(port_block, bg=RAISED)
        port_row.pack(fill="x")
        tk.Label(
            port_row,
            text=str(PORT),
            fg=INK,
            bg=RAISED,
            font=("Consolas", 28, "bold"),
        ).pack(side="left")
        tk.Button(
            port_row,
            text="Copy port",
            command=self.copy_port,
            bg=INSET,
            fg=GOLD,
            activebackground=HOUSING,
            activeforeground=GOLD,
            relief="flat",
            padx=10,
            pady=4,
            cursor="hand2",
        ).pack(side="right")

        status = tk.Frame(self.root, bg=HOUSING)
        status.pack(fill="x", padx=22, pady=(10, 4))
        self.led = tk.Canvas(status, width=14, height=14, bg=HOUSING, highlightthickness=0)
        self.led.pack(side="left", pady=4)
        self.led_dot = self.led.create_oval(2, 2, 12, 12, fill=MUTE, outline="")
        self.status_var = tk.StringVar(value="Starting…")
        tk.Label(
            status,
            textvariable=self.status_var,
            fg=INK,
            bg=HOUSING,
            font=("Segoe UI", 13, "bold"),
        ).pack(side="left", padx=8)

        actions = tk.Frame(self.root, bg=HOUSING)
        actions.pack(fill="x", padx=22, pady=8)
        self.start_btn = tk.Button(
            actions,
            text="Start server",
            command=self.start,
            bg=GOLD,
            fg=HOUSING,
            activebackground="#D4B56A",
            relief="flat",
            font=("Segoe UI", 10, "bold"),
            padx=16,
            pady=8,
            cursor="hand2",
        )
        self.start_btn.pack(side="left")
        tk.Button(
            actions,
            text="Stop",
            command=self.stop,
            bg=RAISED,
            fg=INK,
            activebackground=INSET,
            relief="flat",
            font=("Segoe UI", 10),
            padx=16,
            pady=8,
            cursor="hand2",
        ).pack(side="left", padx=8)

        tk.Label(
            self.root,
            text="Xbox games → phone Xbox   ·   Azahar / 3DS → phone 3DS   ·   leave this window open",
            fg=MUTE,
            bg=HOUSING,
            font=("Segoe UI", 9),
        ).pack(anchor="w", padx=22)

        log_frame = tk.Frame(self.root, bg=INSET)
        log_frame.pack(fill="both", expand=True, padx=22, pady=(10, 16))
        self.log = tk.Text(
            log_frame,
            height=8,
            bg=INSET,
            fg=MUTE,
            insertbackground=INK,
            relief="flat",
            font=("Consolas", 9),
            wrap="word",
        )
        self.log.pack(fill="both", expand=True, padx=8, pady=8)
        self.log.insert("end", "Ready.\n")
        self.log.configure(state="disabled")

    def set_status(self, text: str, color: str) -> None:
        self.status_var.set(text)
        self.led.itemconfig(self.led_dot, fill=color)

    def write(self, line: str) -> None:
        self.log.configure(state="normal")
        self.log.insert("end", line)
        self.log.see("end")
        self.log.configure(state="disabled")
        low = line.lower()
        if "[server] listening" in low:
            self.set_status(f"Server started  ·  {self.primary}  :  {PORT}", TEAL)
        elif "bind failed" in low or "error" in low and "[server]" in low:
            self.set_status("Server failed to start", RED)

    def copy_ip(self) -> None:
        self.root.clipboard_clear()
        self.root.clipboard_append(self.primary)
        self.write(f"Copied IP {self.primary}\n")

    def copy_port(self) -> None:
        self.root.clipboard_clear()
        self.root.clipboard_append(str(PORT))
        self.write(f"Copied port {PORT}\n")

    def start(self) -> None:
        if self.proc and self.proc.poll() is None:
            return
        self.set_status("Starting server…", AMBER)
        cmd = [sys.executable, str(ROOT / "server.py"), "--port", str(PORT)]
        creation = 0
        if sys.platform == "win32":
            creation = getattr(subprocess, "CREATE_NO_WINDOW", 0)
        self.proc = subprocess.Popen(
            cmd,
            cwd=str(ROOT),
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            bufsize=1,
            creationflags=creation,
        )
        threading.Thread(target=self._pump, daemon=True).start()
        self.write("Starting server…\n")

    def _pump(self) -> None:
        assert self.proc and self.proc.stdout
        for line in self.proc.stdout:
            self.root.after(0, self.write, line)
        self.root.after(0, self._ended)

    def _ended(self) -> None:
        if self.status_var.get().startswith("Server started"):
            self.set_status("Server stopped", RED)
        self.write("Server process ended.\n")

    def stop(self) -> None:
        if self.proc and self.proc.poll() is None:
            self.proc.terminate()
            try:
                self.proc.wait(timeout=3)
            except subprocess.TimeoutExpired:
                self.proc.kill()
        self.set_status("Server stopped", RED)
        self.write("Server stopped.\n")

    def on_close(self) -> None:
        self.stop()
        self.root.destroy()

    def run(self) -> None:
        self.root.mainloop()


if __name__ == "__main__":
    HingePadApp().run()
