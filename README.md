# BarbellBase

Offline strength-training tracker for Android. No account, no cloud, no GPS — everything lives in a local Room database, with JSON/CSV export through the system file picker.

## What it does

- **Workouts** — start from a routine day or empty, log weight × reps (or reps/time), per-set types (warm-up, drop, failure), RPE, auto rest timer with notification countdown and a "rest over" alarm, previous-session ghost values, session notes.
- **Routines** — routines → days → exercises with target sets, rep ranges and rest. Reorder days and exercises, start any day in two taps.
- **Exercise library** — ~80 seeded lifts across every muscle group, searchable and filterable; custom exercises supported.
- **History & progress** — session detail with volume/sets, weekly volume and session charts, estimated 1RM trends per lift, bodyweight line chart.
- **Reminders** — water rules ring with the real alarm sound (looping audio through a short-lived foreground service, wakes the screen, vibrates when the phone is silent); walking rules play a short chime. Per-rule interval, window, days of week, daily cap, snooze, ring length and targets.
- **Habits** — water and walking logs feed the reminder notifications, with 14-day mini charts.
- **Settings** — kg/lb, theme (dynamic/mono/dark/light), week start, weight step, rest defaults, water/walk targets, JSON export/import, CSV export.

## Build

Requirements: JDK 17 and the Android SDK with platform 36 and build-tools 36.

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME="C:\Android"
.\gradlew.bat :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/app-release.apk`. It is signed with the standard debug keystore (`~/.android/debug.keystore`) so it can be sideloaded without extra setup, and it is intentionally unminified.

Unit tests for the scheduling maths and unit conversions:

```powershell
.\gradlew.bat :app:testDebugUnitTest
```

## Install

1. On the phone: Settings → About phone → tap *Build number* 7× to enable developer options.
2. Settings → System → Developer options → enable **USB debugging**.
3. Connect USB, then:

```powershell
adb install -r app\build\outputs\apk\release\app-release.apk
```

Alternatively copy the APK to the phone and open it, allowing "install unknown apps" for the file manager.

## First-run checklist

1. Allow **notifications** when prompted (needed for every reminder).
2. Reminders tab → **Allow exact alarms** so water alarms arrive on time instead of being batched by Doze.
3. If your phone has aggressive battery management, set BarbellBase's battery usage to **Unrestricted** (Settings → Apps → BarbellBase → Battery) so the system does not kill alarm delivery.
4. Optional: adjust water/walk targets in Settings → Reminder defaults.

## Notes on alarms

- `AlarmManager.setExactAndAllowWhileIdle` is used when exact-alarm access is granted; otherwise the app falls back to `setAndAllowWhileIdle` (may be delayed).
- Each rule arms exactly one chained alarm: after it fires, the next slot is computed from the rule's window/days and re-armed.
- Alarms are repaired after reboot, timezone/time changes, app updates, and by a daily WorkManager reconciliation pass.
- Water alarms play through `ReminderAlertService` (technically a `mediaPlayback` foreground service, capped by the rule's ring length); if the phone is on silent/vibrate the rule vibrates instead.

## Data

- Database: `barbellbase.db` in app storage (exported to JSON on demand).
- JSON export/import round-trips exercises, routines, sessions, sets, body metrics, reminder rules and habit logs.
- The JSON backup covers database data only — app settings (theme, units, weight step, reminder defaults) live in DataStore and are not included. After an import, reminders are automatically re-scheduled so their next-fire times are current rather than stale values from the backup file.
- CSV export writes one row per completed set.
- Settings → **Reset tracked data** permanently clears workout sessions and sets, bodyweight entries, water/walk logs and reminder history (plus any in-progress session and rest timer). The exercise library, routines, reminder rules and settings are kept, and armed reminders stay armed. It confirms first and cannot be undone, so export a JSON backup beforehand if the history matters.
- Uninstalling deletes everything.
