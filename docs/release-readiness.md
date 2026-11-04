# Life Minder Release Readiness

## Milestones 7 and 8 manual verification

- Deny notification and exact-alarm permissions; confirm local records remain usable and the documented inexact fallback operates.
- Run two timers with the screen off; restart the application and device and confirm recovery.
- Exercise recurring reminder, routine, medication, event, and alarm actions including correction.
- Create overlapping events and itinerary entries and verify warnings do not block saving.
- Create a Detroit-to-Tokyo itinerary, change device time zone, and verify stored IANA labels remain unchanged.
- Enable airplane mode and verify trip dates, entries, addresses, confirmations, contacts, tasks, and references remain readable.
- Verify sensitive references and lodging access codes do not appear in ordinary JSON or notification previews.
- Exercise map, dialer, browser, copy, trip/entry duplication, move-by-day, and travel reminders.
- Test dark mode, 1.5× font scale, and one-handed capture on a physical phone.
- Grant and deny exact-alarm access, then verify one-minute timers in foreground, background, screen-off, locked, and removed-from-recents states.
- Exercise every alarm profile with sound plus vibration, delayed vibration, volume ramps, snooze exhaustion, and Standard/Hold/Number/Trace dismissal. Verify the emergency hold and accessibility-service fallback.
- Trigger two alarms close together and verify they queue without overlapping audio; verify previews stop when the profile editor closes.

Binary trip attachments remain intentionally deferred until a bundled archive can include and validate local files safely.

## Verified gates

- Kotlin formatting and desktop/common unit tests pass.
- Android lint passes for the debug application.
- Room migrations from schema version 1 through version 6 pass on the Android 16/API 36 emulator.
- Debug and release APK variants assemble successfully.
- The release APK installs and launches on the Pixel 7 API 36 emulator without a fatal log entry.
- Android UI launch was checked at 1.5× system font scale; onboarding and primary controls remained readable and operable.
- A 5,000-task analytics workload is covered by an automated test.
- Earlier offline-only builds were verified without Internet permission. Optional Weather today now explicitly requests Internet and approximate foreground-location access; verify its consent screen and the data flow documented in `weather.md` and `PRIVACY.md`.
- JSON export and raw database backup were exercised in the installed release. The generated JSON was non-empty and the backup contained the SQLite database, WAL, and shared-memory files.

## Release signing

The current release variant uses the local Android debug signing key solely so a standalone release APK can be installed during development verification. Before public distribution, replace this with a private production keystore and store its credentials outside the repository.

## User-controlled files

Android exports and backups are stored under the app-specific Documents directory in `LifeMinder`. JSON is versioned and human-readable. Imports validate format version, required values, enum values, IDs, and relationships before performing database writes. Raw restore validates the SQLite header and creates a rollback copy of the current database first.
