# Changelog

All notable changes to Life Minder are recorded here.

## Unreleased

- Added an integrated Pixel Studio with image import, centered contain/cover cropping, smart palette-aware resizing, adjustable sprite dimensions, color quantization, optional Floyd–Steinberg dithering, transparency, pencil/eraser/fill/eyedropper tools, brush sizes, grid and zoom controls, undo/redo, editable palettes and color variants, arbitrary and right-angle rotation, flips, generated eight-direction frame sets, animation preview and timing, onion skinning, multi-layer editing, autosaved project management, crisp PNG export, and sprite-sheet export. Pixel projects are included in full local backups.
- Completed a holistic usability and reliability pass: alarms now use alarm-clock scheduling and full-screen alarm notifications; Today uses local calendar days and clearer action-first sections; every Today item opens its own detail view with Edit, Done/Take, and recoverable Delete actions; Tasks adds All/Inbox/Projects/Trips scopes; editors use tap-based date/time pickers with advanced settings tucked away; trips gain dashboard-style sections, typed itinerary colors, visible references, and reusable packing checklists; Android Back now closes the current detail/editor; navigation uses accessible vector icons; and JSON/trip backups now preserve checklists. Added schema migration 13 to 14 and regression tests for local-day, backup, and migration behavior.
- Added default alarm profiles that populate fully detached alarm settings; local-wall-clock alarm intent and time-zone rescheduling; quiet challenge grace periods; Math, Memory, Sorting, barcode-scan, and NFC ending challenges; first acknowledged alarm handoff to Today after queued alarms; and selectable read-only Android calendar events in Today and Upcoming. Added schema migration 12 to 13 and began splitting alarm/profile UI out of the application UI monolith.
- Moved alarm profiles behind dedicated management so alarms remain first, and added profile-as-template alarm creation with per-alarm sound, volume ramp, vibration, snooze limits, and Button/Number/Trace/Hold end conditions. Added nondestructive Room schema migration 11 to 12 for alarm-specific behavior overrides.
- Added a stable global quick-add workflow; internal Timers/Alarms segmentation; one-tap quick timers; hours/minutes/seconds custom timers; favorite presets and recent restart/save actions; schema-11 live alarm profiles; five built-in profiles; 85 packaged Kenney jingles with curated metadata and licensing; sound preview; bundled playback; simultaneous/delayed vibration; elapsed-time volume ramps; snooze limits; queued alarms; and Standard, Hold, Number, and Trace dismissal with accessible emergency fallback.

- Reconciled Today alarms with their current stored dates, made timeline rows navigable, allowed unnamed timers, hid finished trip entries, added Add Place and entry editing, replaced Do Now time presets with a five-minute slider, and moved alarm sound/vibration into the custom alarm screen.
- Added an app-owned full-screen alarm surface with Snooze and Acknowledge controls.
- Routed alarm and timer completion directly to the app-owned screen, requested exact-alarm access to prevent silent timer drift, aligned timer controls, and corrected trip-card contrast in dark mode.

- Repeating-alarm dismissal now advances stored and scheduled time directly to the next valid future occurrence, including across multiple missed intervals.
- Added deletion controls to disabled timers and alarms, removed unexplained Top 1/2/3 controls from Today, and added a persistent dark-mode toggle in Settings.
- Corrected Today timer handling: only running timers appear, their timeline time is their expected completion rather than their start, they are never overdue, and fired one-shot timer records are removed while session history remains available.
- Added alarm date selection defaulting to today, editing by tapping enabled or disabled alarm cards, per-alarm trash actions, wider three-column weekday controls, and the shorter Add Alarm action label.
- Made Edit Project scrollable, converted project status to a compact selector, and substantially enlarged the project color controls.
- Added Lists as a third Plans type for groceries, notes, and other short-term checklists, with naming, quick item entry, check/uncheck behavior, progress summaries, and confirmed cascading deletion.
- Simplified trip-linked task editing by hiding task status and relabeling the optional date action to Set Date.
- Prevented active and completed timers from appearing as overdue items on Today.
- Added nondestructive Room schema migration 9 to 10 for checklists and checklist items.
- Replaced the permanent Quick Timers grid with an always-visible bottom action bar for Stopwatch, Add Timer, and Create Alarm. Add Timer now contains 5/10/15/20/25/30/45/60-minute presets plus numeric custom entry.
- Active alarms now use green-to-red urgency coloring based on how soon they will fire; disabled alarms remain neutral.
- Timer completion now uses a dedicated audible high-importance alarm-sound notification channel.
- Added simultaneous named countdown timers; timer notifications identify what each timer is for, and running/enabled items are grouped above disabled or completed timers and alarms with enable/disable controls.
- Work sessions shorter than one minute are no longer saved, and session history has been removed from the everyday timer controls.
- Simplified the idle timer screen into equal-width Stopwatch/Create Alarm actions, a uniform 5/10/15/20/25/30-minute Quick Timers grid, and one full-width Custom Timer action; removed dedicated Pomodoro and Focus buttons.
- Consolidated Do Now overdue messaging into the recommendation metadata line, where `overdue!` is now red and no longer duplicated above it.
- Rebuilt Timers & Alarms with standard-duration buttons, custom countdowns, millisecond stopwatch display, compact right-side controls, and a Timer-or-Alarm add menu.
- Added real clock alarms with names, time-of-day input, weekday selection, weekly, bi-weekly, monthly, and custom hourly/daily/weekly/monthly repetition. Repeating alarms now reschedule after firing without being disabled when one occurrence is dismissed.
- Task editing can explicitly associate a task with a trip; trip-created tasks retain their link and changing or clearing that trip updates the association.
- Trip detail is now task-and-place focused, removes the duplicate selected-trip card, uses distinct place cards, and moves location type into a compact selector.
- Improved selected task-status rendering, enlarged project color markers, and added red `Overdue!` text to Do Now recommendations.
- Added Timers as a fifth primary tab and kept alarm controls alongside everyday timer controls.
- Plans now combines project and trip cards; its add button asks which plan type to create.
- Trip screens are task-first, with dates, destination time zone, summary, itinerary locations, and references behind an expandable details section.
- The add button inside a trip now offers either a linked task or a new itinerary location.
- Reduced task-status scrolling with a two-column layout, repaired reminder amount/unit presentation, colored effort sliders, and added visible overdue warnings to Today.
- Made Today the default and first primary destination; renamed Inbox to Tasks and Projects to Plans in the user interface.
- Added five-color priority and effort sliders, priority-colored task cards, and user-selectable project colors.
- Added explicit task due dates/times, numeric Android input modes, reminder amount/unit selection, and interval-based repeat controls.
- Tasks now default to no project, reveal project selection on request, and the project-detail add button creates a task in that project.
- Surfaced Trips & itineraries inside Plans and expanded the quick timer presets for breaks, Pomodoro, and longer focus sessions.
- Added Room schema version 9 for persistent project colors with migration and portability support.

