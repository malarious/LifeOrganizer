# Life Minder

Life Minder is a local-first Android organizer for tasks, projects, daily planning, reminders, routines, medications, events, alarms, timers, reviews, and trips, with an integrated offline Pixel Studio for sprite creation.

Built collaboratively with **Codex using GPT-5.6** as part of **OpenAI Build Week**.

The installed app does not depend on AI, an account, a subscription, telemetry, advertising, or a cloud service. Its core features work offline and store user data locally.

---

## Why We Built It

Many productivity tools divide everyday life among separate task, calendar, reminder, timer, and travel apps. Life Minder brings those responsibilities into one timeline so users can see what needs attention now without keeping the entire plan in their heads.

The goal is a practical personal organizer: one place for immediate responsibilities, longer-term projects, recurring routines, time-sensitive alerts, and travel plans.

---

## Features

- Tasks, projects, checklists, priorities, dependencies, tags, recurrence, and search
- Today timeline with current, upcoming, earlier, overdue, and all-day items
- **Do Now** recommendations based on time, energy, urgency, priority, and task dependencies
- Reminders and routines with completion, snooze, skip, pause, and history
- Medication schedules and occurrence records
- Read-only display of events from user-selected device calendars
- Calendar privacy mode that censors calendar names and account addresses while retaining event titles
- Standalone countdown timers, reusable presets, recent timers, and a stopwatch
- Clock alarms with recurrence, snooze limits, volume ramps, vibration modes, bundled music, and dismissal challenges
- One-tap alarm acknowledgment followed by optional Weather today: current conditions, daily rain/high/low, hourly and seven-day forecasts, approximate-location or city search, °C/°F, and offline cached forecasts
- Daily and weekly reviews, work-session history, and productivity summaries
- Offline trip planning with itinerary entries, transportation, lodging, places, linked tasks, and conflict warnings
- Offline Pixel Studio with smart resizing, image pixelification, palettes, drawing tools, layers, animation frames, rotations, and sprite-sheet export
- JSON import/export, CSV task export, and local database backup and restore
- Light and dark themes

Life Minder's medication features are organizational aids only and do not provide medical advice.

---

## How Codex and GPT-5.6 Were Used

Life Minder was created through an iterative collaboration between the product owner and Codex using GPT-5.6.

The product owner:

- Defined the goals, workflows, priorities, and desired behavior
- Tested builds on a real Android phone through USB debugging and device mirroring
- Supplied screenshots and detailed feedback about layout, readability, and behavior
- Exercised features with real device services such as calendars, alarms, audio, and notifications
- Reviewed each iteration and directed the next changes

Codex:

- Inspected and extended the application architecture
- Implemented features and UI changes from that feedback
- Diagnosed bugs using source inspection, builds, tests, and the connected Android device
- Refactored code while preserving existing data and behavior
- Generated and integrated application resources
- Built, tested, installed, and verified development versions
- Maintained project documentation

The working loop was straightforward: the product owner tested the app and explained what worked or felt wrong; Codex implemented and verified a change; then the result was tested again. Codex acted as an engineering collaborator, not as a one-time code generator.

GPT-5.6 is part of the development story, not a runtime dependency. Life Minder does not currently send user data to an AI service or offer in-app generative-AI features.

---

## Technology

- **Platform:** Android 8.0/API 26 or newer
- **Language:** Kotlin
- **UI:** Compose Multiplatform and Material 3
- **Architecture:** Kotlin Multiplatform shared module with an Android application module
- **Storage:** Room with bundled SQLite; schema migrations preserve local data
- **Concurrency:** Kotlin Coroutines and Flow
- **Build:** Gradle, Android Gradle Plugin, KSP, and JDK 21 producing Java 17-compatible bytecode
- **Android integrations:** Calendar Provider, notifications, exact alarms, boot recovery, vibration, NFC, maps, browser, dialer, and clipboard
- **AI during development:** Codex using GPT-5.6

