# BarbellBase

Offline-first strength-training tracker for Android. Everything lives in a local Room database, with optional Google cloud sync and JSON/CSV export through the system file picker.

## What it does

- **Workouts** — start from a routine day or empty, log weight × reps (or reps/time), per-set types (warm-up, drop, failure), RPE, auto rest timer with notification countdown and a "rest over" alarm, previous-session ghost values, session notes.
- **Routines** — routines → days → exercises with target sets, rep ranges and rest. Reorder days and exercises, start any day in two taps.
- **Routine templates** — import proven plans (Push/Pull/Legs, Upper/Lower, Full Body, 5-day split, Starting Strength, 5/3/1, dumbbell-only, bodyweight) as fully editable routines.
- **Exercise library** — ~80 seeded lifts across every muscle group, searchable and filterable; custom exercises supported.
- **History & progress** — session detail with volume/sets, weekly volume and session charts, estimated 1RM trends per lift, bodyweight line chart.
- **Reminders** — water rules ring with the real alarm sound (looping audio through a short-lived foreground service, wakes the screen, vibrates when the phone is silent); walking rules play a short chime. Per-rule interval, window, days of week, daily cap, snooze, ring length and targets.
- **Habits** — water and walking logs feed the reminder notifications, with 14-day mini charts.
- **Settings** — kg/lb, theme (dynamic/mono/dark/light), week start, weight step, rest defaults, water/walk targets, JSON export/import, CSV export.
- **Optional cloud sync** — sign in with Google to keep workouts, routines, exercises, habits and settings in sync across devices. Changes merge per record; offline use is unchanged.
- **In-app updates** — a daily check against Firebase Remote Config, with in-app download and install of newer APKs (SHA-256 verified).

## Build

Requirements: JDK 17 and the Android SDK with platform 36 and build-tools 36.

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot"
$env:ANDROID_HOME="C:\Android"
.\gradlew.bat :app:assembleRelease
```

The APK lands in `app/build/outputs/apk/release/app-release.apk`. It is signed with the committed debug keystore (`app/debug.keystore`, password `android`, alias `barbellbase-debug`) so every sideload build has a stable SHA-1 for Google Sign-In, and it is intentionally unminified. This keystore is for sideloading only — never use it for a Play Store release.

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

## Cloud sync setup (optional)

Cloud sync stays disabled until Firebase is configured. It uses Google Sign-In and Cloud Firestore directly, so no `google-services.json` is required and no project credentials are committed.

1. Create a Firebase project and add an Android app with package `com.david.barbellbase`.
2. Register the sideload signing certificate. The SHA-1 of `app/debug.keystore` is `55:AA:7A:E4:02:3D:0C:5C:18:FF:81:0A:CF:13:D2:25:32:CC:00:73`.
3. Enable **Google** under Authentication → Sign-in method, and create a **Cloud Firestore** database.
4. Add the following to `local.properties` (already gitignored):
   ```properties
   firebase.projectId=your-project-id
   firebase.applicationId=1:1234567890:android:abcdef
   firebase.apiKey=AIza...
   google.webClientId=1234567890-abc.apps.googleusercontent.com
   ```
   `google.webClientId` is the **Web client** OAuth ID shown under Authentication → Google → Web SDK configuration.
5. Publish Firestore rules that scope data to the signed-in user:
   ```
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       match /users/{uid}/{document=**} {
         allow read, write: if request.auth != null && request.auth.uid == uid;
       }
     }
   }
   ```
6. Rebuild. A **Cloud sync** section appears in Settings. Sign in with the same Google account on each device.

Sync stores one Firestore document per record under `users/{uid}/records/{table}:{id}` and resolves conflicts per record with last-write-wins. Deletions propagate as tombstones. Reminder rules and events stay device-local because alarms must be armed on each device.

Built-in exercises are seeded identically on every install, so untouched ones are never uploaded; only custom or edited entries sync. The incremental query also excludes the current device's own records, so a repeated sync does not re-read a batch it just wrote. Create a composite index on the `records` collection with `deviceId` (ascending) and `serverUpdatedAt` (ascending) for that query.

## In-app updates

BarbellBase checks Firebase Remote Config once a day and can download and install a newer APK from Settings → **Updates**. When a release is available, a banner at the bottom of the Home screen offers **Download** or **Skip**; skipping hides it for the rest of the local calendar day, and it returns the next day.

When a GitHub release is published, `.github/workflows/publish-update-config.yml` copies the release's version, APK URL, size, SHA-256 digest and notes into these Remote Config parameters: `latest_version`, `latest_tag`, `apk_url`, `apk_sha256`, `apk_size_bytes`, `release_notes`. The app then downloads directly from GitHub, verifies the SHA-256, and opens the system installer.

One-time setup for automatic publishing:

1. Firebase Console → Project settings → **Service accounts** → *Generate new private key*.
2. GitHub repo → **Settings → Secrets and variables → Actions** → add a secret named `FIREBASE_SERVICE_ACCOUNT` containing that JSON.

The first update also needs **Install unknown apps** permission for BarbellBase; the app links to the correct settings screen.

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
- With cloud sync enabled, the same records are also written to Firestore under your Google account and merged per record. Reminder rules and events remain device-local.
- The JSON backup covers database data only — app settings (theme, units, weight step, reminder defaults) live in DataStore and are not included. After an import, reminders are automatically re-scheduled so their next-fire times are current rather than stale values from the backup file. (Cloud sync does sync durable settings.)
- CSV export writes one row per completed set.
- Settings → **Reset tracked data** permanently clears workout sessions and sets, bodyweight entries, water/walk logs and reminder history (plus any in-progress session and rest timer). The exercise library, routines, reminder rules and settings are kept, and armed reminders stay armed. It confirms first and cannot be undone, so export a JSON backup beforehand if the history matters.
- Uninstalling deletes local data. Cloud data remains in your Google account and is restored on the next sync.

## License

[MIT](LICENSE)
