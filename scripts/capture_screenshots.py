#!/usr/bin/env python3
"""
Play Store Screenshot Capture Script for WC-Info Android
=========================================================

Automates capturing localized screenshots for Google Play Store across devices:
- Medium Phone (Portrait) -> screenshots/phone_portrait_{de,en}/
- Pixel Tablet (Landscape) -> screenshots/tablet_landscape_{de,en}/

Scenes captured:
1. 01_home.png      - Home search screen
2. 02_results.png   - Search results for "Berlin" (split view on tablet)
3. 03_compass.png   - Compass live navigation
4. 04_detail.png    - Toilet detail view
5. 05_widget.png    - Android home screen with pinned urgent toilet widget

Usage:
    python3 scripts/capture_screenshots.py                  # Capture everything (phone + tablet, DE + EN)
    python3 scripts/capture_screenshots.py --phone-only     # Capture phone only
    python3 scripts/capture_screenshots.py --tablet-only    # Capture tablet only
    python3 scripts/capture_screenshots.py --locale de-DE   # Capture German only
    python3 scripts/capture_screenshots.py --locale en-US   # Capture English only
"""

import argparse
import os
import shutil
import subprocess
import sys
import time
from pathlib import Path

PACKAGE_NAME = "de.wcinfo.app"
DEFAULT_TOILET_ID = "13095"
BERLIN_LAT = 52.520005
BERLIN_LON = 13.4049533
NAV_START_LAT = 52.510000
NAV_START_LON = 13.400000


def find_adb() -> str:
    """Find adb binary from PATH or standard Android SDK locations."""
    adb_path = shutil.which("adb")
    if adb_path:
        return adb_path

    candidates = [
        Path.home() / "Library/Android/sdk/platform-tools/adb",
        Path.home() / "Android/Sdk/platform-tools/adb",
        Path(os.environ.get("ANDROID_HOME", "")) / "platform-tools/adb",
        Path(os.environ.get("ANDROID_SDK_ROOT", "")) / "platform-tools/adb",
    ]
    for c in candidates:
        if c.is_file() and os.access(c, os.X_OK):
            return str(c)

    print("Error: 'adb' executable not found. Please ensure Android SDK platform-tools are in PATH.")
    sys.exit(1)


class DeviceController:
    def __init__(self, adb: str, serial: str, is_tablet: bool = False):
        self.adb = adb
        self.serial = serial
        self.is_tablet = is_tablet

    def run_cmd(self, cmd: str, check: bool = False) -> subprocess.CompletedProcess:
        full_cmd = f"{self.adb} -s {self.serial} {cmd}"
        res = subprocess.run(full_cmd, shell=True, capture_output=True, text=True)
        if check and res.returncode != 0:
            print(f"Command failed [{res.returncode}]: {full_cmd}\nStderr: {res.stderr}")
        return res

    def wake_device(self):
        self.run_cmd("shell input keyevent KEYCODE_WAKEUP")
        self.run_cmd("shell wm dismiss-keyguard")
        time.sleep(0.5)

    def set_demo_mode(self):
        """Enable standard clean demo mode (09:41, 100% battery, full wifi, no notifications)."""
        self.run_cmd("shell settings put global sysui_demo_allowed 1")
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command enter")
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0941")
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true")
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false")
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false")

    def exit_demo_mode(self):
        self.run_cmd("shell am broadcast -a com.android.systemui.demo -e command exit")

    def set_locale(self, locale: str):
        """Set both app-specific locale and system locale for consistent UI & widget."""
        print(f"  [>] Setting locale to {locale}...")
        self.run_cmd(f"shell cmd locale set-app-locales {PACKAGE_NAME} --locales {locale}")
        self.run_cmd(f"shell cmd locale set-device-locale {locale}")
        time.sleep(2.5)

    def set_geo(self, lat: float, lon: float):
        """Set emulator GPS fix."""
        self.run_cmd(f"emu geo fix {lon} {lat}")
        time.sleep(1.0)

    def nav_to(self, route_path: str, wait_sec: float = 4.0):
        """Navigate to in-app destination via deep link."""
        self.run_cmd(f"shell am start -a android.intent.action.VIEW -d \"wcinfo://route?path={route_path}\"")
        time.sleep(wait_sec)

    def capture_screenshot(self, output_file: Path):
        """Capture screen via adb exec-out screencap into target file."""
        output_file.parent.mkdir(parents=True, exist_ok=True)
        self.set_demo_mode()
        time.sleep(0.5)
        with open(output_file, "wb") as f:
            subprocess.run(f"{self.adb} -s {self.serial} exec-out screencap -p", shell=True, stdout=f)
        print(f"  [✓] Saved: {output_file} ({output_file.stat().st_size // 1024} KB)")

    def capture_suite(self, locale: str, output_dir: Path):
        lang_code = "de" if "de" in locale.lower() else "en"
        device_type = "Tablet Landscape" if self.is_tablet else "Phone Portrait"
        print(f"\n=======================================================")
        print(f" Capturing {device_type} [{lang_code.upper()}] -> {output_dir}")
        print(f"=======================================================")

        self.wake_device()
        # Dismiss any active dialogs
        self.run_cmd("shell input keyevent 4")
        time.sleep(0.5)

        self.set_locale(locale)
        self.set_geo(NAV_START_LAT, NAV_START_LON)

        # Launch main app
        self.run_cmd(f"shell am start -n {PACKAGE_NAME}/.MainActivity")
        time.sleep(3.0)

        # 1. Home
        print("  --> Scene 1: Home Screen")
        self.nav_to("home", wait_sec=3.0)
        self.capture_screenshot(output_dir / "01_home.png")

        # 2. Results
        print("  --> Scene 2: Results View (Berlin)")
        results_route = f"results%3Flat%3D{BERLIN_LAT}%26lon%3D{BERLIN_LON}%26name%3DBerlin"
        if self.is_tablet:
            results_route += f"%26toiletId%3D{DEFAULT_TOILET_ID}"
        self.nav_to(results_route, wait_sec=6.0)
        self.capture_screenshot(output_dir / "02_results.png")

        # 3. Compass
        print("  --> Scene 3: Compass Navigation")
        self.set_geo(NAV_START_LAT, NAV_START_LON)
        self.nav_to(f"compass/{DEFAULT_TOILET_ID}", wait_sec=3.5)
        self.capture_screenshot(output_dir / "03_compass.png")

        # 4. Detail
        print("  --> Scene 4: Detail View")
        self.nav_to(f"detail/{DEFAULT_TOILET_ID}", wait_sec=3.5)
        self.capture_screenshot(output_dir / "04_detail.png")

        # 5. Widget
        print("  --> Scene 5: Home Screen Widget")
        # Navigate to app home to trigger Glance widget locale update
        self.nav_to("home", wait_sec=2.5)
        # Press device Home button
        self.run_cmd("shell input keyevent 3")
        time.sleep(1.5)

        if self.is_tablet:
            # On tablet launcher landscape, swipe to page 2 where widget resides
            self.run_cmd("shell input swipe 2200 800 200 800 150")
            time.sleep(1.5)

        self.capture_screenshot(output_dir / "05_widget.png")


