# ReelCounter for iPhone

Same product idea as the Android app: count Instagram Reels / YouTube Shorts, keep history on device, show a live count, and alert at 100 / 150 / 200.

## Important iOS limitation

Apple does **not** allow a third-party app to:

- read Instagram/YouTube’s view tree
- draw a floating pill *inside* those apps
- run an Android-style AccessibilityService

So iOS uses the closest personal-use equivalent:

| Android | iPhone |
|---------|--------|
| Accessibility overlay pill | **Live Activity** (Dynamic Island / Lock Screen) |
| Auto-detect scroll page change | **Back Tap / Action Button / Shortcut / in-app +** to count each reel |
| Room SQLite | **SwiftData** (same daily_stats fields) |
| Setup checklist | Setup for notifications + Back Tap + Live Activities |

Functionality you still get: today totals, Instagram vs YouTube split, metres/time estimates, 7/30-day history chart, daily limit, milestones, reset/clear data.

## Requirements

- Mac with **Xcode 15+** (this machine has Xcode)
- iPhone on **iOS 17+**
- Free or paid Apple ID for signing (sideload via Xcode)

## Build & install

```bash
cd ios
xcodegen generate
open ReelCounter.xcodeproj
```

In Xcode:

1. Select the **ReelCounter** target → **Signing & Capabilities**
2. Choose your **Team** (personal Apple ID is fine)
3. Plug in iPhone, trust the computer
4. Select your iPhone as run destination → press **Run** ▶

On the phone: Settings → General → VPN & Device Management → trust your developer certificate.

## How to use (same flow, iOS-shaped)

1. Open **ReelCounter** → finish Setup
2. Home → tap **Instagram** or **YouTube** to start a session (Live Activity appears)
3. Switch to Reels / Shorts
4. Count each reel with one of:
   - **Back Tap**: Settings → Accessibility → Touch → Back Tap → Double Tap → Shortcut **Count a reel**
   - **Action Button** / Control Center shortcut running **Count one reel**
   - Big **Count 1 reel** button after switching back (or Split View / Stage Manager on iPad)
5. Live Activity shows the number (and turns red-style for milestone alerts via notification + activity text)
6. History / Settings match the Android screens

## Privacy

No internet permission / no analytics. All stats stay in the on-device SwiftData store.
