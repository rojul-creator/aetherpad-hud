# AetherPad HUD // Android Native Wireless Gamepad Relay

Ultra low-latency (<3ms) wireless gamepad relay built in Kotlin & Jetpack Compose with Material 3.

## Features
- **Foreground Service**: Keeps continuous 120Hz UDP transmission alive when phone is locked or screen off.
- **PARTIAL_WAKE_LOCK**: CPU remains awake while screen turns off to save battery.
- **WIFI_MODE_FULL_LOW_LATENCY / FULL_HIGH_PERF**: Prevents Wi-Fi throttling.
- **Adaptive Rendering**: Completely suspends Compose Canvas visualizer during SCREEN_OFF broadcasts to minimize GPU/battery consumption to <18mA.
- **Compact 20-Byte Binary Frame**: Ultra-fast UDP serialization with zero garbage collection overhead.
- **Free Tier Limitation**: Locked to single gamepad instance.

## Quick Start
1. Open the project in **Android Studio Ladybug (2024.2+)** or newer.
2. Connect your Android phone via USB and enable USB Debugging.
3. Build & Run the app.
4. On your PC, run the receiver script in `server/pc_receiver.py`:
   ```bash
   pip install vgamepad
   python pc_receiver.py
   ```
5. In the Android App HUD, enter your PC's Wi-Fi IP (e.g., 192.168.1.3:47800) and press **START RELAY**.
