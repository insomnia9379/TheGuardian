# Guardian Watchdog

An Android watchdog that catches monitoring / stalkerware the second it appears —
in **all** the places these things hide, not just the app drawer.

Built for your case: AirDroid Parental Control (`com.sand.airdroidkidp`) +
AirDroid Kids (`com.sand.airdroidkids`), including the "won't let me delete it"
device-admin trick and the Play auto-restore after factory reset.

## What it watches

1. **App installs** — `InstallReceiver` fires on `PACKAGE_ADDED`. If the package
   is in `ThreatDatabase` (AirDroid family, mSpy, FlexiSPY, etc.) you get an
   instant high-priority notification. Suspicious keywords also trigger a low
   alert.
2. **5-minute watchdog** — `WatchdogForegroundService` runs as a foreground
   service, scanning every 5 minutes even when the app is closed. Survives Doze.
   Restarts on boot via `BootReceiver`.
3. **Daily 9am deep scan** — `DailyScanReceiver` via `AlarmManager`. Full audit
   of all vectors, huge alert if anything found, quiet "clean" note otherwise.
4. **Device Admin apps** — the "can't uninstall" vector. Lists active admins,
   flags known threats as HIGH.
5. **Accessibility services** — can read screen + keystrokes. Flags threats.
6. **Notification listeners** — can read texts/WhatsApp. Flags threats.
7. **ADB / USB debugging** — allows silent installs from a PC. Warns if on.
8. **Audit log** — append-only timestamped log of every detection + manual scan.
   Proof of when something was installed.

## Huge alerts

`AlertHelper.sendHugeAlert()` uses:
- `PRIORITY_MAX` + `CATEGORY_ALARM`
- Vibration pattern + red LED
- `setFullScreenIntent()` — pops MainActivity over lock screen, like an alarm
- "REVIEW NOW" action button
- Requires `USE_FULL_SCREEN_INTENT` permission (Android 14+ asks at runtime)

You asked for "huge" — this is as huge as Android allows without system rights.

## Why can't it *block* installs? How does Package Disabler do it?

You're right to ask — here's the real answer:

**Regular apps can't block installs.** Android's security model doesn't give
third-party apps a veto on `PACKAGE_ADDED`. We can only observe and scream
loudly.

**Package Disabler apps cheat by getting system-level power one of these ways:**

1. **Device Owner provisioning** — via ADB: `adb shell dpm set-device-owner com.packagedisabler/.AdminReceiver`.
   Once you're Device Owner, `DevicePolicyManager.setApplicationHidden()` can
   actually hide/disable any package. This is the legit enterprise API.
   Downside: factory reset is required to provision, and only one Device Owner
   can exist.

2. **Samsung Knox SDK** — on Samsung phones, Knox gives `ApplicationPolicy.setDisableApplication()`
   with a Knox license. That's why most "Package Disabler Pro" apps are
   Samsung-only.

3. **Shizuku / ADB elevated** — Shizuku lets an app run with ADB-level
   permissions (`pm disable-user`). No root, but needs a PC or wireless
   debugging to start Shizuku each boot.

4. **Root** — `pm uninstall` / `pm disable` as root. Full power, voids warranty.

**If you want Guardian to also *disable* (not just alert):**
The cleanest path is Device Owner via ADB — one-time PC command, no root.
I can add a `DeviceOwnerReceiver` + a "DISABLE NOW" button that calls
`setApplicationHidden()` when we're Device Owner. Say the word and I'll add it.
Without that, we're an alarm system, not a lock — which is still what you need
to catch it in the act.

## Project layout

```
app/src/main/
  AndroidManifest.xml
  java/com/guardian/watchdog/
    ThreatDatabase.kt        # known bad packages
    InstallReceiver.kt       # install tripwire
    WatchdogForegroundService.kt # 5-min checks
    Schedulers.kt            # daily 9am + boot restart
    PrivilegeChecker.kt      # scans all hiding spots
    AlertHelper.kt           # normal + HUGE alerts
    AuditLog.kt              # audit trail
    MainActivity.kt          # SCAN NOW UI + starts service
```

## Build

1. Open `guardian-watchdog` in Android Studio (or `File > Open`).
2. Add a minimal `app/build.gradle` if you scaffolded without the wizard:
   ```gradle
   plugins { id 'com.android.application'; id 'org.jetbrains.kotlin.android' }
   android {
     namespace 'com.guardian.watchdog'
     compileSdk 34
     defaultConfig {
       applicationId "com.guardian.watchdog"
       minSdk 26
       targetSdk 34
       versionCode 1
       versionName "1.0"
     }
   }
   dependencies {
     implementation 'androidx.appcompat:appcompat:1.6.1'
     implementation 'androidx.core:core:1.12.0'
   }
   ```
3. Run on your Pixel. Grant notifications permission.
4. Tap **SCAN NOW**. Leave it installed — the receiver works even when the app
   isn't open.

## What it can't do (Android limits)

- It **can't block** an install, only alert instantly. Blocking requires system
  privileges Android doesn't give regular apps.
- It **can't remove** a device admin by itself — you must do
  Settings > Security > Device admin apps > deactivate manually.
- Play auto-restore after factory reset will still reinstall apps, but now
  you'll get an alert the second it happens, with the audit log as proof.

## Your immediate checklist (no app needed)

- [x] Factory reset removed the admin lock
- [ ] Play Store > Subscriptions > cancel AirDroid Parental Control
- [ ] Settings > Security > Device admin apps — confirm empty
- [ ] Settings > Accessibility — review enabled services
- [ ] Change lock screen + Google password if someone had physical access

Stay safe.
