# MBF Launcher — Pixel 9a

A lightweight, independently written Niagara-inspired Android home screen. No Niagara Pro code is used.

## Features in version 0.1
- Compact **Classic** and larger **Modern** layouts; saved preference, dark/light theme.
- Installed application list, tappable and drag-scroll A–Z index on the right.
- Long-press an app to add/remove it from the favorites shown on the home screen.
- Search installed applications, open apps, date/time, one-handed layout with readable touch targets.
- Notification preview cards near the top: tap to open, or **Reply** for apps exposing Android's RemoteInput reply actions. Notification access is **opt-in**, all processing stays on-device. Some apps will not offer replies.

## Get the APK without Android Studio
Open **Actions → Build MBF Launcher APK → latest successful run → Artifacts → MBF-Launcher-Pixel9a-debug**. Download the ZIP, extract `app-debug.apk` and open it on the Pixel 9a. Allow installations from the file app when Android asks. The APK is a debug build, not a Play Store release. Android may warn about sideloading.

Open **Settings → Apps → Default apps → Home app → MBF Launcher** to select it as your launcher. In MBF Launcher, open **⚙ → Enable notifications**, and grant **Notification access** only if you want notification previews/replies.

## Build locally (optional)
Install JDK 17, Android SDK 35 and Gradle 8.13. Run `gradle :app:assembleDebug` from the project root.

## Current limitations
No user-added Android widgets, automatic backups, custom fonts, app icon packs or sweeping gesture editor yet. The launcher doesn't replace the Android notification shade/lock screen; remote replies depend on each application's notification actions. This first version needs on-device testing before daily use.