Organizer features work offline. Optional Weather today uses Android Internet permission and, only when requested, approximate foreground location. It connects to Open-Meteo after the user enables weather or searches for a city. See [PRIVACY.md](PRIVACY.md) for the data shared and how to turn it off.

---

## Installation and Development

### Requirements

- Android Studio
- Android SDK Platform 36 and Build Tools 36.0.0
- Android Studio's bundled JDK 21
- An emulator or physical Android device running API 26 or newer

Clone the repository:

```bash
git clone <repository-url>
cd LifeOrganizer
```

Open the repository in Android Studio, allow Gradle synchronization to finish, select the `androidApp` run configuration, and run it on an emulator or connected device.

From PowerShell, a debug build can be created with:

```powershell
.\gradlew.bat :androidApp:assembleDebug
```

Run the shared tests with:

```powershell
.\gradlew.bat :shared:desktopTest
```

The debug APK is written to:

```text
androidApp/build/outputs/apk/debug/androidApp-debug.apk
```

---

## Usage

1. Launch Life Minder and complete the short setup flow.
2. Add tasks directly or organize them into projects and trips.
3. Use Today to review scheduled, current, all-day, and overdue items.
4. Use Do Now when you want a recommendation based on your available time and energy.
5. Create reminders, routines, alarms, or timers for time-sensitive responsibilities.
6. Optionally grant read-only calendar access and choose which device calendars appear in Today and Upcoming.
7. Use Reviews to record progress and adjust upcoming priorities.

Notification, exact-alarm, calendar, and NFC permissions are requested only for features that need them. Manufacturer battery-management policies can still affect background delivery.

---

## Project Structure

```text
LifeOrganizer/
|-- androidApp/              Android packaging and platform integrations
|   `-- src/main/
|       |-- kotlin/          Activity, alarms, notifications, audio, and device actions
|       |-- res/             Android resources and launcher icons
|       `-- assets/          Bundled alarm music
|-- shared/                  Shared Compose UI, domain logic, Room data, and tests
|   |-- schemas/             Exported Room schema history
|   `-- src/
|       |-- commonMain/      UI, repositories, models, and services
|       |-- commonTest/      Shared tests
|       |-- androidMain/     Android-specific shared implementations
|       `-- desktopMain/     Desktop test implementations
|-- docs/                    Requirements, architecture, milestones, and verification notes
|-- Audio/                   Source audio used for bundled alarm tracks
|-- PRIVACY.md               Privacy details
`-- CHANGELOG.md             Project change history
```

The `androidApp` module owns Android-specific behavior. The `shared` module contains the Compose UI, database, repositories, scheduling rules, unified timeline, portability logic, and most tests.

---

## Privacy

- Core operation is offline.
- User data is stored locally in Room/SQLite.
- No account or cloud synchronization is required.
- No telemetry or advertising SDK is included.
- Calendar access is read-only and limited in the UI to calendars selected by the user.
- Recording privacy mode hides calendar names and account addresses without hiding test event titles.
- Optional weather sends rounded coordinates or explicit city-search text to Open-Meteo; organizer records are never included.

See [PRIVACY.md](PRIVACY.md) for additional details.

---

## Future Work

- Continued accessibility, typography, spacing, and one-handed-use refinement
- Broader automated and on-device testing across Android versions and manufacturers
- More advanced calendar presentation and refresh diagnostics
- Additional correction and editing workflows for recurring life items
- Safer archive support for trip attachments
- Release signing, store packaging, and final pre-release hardening

Cloud services, accounts, telemetry, and runtime AI are not assumed future features; adding any of them would require an explicit product and privacy decision.

---

## License

No open-source license has been selected or added yet. Until that changes, the repository should not be treated as granting permission to copy, modify, or redistribute the code.

---

## Acknowledgements

Created for **OpenAI Build Week** through a hands-on collaboration between the product owner and **Codex using GPT-5.6**.

For deeper implementation details, see the [product requirements](docs/product-requirements.md), [architecture notes](docs/architecture.md), [milestone status](docs/milestones.md), [release-readiness notes](docs/release-readiness.md), and [changelog](CHANGELOG.md).
