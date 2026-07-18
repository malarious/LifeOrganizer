# Life Minder

Life Minder is a local-first Android organizer for tasks, schedules, reminders, routines, medications, events, alarms, timers, reviews, and travel information. It is designed to work without an account, subscription, cloud service, telemetry, advertising, or AI dependency.

The product is actively evolving. Its data and scheduling foundations are implemented, while navigation, terminology, screen structure, and visual layout are still open to revision.

## Current capabilities

### Tasks and planning

- Plans/projects, trips, short-term checklists, unassigned-task capture, search, tags, dependencies, five-level colored priorities, effort requirements, date/time scheduling, recurrence, and blocked-state explanations
- Today-first planning with a unified timeline, current-date reconciliation for repeating alarms, overdue/upcoming grouping, and direct navigation from timeline cards to the relevant task, trip, life-item, alarm, or timer screen
- Do Now recommendations based on available time, energy, priority, dependencies, urgency, and prior feedback; available time is adjustable from 5 to 120 minutes in five-minute increments
- Completion, cancellation, deletion, undo, recurring-task history, and task-linked work sessions
- Persistent light/dark appearance selection with theme-aware cards and controls

### Time and life items

- Standalone reminders with complete, dismiss, snooze, and recurrence behavior
- Routines with occurrences, completion, skip, pause/resume, history, and completion rates
- Medication schedules and occurrence records with taken, skipped, snoozed, correction, and optional refill quantities
- Events with duration, location fields, recurrence, and explicit time-zone identifiers
- Standalone alarms with enable/disable, sound, vibration, configurable snooze duration storage, acknowledgment, deletion, editing, and restart restoration
- Multiple simultaneous countdown timers with optional names, one-tap quick durations, hours/minutes/seconds custom entry, favorite presets, recent restart/save actions, a custom audible completion screen, automatic removal after acknowledgment, enable/disable controls, and timer deletion
- Stopwatch and task-linked productivity timers with millisecond display, pause/resume, time adjustment, overtime calculations, and persistent work-session history (sessions shorter than one minute are omitted)
- Clock alarms with an app-owned queued alarm screen, bundled Kenney jingles, simultaneous music/vibration, delayed vibration modes, gradual volume ramps, snooze limits, Standard/Hold/Number/Trace dismissal, an accessible emergency fallback, weekday selection, and once, weekly, bi-weekly, monthly, or custom hourly/daily/weekly/monthly recurrence
- Live reusable alarm profiles with built-in Gentle, Standard, Heavy Sleeper, Silent Vibration, and Reminder defaults; built-ins are copied before customization and existing alarms retain their sound/vibration behavior during migration
- Repeating-alarm acknowledgment advances the persisted date to the next valid future occurrence and reschedules it
- Upcoming filtering and a Today timeline that distinguishes actionable items from passive events while excluding timers from overdue treatment

Life Minder medication features are organizational aids only. They do not provide dosing, interaction, diagnostic, or other medical advice.

### Trips and itineraries

- Offline trips with dates, status, notes, time zones, and overview warnings
- Typed itinerary entries including flights, trains, lodging, events, restaurants, activities, transfers, and general entries
- Transportation and lodging details, confirmation information, addresses, contacts, websites, and sensitive reference cards
- Existing-task associations, itinerary reminders, conflict detection, duplication, and moving entries between days
- Task-first trip views with Add Task and Add Place actions, editable place cards, completion controls, and automatic hiding of completed, cancelled, or already-ended itinerary entries
- Android handoff to installed map, dialer, and browser applications, plus clipboard copying
- Full JSON portability and a trip-only package service with sensitive-field redaction

Binary trip attachments are intentionally deferred until they can be included safely in a validated archive backup.

### Reviews and data safety

- Daily and weekly reviews, work-session history, focus summaries, and task/project analytics
- Human-readable versioned JSON export/import, CSV task export, and relationship validation before import
- Raw database backup/restore with rollback protection and confirmed local-data deletion
- Room migrations from schema version 1 through the current version 11 without destructive fallback

## Platform and privacy

- Production target: Android 8.0/API 26 or newer
- Compile target: Android SDK 36 with Build Tools 36.0.0
- Core operation is offline; the application does not request Internet permission
- External maps and websites may require connectivity through their installed applications
- User data is stored locally in the Room/SQLite database

Android permissions currently used:

- Notifications
- Exact alarms; Android 12 and newer users are prompted to grant Alarms & reminders access so short timers are not silently treated as inexact work
- Vibration
- Device-boot reception to restore enabled alarms, reminders, and timers

Manufacturer battery-management policies can still delay background delivery. Life Minder does not claim to bypass Android or device-vendor restrictions.

## Reviewer walkthrough

The five primary destinations are **Today**, **Do Now**, **Tasks**, **Plans**, and **Timers**. Additional destinations such as Upcoming, Trips, Reviews, life items, and Settings are available from the application menu.

The global `+` action opens a compact quick-add menu without changing the five-tab navigation. Timers uses internal Timers/Alarms segmentation rather than a sixth tab.

For a focused review:

1. Grant notification access if reviewing ordinary task/review reminders, and grant **Alarms & reminders** access when prompted for exact alarms and short countdown timers.
2. Create a task with a due date, add it to Today, and tap its Today card to return to the task editor.
3. Adjust Do Now's time slider and effort control to verify recommendation filtering and ranking.
4. Start a named or unnamed one-minute timer. At completion, the app-owned screen should sound and vibrate; Acknowledge removes the completed standalone timer.
5. Create a repeating alarm and use Snooze or Acknowledge on the app-owned alarm screen. Acknowledgment should retain the alarm and advance its stored occurrence.
6. Create a trip, add a linked task and a place, then tap the place to edit it or mark it complete. Completed and already-ended entries should leave the current/upcoming trip view.
7. Enable dark mode in Settings and inspect Today, Timers, Plans, and trip cards for readable theme-aware contrast.

## Requirements

- Android Studio with Android SDK Platform 36
- Android Studio's bundled JDK 21
- Android emulator or physical device running API 26 or newer

The project produces Java 17-compatible Android bytecode. On this development machine Android Studio and its JDK are installed on `D:`, but the project does not require that location.

## Open and run in Android Studio

1. Open this repository directory in Android Studio.
2. Allow Gradle synchronization to finish.
3. If prompted for a Gradle JDK, select Android Studio's bundled JDK 21.
4. Select the `androidApp` run configuration.
5. Choose an emulator or connected Android device and press Run.

After changing source code, Android Studio normally rebuilds and reinstalls the application when Run is pressed. The emulator does not need to be shut down between builds.

## Command-line development

With a suitable JDK and Android SDK configured:

```powershell
.\gradlew.bat :shared:desktopTest --no-daemon --no-configuration-cache
.\gradlew.bat :shared:connectedDebugAndroidTest --no-daemon --no-configuration-cache
.\gradlew.bat :androidApp:lintDebug --no-daemon --no-configuration-cache
.\gradlew.bat :androidApp:assembleDebug --no-daemon --no-configuration-cache
.\gradlew.bat :androidApp:assembleRelease --no-daemon --no-configuration-cache
.\gradlew.bat spotlessCheck --no-daemon --no-configuration-cache
```

Room schema generation for the desktop and Android targets should be run sequentially. Running both KSP targets concurrently can make them compete for the same generated schema file. Formatting and release compilation should also be run separately if Gradle reports a temporary build-directory race.

APK outputs:

- Debug: `androidApp/build/outputs/apk/debug/androidApp-debug.apk`
- Release: `androidApp/build/outputs/apk/release/androidApp-release.apk`

## Architecture

- `androidApp` contains Android packaging, the activity, notifications, alarms, boot recovery, file operations, and external application actions.
- `shared` contains the Compose UI, Room database, repositories, domain services, scheduling calculations, unified timeline, portability logic, and shared tests.
- `shared/schemas` contains the exported Room schema history used for migration verification.
- `docs` contains product requirements, architecture decisions, milestone records, privacy/release notes, and manual verification checklists.

Existing tasks remain in the original task model so Do Now recommendations and project behavior are preserved. Reminders, routines, medications, events, alarms, timers, trips, and itinerary entries use specialized records and are normalized by `UnifiedTimelineService` for combined Today and Upcoming displays.

The user-facing name is Life Minder. The existing Android application ID, Kotlin package namespace, and database filename retain their original `com.geepscontrol` identity to preserve upgrade and user-data compatibility.

## Current design status

Milestones 0–6 are implemented. Milestones 7 and 8 have working architecture and primary workflows, but the product should still be treated as pre-release while its interaction design is reconsidered.

Areas deliberately open for revision include:

- The primary navigation is Today, Do Now, Tasks, Plans, and Timers; the internal legacy `INBOX` identifier is displayed as Unassigned for data compatibility
- Tasks default to no project, and project selection appears only when requested
- Task priority and effort use five-level sliders; Do Now uses a five-minute-step availability slider; project and task colors provide visual scanning cues
- Plans displays project and trip cards together; adding asks for a project or trip, while an open trip emphasizes linked tasks and keeps travel details collapsible
- The global add workflow and density of item editors
- Today, Do Now, Upcoming, and timeline presentation continue to be refined, although their primary workflows are functional
- Terminology and grouping across tasks, routines, reminders, alarms, and events
- Some advanced editing and correction workflows across reminders, routines, medications, and events
- Advanced itinerary ordering and trip-only import/export exposure in the UI
- Overall visual design, spacing, typography, and one-handed usability

See [product requirements](docs/product-requirements.md), [architecture](docs/architecture.md), [alarm/timer refinement notes](docs/alarm-timer-refinement.md), [milestone status](docs/milestones.md), [release readiness](docs/release-readiness.md), [privacy](PRIVACY.md), and the [changelog](CHANGELOG.md) for more detail.

## Development principles

- Preserve existing user data and provide a migration for every schema change.
- Keep scheduling, recurrence, medication, timeline, and conflict rules outside UI widgets.
- Keep alarms distinct from ordinary notifications and standalone timers distinct from productivity sessions.
- Reuse the existing task model for trip preparation and follow-up tasks.
- Validate imports before modifying local data and never silently discard unsupported records.
- Do not add cloud services, accounts, telemetry, advertising, or network requirements without an explicit product decision.