def detect_devices(adb: str) -> list[str]:
    """Get list of active online emulator/device serials."""
    res = subprocess.run(f"{adb} devices", shell=True, capture_output=True, text=True)
    devices = []
    for line in res.stdout.strip().splitlines()[1:]:
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def main():
    parser = argparse.ArgumentParser(description="Capture Play Store screenshots for WC-Info Android.")
    parser.add_argument("--phone", default=None, help="Serial of phone emulator (default: first connected or emulator-5554)")
    parser.add_argument("--tablet", default=None, help="Serial of tablet emulator (default: second connected or emulator-5556)")
    parser.add_argument("--phone-only", action="store_true", help="Capture phone screenshots only")
    parser.add_argument("--tablet-only", action="store_true", help="Capture tablet screenshots only")
    parser.add_argument("--locale", choices=["de-DE", "en-US", "all"], default="all", help="Target locale to capture")
    parser.add_argument("--output-dir", default="screenshots", help="Base output directory (default: screenshots)")
    args = parser.parse_args()

    adb = find_adb()
    connected = detect_devices(adb)

    phone_serial = args.phone
    tablet_serial = args.tablet

    if not phone_serial:
        phone_serial = connected[0] if len(connected) > 0 else "emulator-5554"
    if not tablet_serial:
        tablet_serial = connected[1] if len(connected) > 1 else "emulator-5556"

    locales = ["de-DE", "en-US"] if args.locale == "all" else [args.locale]
    base_out = Path(args.output_dir)

    print(f"Using ADB: {adb}")
    print(f"Connected devices: {connected or 'None detected (using defaults)'}")

    if not args.tablet_only:
        print(f"Target Phone Serial: {phone_serial}")
        phone_ctrl = DeviceController(adb, phone_serial, is_tablet=False)
        for loc in locales:
            code = "de" if "de" in loc.lower() else "en"
            phone_ctrl.capture_suite(loc, base_out / f"phone_portrait_{code}")

    if not args.phone_only:
        print(f"Target Tablet Serial: {tablet_serial}")
        tablet_ctrl = DeviceController(adb, tablet_serial, is_tablet=True)
        for loc in locales:
            code = "de" if "de" in loc.lower() else "en"
            tablet_ctrl.capture_suite(loc, base_out / f"tablet_landscape_{code}")

    print("\n[✔] Screenshot capture completed successfully!")


if __name__ == "__main__":
    main()