- Added Milestones 7 and 8: a unified life timeline, reminders, routines, medications, events, standalone alarms, multiple timers, trips, and itineraries.
- Added Room migrations 6→7 and 7→8, JSON format 3 portability, sensitive travel-field redaction, and restart restoration for new scheduled items.
- Added Upcoming filters, structured Today sections, recurring life-item scheduling, timer presets/history, detailed itinerary/reference workflows, trip-task links, trip-only packages, and Android map/call/web/copy actions.

- Renamed the user-facing product from Geeps Control to Life Minder while preserving the existing Android application ID and database filename for update compatibility.
- Added persistent Today plans, Top 3 selection, and the first complete Do Now recommendation engine with time/energy controls, explanations, deferral, hiding, and feedback history.
- Added the Milestone 3 foundation: persistent timestamp-derived stopwatch/countdown/Pomodoro timers, pause/resume/overtime, work-session history and accomplishment capture, Room-backed alarms, exact Android scheduling, reboot restoration, and alarm notification Snooze/Dismiss actions.
- Added recurring tasks, recurring reminder adjustment, due-soon/due-time notifications, daily/evening/weekly routines, per-category notification switches, and quiet-hours enforcement.
- Added persistent daily and weekly reviews, focus/completion charts, estimate analytics, project inactivity and missing-next-action detection, deferral insights, and review history.
- Added versioned JSON import/export, CSV export, validated database backup/restore with rollback, local-data deletion, onboarding and starter templates, privacy documentation, large-data coverage, and an installable release build.

## Initial foundation (historical)

### Added

- Kotlin and Compose Android project foundation.
- Shared Kotlin Multiplatform module for portable UI and core boundaries.
- Seven-destination navigation shell with Material 3 light/dark theming.
- Room database version 1 with migration/schema-export configuration.
- Repository, settings, and error-logging interfaces.
- Android database and local settings implementations.
- Initial common unit tests.
- Spotless/ktlint formatting verification and Android lint task configuration.
- Product, architecture, milestone, and build documentation.
- Verified Android debug APK build with SDK 36 and Android Studio's JDK 21.
- Generated and committed the Room version 1 schema.
- Verified Android lint, Android/shared unit tests, desktop tests, and formatting checks.
- Room database version 2 with projects, tasks, contexts, tags, joins, and task dependencies.
- Nondestructive Room auto-migration from database version 1 to version 2.
- Persistent Inbox, quick task capture, project list, and project detail screens.
- Project and task create/edit workflows with priority, energy, duration, status, and project assignment.
- Task completion and deletion with undo actions.
- Task/project search and priority filtering.
- Removable development seed-data service.
- Context and tag creation, editing, deletion, and task assignment.
- Task dependency editing with blocked-state explanations and cycle prevention.
- Due-date entry and due/overdue labels.
- On-device Room migration and repository relationship tests.

### Known limitations

- Projects and tasks begin in Milestone 1.
- Timers and alarms begin in Milestone 3.
- No emulator or physical Android device has been connected for the initial launch smoke test.
