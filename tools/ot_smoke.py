#!/usr/bin/env python3
"""End-to-end smoke test for The Oregon Trail (Android).

Drives the real app on a connected device/emulator using taps whose
coordinates are computed from the debug screen dump the app writes to
logcat (tag "OTS"). Requires a **debug** build.

Usage:
    python3 tools/ot_smoke.py [--serial emulator-5554] [--max-steps 400]

Exit code 0 = the app was driven through buying supplies, a river
crossing and at least one landmark without crashing; 1 = failure.
"""
import argparse
import os
import re
import subprocess
import sys
import time

PKG = "com.oregontrail.app"


def find_adb() -> str:
    adb = os.environ.get("ADB")
    if adb and os.path.exists(adb):
        return adb
    sdk = os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
    if sdk:
        cand = os.path.join(sdk, "platform-tools", "adb")
        if os.path.exists(cand):
            return cand
    return "adb"


class Driver:
    def __init__(self, serial=None, max_steps=400, verbose=True):
        self.adb = find_adb()
        self.dev = ["-s", serial] if serial else []
        self.max_steps = max_steps
        self.verbose = verbose
        self.seen_phases = set()

    def sh(self, *args, check=True, capture=False):
        cmd = [self.adb] + self.dev + list(args)
        if capture:
            return subprocess.run(cmd, check=check, capture_output=True, text=True)
        return subprocess.run(cmd, check=check)

    def log(self, msg):
        if self.verbose:
            print(msg, flush=True)

    def logcat(self) -> str:
        return subprocess.run(
            [self.adb] + self.dev + ["logcat", "-d", "-s", "OTS"],
            capture_output=True, text=True
        ).stdout

    def last_screen(self):
        lines = self.logcat().splitlines()
        idx = None
        for i, l in enumerate(lines):
            if "grid=" in l:
                idx = i
        if idx is None:
            return None
        grid = lines[idx]
        m = re.search(r"grid=\((\d+), (\d+)\).*phase=(\w+)", grid)
        if not m:
            return None
        cols, rows, phase = int(m.group(1)), int(m.group(2)), m.group(3)
        mm = re.search(r"metrics=([\d.]+),([\d.]+),([\d.]+),([\d.]+)", grid)
        cw, lh, mx, my = map(float, mm.groups())
        om = re.search(r"origin=(-?\d+),(-?\d+)", grid)
        ox, oy = (int(om.group(1)), int(om.group(2))) if om else (0, 0)
        scr = [""] * rows
        for l in lines[idx + 1:]:
            rm = re.search(r"R(\d+)\|(.*)$", l)
            if rm:
                r = int(rm.group(1))
                if r < rows:
                    scr[r] = rm.group(2)
        return dict(cols=cols, rows=rows, phase=phase, cw=cw, lh=lh,
                    mx=mx, my=my, ox=ox, oy=oy, scr=scr)

    def wait_screen(self, timeout=90.0):
        """Waits for the first screen dump, re-launching the app if the cold
        start is slow (CI emulators can take a minute or more)."""
        deadline = time.time() + timeout
        last_launch = time.time()
        while time.time() < deadline:
            s = self.last_screen()
            if s:
                return s
            if time.time() - last_launch > 20:
                self.sh("shell", "am", "start", "-n", PKG + "/.MainActivity", check=False)
                last_launch = time.time()
            time.sleep(0.5)
        raise RuntimeError("no screen dump found - is this a debug build and is the app running?")

    def wait_stable(self, timeout=20.0):
        """Waits until the view has done its first layout and the grid stops
        changing, so tap coordinates are computed from real metrics."""
        prev = None
        deadline = time.time() + timeout
        while time.time() < deadline:
            s = self.last_screen()
            if s and s["cw"] > 0:
                key = (s["cols"], s["rows"], round(s["cw"], 2), round(s["lh"], 2),
                       s["phase"], s["ox"], s["oy"])
                if key == prev:
                    return s
                prev = key
            time.sleep(0.5)
        return self.last_screen()

    def tap_cell(self, row, col, screen=None):
        s = screen or self.last_screen()
        x = int(s["ox"] + s["mx"] + (col + 0.5) * s["cw"])
        y = int(s["oy"] + s["my"] + (row + 0.5) * s["lh"])
        self.sh("shell", "input", "tap", str(x), str(y))
        time.sleep(0.35)

    def find(self, text, screen=None):
        s = screen or self.last_screen()
        for r, line in enumerate(s["scr"]):
            c = line.find(text)
            if c >= 0:
                return r, c, s
        return None

    def tap_text(self, text, required=True):
        for _ in range(6):
            s = self.last_screen()
            if s and s["cw"] > 0:
                hit = self.find(text, s)
                if hit:
                    r, c, s = hit
                    self.tap_cell(r, c + len(text) // 2, s)
                    return True
            time.sleep(0.3)
        if required:
            raise RuntimeError("tapped text not found: %r" % text)
        return False

    def tap_plus(self, label, times=1):
        for _ in range(times):
            s = self.last_screen()
            r = None
            line = None
            for i, l in enumerate(s["scr"]):
                if label in l:
                    r, line = i, l
                    break
            if r is None:
                raise RuntimeError("store row not found: %r" % label)
            c = line.rfind("[+]")
            if c < 0:
                raise RuntimeError("no [+] on row: %r" % line)
            self.tap_cell(r, c + 2, s)

    def capture(self, tag):
        s = self.last_screen()
        if s and s["phase"] not in self.seen_phases:
            self.seen_phases.add(s["phase"])
            self.log("  reached phase %s (grid %dx%d)" % (s["phase"], s["cols"], s["rows"]))


def run(serial=None, max_steps=400, verbose=True):
    d = Driver(serial=serial, max_steps=max_steps, verbose=verbose)
    d.log("== Oregon Trail smoke test ==")
    d.sh("shell", "am", "force-stop", PKG, check=False)
    d.sh("shell", "pm", "clear", PKG, check=False)
    d.sh("logcat", "-c", check=False)
    # Don't let the "swipe up to exit fullscreen" prompt eat our first tap.
    d.sh("shell", "settings", "put", "secure", "immersive_mode_confirmations", "confirmed", check=False)
    d.sh("shell", "am", "start", "-n", PKG + "/.MainActivity")
    time.sleep(4)
    try:
        d.wait_screen()
        d.wait_stable()
        return _drive(d)
    except Exception:
        d.log("--- recent device logcat (app/crash lines) ---")
        try:
            diag = subprocess.run(
                [d.adb] + d.dev + ["logcat", "-d", "-t", "600"],
                capture_output=True, text=True
            ).stdout
            for line in diag.splitlines():
                if any(k in line for k in ("oregontrail", "OTS", "FATAL", "AndroidRuntime")):
                    d.log(line)
        except Exception:
            pass
        raise


def _drive(d):
    d.capture("title")

    # Set up a fresh journey.
    d.tap_text("1. Travel the trail")
    d.capture("profession")
    d.tap_text("1. Banker")
    d.capture("month")
    d.tap_text("1. March")
    d.capture("names")
    d.tap_text("[ Begin the journey ]")
    d.capture("store")
    d.tap_plus("Oxen", 3)
    d.tap_plus("Food", 60)
    d.tap_plus("Cloths", 4)
    d.tap_plus("Ammo", 12)
    d.tap_plus("Wheel", 2)
    d.tap_plus("Axle", 2)
    d.tap_plus("Tongue", 2)
    d.tap_text("[ Leave the store ]")
    d.tap_text("[ Continue ]")
    d.capture("travel")

    reached_landmark = False
    reached_river = False
    for step in range(d.max_steps):
        s = d.last_screen()
        if not s:
            raise RuntimeError("lost the screen dump mid-run")
        phase = s["phase"]
        d.capture(phase)
        if phase == "LANDMARK":
            reached_landmark = True
        if phase == "RIVER":
            reached_river = True
        if phase == "ARRIVED":
            d.log("  ARRIVED in Oregon after %d steps" % step)
            return True
        if phase == "DEATH":
            d.log("  party died after %d steps (still a valid run)" % step)
            # A death is an acceptable smoke result once we've exercised phases.
            return reached_landmark and reached_river
        if phase == "TRAVEL":
            d.tap_text("1. Continue on trail")
        elif phase == "NOTICE":
            d.tap_text("[ Continue ]")
        elif phase == "CHOICE":
            d.tap_text("No thanks", required=False) or d.tap_text("3. Continue", required=False) \
                or d.tap_text("4. Circle", required=False) or d.tap_cell(0, 0)
        elif phase == "RIVER":
            d.tap_text("Take the ferry", required=False) or d.tap_text("Caulk", required=False) \
                or d.tap_text("Ford the river", required=False)
        elif phase == "LANDMARK":
            if "The Dalles" in "\n".join(s["scr"]):
                d.tap_text("Raft down", required=False) or d.tap_text("Barlow", required=False)
            else:
                d.tap_text("Continue on the trail", required=False) \
                    or d.tap_text("Continue on trail", required=False)
        elif phase == "RAFTING":
            d.tap_text("RIGHT >>", required=False) or d.tap_text("<< LEFT", required=False)
        elif phase == "HUNTING":
            d.tap_text("[ Return to trail ]")
        elif phase == "MAP":
            d.tap_text("[ Back ]")
        elif phase == "JOURNAL":
            d.tap_text("[ Back ]")
        elif phase == "STORE":
            d.tap_text("[ Leave the store ]")
        else:
            break
    d.log("  stopped after %d steps; phases=%s" % (d.max_steps, sorted(d.seen_phases)))
    return reached_landmark and reached_river


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--serial", default=None)
    ap.add_argument("--max-steps", type=int, default=400)
    ap.add_argument("--quiet", action="store_true")
    args = ap.parse_args()
    try:
        ok = run(args.serial, args.max_steps, verbose=not args.quiet)
    except Exception as e:  # noqa: BLE001
        print("SMOKE TEST FAILED: %s" % e, file=sys.stderr)
        return 1
    print("SMOKE TEST PASSED" if ok else "SMOKE TEST FAILED (insufficient progress)")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
