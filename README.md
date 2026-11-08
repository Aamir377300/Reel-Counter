# ReelCounter

Personal app that counts how many **Instagram Reels** and **YouTube Shorts** you scroll each day, and shows a live count while you watch.

- **Android** (Kotlin) — AccessibilityService overlay (auto-detect scrolls)
- **iPhone** (SwiftUI) — Live Activity + Back Tap / Shortcuts (see [`ios/IOS.md`](ios/IOS.md))

---

# Android

Personal Android app that counts how many **Instagram Reels** and **YouTube Shorts** you scroll each day, and shows a live overlay while you’re in those screens.

- Kotlin + Jetpack Compose (Material 3)
- Room (on-device only — **no internet permission**, no analytics)
- AccessibilityService for scroll detection + `TYPE_ACCESSIBILITY_OVERLAY` pill
- Package: `com.aamir.reelcounter`
- Sideload only — **not** for Play Store

---

## Table of contents

1. [Build the APK (on your computer)](#1-build-the-apk-on-your-computer)
2. [Install on your Android phone](#2-install-on-your-android-phone)
3. [First-time setup (required)](#3-first-time-setup-required)
4. [How to use the app](#4-how-to-use-the-app)
5. [Understanding the overlay](#5-understanding-the-overlay)
6. [Settings explained](#6-settings-explained)
7. [Updating view-ID keywords after Instagram/YouTube updates](#7-updating-view-id-keywords-after-instagramyoutube-updates)
8. [Troubleshooting](#8-troubleshooting)
9. [Project layout](#9-project-layout)

---

## 1. Build the APK (on your computer)

### Requirements

- JDK 17+
- Android SDK with **platform 34** and a recent build-tools package
- On macOS/Linux, set `sdk.dir` in `local.properties` (already present on the machine that built this project):

```properties
sdk.dir=/Users/YOUR_USER/Library/Android/sdk
```

### Build debug APK

From the project root:

```bash
./gradlew assembleDebug
```

When it succeeds, the APK is here:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Optional release build (still unsigned / debug-key unless you add your own signing config):

```bash
./gradlew assembleRelease
```

---

## 2. Install on your Android phone

### Option A — USB (adb)

1. On the phone: **Settings → About phone → tap Build number 7 times** to enable Developer options.
2. **Settings → Developer options → USB debugging → ON**.
3. Plug in the phone and allow the computer.
4. Copy and install:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Option B — File transfer (no computer tools)

1. Copy `app-debug.apk` to the phone (AirDrop, Google Drive, cable, WhatsApp to yourself, etc.).
2. On the phone open the APK with **Files** / **My Files**.
3. If Android blocks it: **Settings → Apps → Special app access → Install unknown apps** → allow the app you’re installing from (Files, Chrome, Drive…).
4. Tap **Install**.

> This app is for personal sideloading. You do **not** need a Play Store listing.

---

## 3. First-time setup (required)

Open **ReelCounter**. You’ll see a setup checklist. Complete every step.

### Step 1 — Enable the accessibility service

1. In the app, tap **Enable** next to “Enable accessibility service”.
2. Android opens **Settings → Accessibility**.
3. Find **ReelCounter** (sometimes under “Downloaded apps” / “Installed services”).
4. Turn it **ON** and accept the system warning.

**What it does:** reads scroll / window events and a few view IDs so it can tell when you’re on Reels or Shorts. It does **not** read DMs, passwords, or send data off the phone.

#### Android 13+ — “Allow restricted settings”

On many phones, Accessibility for sideloaded apps is greyed out until you unlock it:

1. **Settings → Apps → ReelCounter**.
2. Tap the **⋮** (three-dot) menu in the top-right.
3. Tap **Allow restricted settings**.
4. Go back to Accessibility and enable ReelCounter.

The setup screen has a button that opens app settings for this.

### Step 2 — Notification permission (Android 13+)

Tap **Enable** and allow notifications. Used only for:

- Daily reel-limit alert
- A notice if the accessibility service gets turned off

### Step 3 — Battery = Unrestricted

Android will otherwise kill background accessibility services aggressively.

1. Tap **Enable** on “Unrestricted battery”, **or**
2. Manually: **Settings → Apps → ReelCounter → Battery → Unrestricted**  
   (wording varies: “No restrictions”, “Don’t optimize”, etc.)

OEM tips:

| Brand   | Where to look                                      |
|---------|----------------------------------------------------|
| Samsung | Battery → Background usage limits / Never sleeping |
| Xiaomi  | Autostart + Battery saver → No restrictions        |
| Oppo/Vivo | Allow auto-start + high background power         |
| Pixel   | App battery usage → Unrestricted                   |

### Step 4 — Done

When the checklist is all green, tap **Continue**. You can reopen setup later by turning the accessibility service off (the app will nudge you back).

---

## 4. How to use the app

### Daily flow

1. Leave ReelCounter running in the background (no need to keep the UI open).
2. Open **Instagram → Reels** or **YouTube → Shorts**.
3. A small pill appears at the **top-right** (default):

   `Reels: 42 | 120 m | 18 min`

4. Scroll as usual — the count updates live.
5. Leave Reels/Shorts (home, Feed, another app, lock screen) → the overlay **hides**.
6. Open ReelCounter anytime to see today’s totals and history.

### Home tab

- Today’s **total reels**, **metres scrolled**, and **time spent**
- Split by **Instagram** vs **YouTube**

### History tab

- Switch **7 days** / **30 days**
- Bar chart of reels per day (drawn with Compose Canvas)
- List of each day with distance + time

### Settings tab

See [Settings explained](#6-settings-explained).

---

## 5. Understanding the overlay

| Piece        | Meaning |
|--------------|---------|
| `Reels: N`   | How many Reels/Shorts counted today (both apps combined) |
| `X m`        | Approximate finger-scroll distance converted from pixels using your screen DPI |
| `Y min`      | Time spent *inside* Reels/Shorts screens today |

Rules:

- Visible **only** on tracked Reels/Shorts screens
- **Not touchable** — taps pass through to Instagram/YouTube
- Uses `TYPE_ACCESSIBILITY_OVERLAY` (no separate “draw over other apps” permission)
- Can be turned off or moved top-left / top-right in Settings

---

## 6. Settings explained

| Setting | Default | What it does |
|---------|---------|--------------|
| Daily limit | 100 | When today’s total crosses the limit → one notification + one vibration (once per day) |
| Show overlay | On | Hide/show the live pill |
| Overlay position | Top right | Top left or top right |
| Debug mode | Off | Logs view IDs with tag `REEL_DEBUG` so you can fix keywords after app updates |
| Reset today | — | Deletes today’s stats only |
| Clear all data | — | Wipes the whole Room database (confirm dialog) |

---

## 7. Updating view-ID keywords after Instagram/YouTube updates

Detection looks for keywords in view resource IDs / content descriptions (e.g. `clips`, `reel`, `shorts`). Apps change these over time.

### Edit the config

Open:

```text
app/src/main/java/com/aamir/reelcounter/util/TrackingKeywords.kt
```

Update:

- `INSTAGRAM_REELS_HINTS`
- `YOUTUBE_SHORTS_HINTS`

Rebuild and reinstall the APK.

### Discover new IDs with debug mode

1. In ReelCounter → **Settings → Debug mode → ON**.
2. Open Instagram Reels or YouTube Shorts and scroll a bit.
3. On your computer:

```bash
adb logcat -s REEL_DEBUG
```

4. Copy useful ID fragments that clearly mean “reels/shorts viewer” into `TrackingKeywords.kt`.
5. Turn debug mode **OFF** when finished (less log noise).

Tunable timing constants (debounce, flush interval, tree depth) live at the top of:

```text
app/src/main/java/com/aamir/reelcounter/service/ReelTrackerService.kt
```

---

## 8. Troubleshooting

| Problem | Fix |
|---------|-----|
| Overlay never appears | Accessibility off? Battery restricted? Confirm you’re on the Reels/Shorts tab, not Feed/Home. Turn on Debug mode and check keywords. |
| Count stuck at 0 | Keywords outdated — use Debug mode + `adb logcat -s REEL_DEBUG`. Or debounce may be filtering — try scrolling fully to the next reel. |
| Service dies overnight | Set battery to **Unrestricted**; on Xiaomi/Oppo also enable Autostart. |
| Accessibility greyed out | **Allow restricted settings** (Android 13+), then enable the service again. |
| App says service is off | Open the app → follow setup → re-enable Accessibility. |
| Want a clean slate | Settings → Reset today / Clear all data. |

Privacy reminder: there is **no `INTERNET` permission**. Stats never leave the device.

---

## 9. Project layout

```text
app/src/main/java/com/aamir/reelcounter/
  service/ReelTrackerService.kt   # Accessibility + counting + overlay updates
  service/OverlayController.kt    # TYPE_ACCESSIBILITY_OVERLAY pill
  data/                           # Room entity, DAO, DB, repository, preferences
  ui/                             # Compose screens + ViewModels + theme
  util/                           # Keywords, dates, px→m, permissions
app/src/main/res/xml/accessibility_config.xml
```

---

## Assumptions

- “One reel” ≈ a visible page change (`fromIndex`/`toIndex` or a large `scrollY` jump); otherwise a **600 ms** scroll debounce fallback.
- Scroll distance is `|scrollDeltaY|` summed and converted with screen DPI → metres (approximate physical finger travel, not video length).
- Time is measured only while the service believes you’re on a Reels/Shorts screen.
- Instagram package: `com.instagram.android` · YouTube: `com.google.android.youtube` (standard Play Store apps, not clones).
- Overlay totals are **combined** for both apps; Home screen also shows the per-app split.
