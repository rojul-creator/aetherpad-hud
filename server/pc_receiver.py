#!/usr/bin/env python3
"""
AetherPad PC UDP Controller Receiver & Virtual Gamepad Driver
Receives 20-byte binary UDP datagrams from the Android HUD and feeds them
directly to Windows/Linux as a native Xbox 360 controller via ViGEmBus.

Requirements:
    pip install vgamepad
"""

import socket
import struct
import sys

# Try importing vgamepad
try:
    import vgamepad as vg
    gamepad = vg.VX360Gamepad()
    HAS_VGAMEPAD = True
    print("[✓] Virtual Xbox 360 controller initialized successfully.")
except ImportError:
    HAS_VGAMEPAD = False
    print("[!] 'vgamepad' not installed. Running in telemetry print mode.")
    print("    To emulate real Xbox controller: pip install vgamepad")

UDP_IP = "0.0.0.0"
UDP_PORT = 47800

sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
sock.setsockopt(socket.SOL_SOCKET, socket.SO_RCVBUF, 65536)
sock.bind((UDP_IP, UDP_PORT))

print(f"==================================================")
print(f"  AETHERPAD HUD // PC CONTROLLER RECEIVER DAEMON")
print(f"  Listening on UDP 0.0.0.0:{UDP_PORT}")
print(f"==================================================")

packet_count = 0

try:
    while True:
        data, addr = sock.recvfrom(64)
        if len(data) < 20:
            continue

        # Unpack 20-byte binary frame
        # Magic(2B), Seq(1B), Bitmask(2B), LT(1B), RT(1B), LX(2B), LY(2B), RX(2B), RY(2B), Battery(1B), Ts(4B)
        magic, seq, buttons, lt, rt, lx, ly, rx, ry, battery, ts = struct.unpack('<HBHBBhhhhBI', data[:20])

        if magic != 0x50AE:
            continue

        packet_count += 1

        if HAS_VGAMEPAD:
            # Map Buttons: A, B, X, Y, LB, RB, Back, Start, Guide, L3, R3, Up, Down, Left, Right
            if buttons & (1 << 0): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_A)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_A)

            if buttons & (1 << 1): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_B)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_B)

            if buttons & (1 << 2): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_X)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_X)

            if buttons & (1 << 3): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_Y)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_Y)

            if buttons & (1 << 4): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_SHOULDER)

            if buttons & (1 << 5): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_SHOULDER)

            if buttons & (1 << 6): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_BACK)

            if buttons & (1 << 7): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_START)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_START)

            if buttons & (1 << 8): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_GUIDE)

            if buttons & (1 << 9): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_LEFT_THUMB)

            if buttons & (1 << 10): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_RIGHT_THUMB)

            if buttons & (1 << 11): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_UP)

            if buttons & (1 << 12): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_DOWN)

            if buttons & (1 << 13): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_LEFT)

            if buttons & (1 << 14): gamepad.press_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT)
            else: gamepad.release_button(button=vg.XUSB_BUTTON.XUSB_GAMEPAD_DPAD_RIGHT)

            # Triggers (0..255)
            gamepad.left_trigger(value=lt)
            gamepad.right_trigger(value=rt)

            # Sticks (-32768..32767) -> Invert Y axis for standard Windows direct input
            gamepad.left_joystick(x_value=lx, y_value=-ly)
            gamepad.right_joystick(x_value=rx, y_value=-ry)

            gamepad.update()

        if packet_count % 120 == 0:
            print(f"[STREAM] Pkts: {packet_count} | Seq: {seq:03d} | LT: {lt:03d} RT: {rt:03d} | LX: {lx:05d} LY: {ly:05d} | Batt: {battery}%")

except KeyboardInterrupt:
    print("\n[!] Receiver stopped.")
    sock.close()
    sys.exit(0)
