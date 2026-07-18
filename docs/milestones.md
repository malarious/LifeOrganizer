# Milestones

The full acceptance criteria live in [product-requirements.md](product-requirements.md). Only one milestone is implemented at a time.

## Milestone 0 — Repository and Architecture

- [x] Establish modules and folder structure
- [x] Add README, architecture, milestone tracking, and changelog
- [x] Add Android application entry point
- [x] Add Compose navigation shell
- [x] Add visible placeholders for all seven destinations
- [x] Add Material 3 light and dark themes
- [x] Add Room database version 1 and schema export configuration
- [x] Add repository, settings, and error-logging boundaries
- [x] Add development seed-data setting, disabled by default
- [x] Add and run common unit-test foundation on the JVM target
- [x] Add Spotless/ktlint formatting checks and Android lint configuration
- [x] Generate and commit Room schema after the first successful Android build
- [x] Run build, Android unit tests, lint, and formatting with Android Studio JDK 21 and SDK 36
- [x] Launch on an Android emulator or device

The debug APK builds successfully. Device launch remains open because no emulator or physical device is currently connected.

## Milestone 1 — Projects, Tasks, and Inbox

- [x] Add Room version 2 project, task, context, tag, and dependency tables
- [x] Add nondestructive automatic migration from version 1
- [x] Add project and task repositories
- [x] Add persistent Inbox and quick capture
- [x] Add project list and project detail
- [x] Add create/edit projects and tasks
- [x] Add task completion, deletion, and undo
- [x] Add task/project search and priority filtering
- [x] Add project, priority, energy, duration, and status editing
- [x] Add removable development seed-data service
- [x] Add context and tag management UI and task assignment
- [x] Add dependency editing and visible blocked-state explanations
- [x] Add due-date editor and broader task filtering
- [x] Add dedicated database migration and repository integration tests
- [x] Prevent direct and transitive dependency cycles
- [x] Complete emulator acceptance test for Milestone 1

## Milestone 2 — Today and Do Now — Complete

- Persistent daily plans and Top 3 ranking are stored in Room schema version 3.
- Today supports adding/removing tasks and selecting Top 3 priorities.
- Do Now filters by available time, energy, task state, dependencies, deferral, and visibility.
- Recommendations are ranked with visible reasons and support Start, Done, Later, and Hide feedback.
- Pure recommendation tests cover eligibility and urgency/Top 3 scoring.

- [ ] Next milestone

## Milestone 3 — Timers, Alarms, and Work Sessions — Complete

- Timestamp-derived stopwatch, countdown, Pomodoro, and focus sessions persist through Room.
- Timers support task-linked or general sessions, pause/resume, add-time, overtime, completion summaries, and history.
- Countdown/Pomodoro completion uses a dedicated high-priority Android notification and exact scheduling where permission allows.
- Alarms support advance cancellation, notification snooze/dismiss, exact scheduling, and reboot/package-update restoration.
- Completing a linked task cancels its pending alarms and active timer.
- Project views aggregate focused duration and session count.
- Timer unit tests, migration instrumentation tests, APK assembly, emulator installation, and launch verification pass.

- [ ] Not started

## Milestone 4 — Recurrence and Notifications — Complete

- Tasks support daily, weekly, monthly, and daily-after-completion recurrence from the editor.
- Completing an occurrence preserves it as history and creates a distinct next occurrence with adjusted due/reminder dates.
- Task reminders plus due-soon and due-time notifications use exact Android scheduling when available.
- Daily planning, evening review, and weekly review reminders reschedule after delivery and device restart.
- Notification categories can be individually disabled and optional notifications respect configurable quiet hours; explicit alarms remain exempt.
- Room schema version 5 preserves existing data through automatic migrations.
- Recurrence, quiet-hours, repository-history, migration, desktop, Android build, and emulator tests pass.

- [ ] Not started

## Milestone 5 — Review and Analytics — Complete

- Daily and weekly review workflows summarize tasks, sessions, focused time, planned-versus-actual time, unfinished priorities, additions, and deferrals.
- Reviews persist prompts, priorities, free-form notes, completion time, and history in Room schema version 6.
- Weekly review includes the required inbox/project/waiting/deadline/stalled/priority/parking checklist and direct inbox processing.
- Analytics detect stalled projects, missing next actions, frequent deferral, and estimate variance with evidence-based explanations.
- Readable bars show focus by project, focus trend by date, and completed tasks by project.
- Analytics unit tests, review persistence and migration device tests, desktop tests, Android assembly, and emulator installation pass.

- [ ] Not started

## Milestone 6 — Data Safety and Release Readiness — Complete

- Versioned, human-readable JSON export/import covers projects, tasks and relationships, contexts, tags, sessions, reviews, and alarms.
- Imports preflight format, required fields, enums, duplicate identifiers/names, and every relationship before writes; existing contexts/tags are safely reused.
- CSV task export, atomic file writes, validated raw database backup, rollback-preserving restore, and confirmed local-data deletion are available in Settings.
- First-run onboarding explains local-first operation and offers empty or starter-template setup.
- Privacy documentation confirms no account, network, analytics, or internet permission is required.
- Error states are surfaced in Settings; destructive restore/delete operations require explicit confirmation.
- A 5,000-task test, Android lint, 1.5× font-scale manual check, migration/device tests, release assembly/install, permission audit, launch/crash check, and real export/backup exercise pass.

- [ ] Not started

## Milestone 7 — Unified Time and Life Items

- [x] Room version 7 and distinct reminder, routine, medication, event, alarm, timer preset/timer/history records
- [x] Unified Today projection without replacing tasks or recommendation logic
- [x] Quick creation, occurrence history, actionable controls, multiple timers, Android scheduling and restart restoration
- [x] Configurable scheduling/duration/time zones, daily recurrence, Upcoming filtering, Today sections/conflicts, timer presets/history, and correction controls
- [x] Versioned portability and scheduling/refill/timer tests

## Milestone 8 — Trips and Itineraries

- [x] Room version 8 trips, itinerary, transportation/lodging, references, and existing-task associations
- [x] Explicit time zones, Today integration, overlap detection, offsets, duplication/move domain operations, and offline storage
- [x] Typed entries/references, transport/lodging records, trip-task links, reminders, duplication/moving, overview warnings, and Android map/call/web/copy actions
- [x] Trip-only package export/import service and sensitive-field redaction
- [x] Relationship-safe export/import and sensitive travel-field redaction
- [ ] Binary attachments are intentionally deferred pending a safe bundled archive format
