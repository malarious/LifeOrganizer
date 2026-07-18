# Life Minder Architecture

## Decision

Life Minder uses Kotlin with a shared Kotlin Multiplatform foundation and a native Android application entry point. Compose Multiplatform supplies the shared declarative UI. Android is the production target; the desktop JVM target is currently a test host.

This structure shares product behavior without hiding platform differences. Android alarms, notifications, permissions, boot handling, and foreground services will remain in Android-specific source code behind small interfaces.

## Modules

- `androidApp`: Android manifest, activity, application packaging, and future Android system integrations.
- `shared`: common UI, domain logic, repositories, Room schema, settings contracts, and tests.
- `docs`: product source of truth, architecture decisions, and milestone status.

## Dependency direction

```text
Android entry point -> shared UI -> domain/use cases -> repository contracts
                                             ^             |
                                             |             v
                                  platform adapters <- Room database
```

UI code must not call Android alarm or database APIs directly. Business rules must remain deterministic and testable without an Android device.

## Persistence

Room/SQLite is the primary local store. Schema export is enabled and every released schema change requires a migration. Production builds must never use destructive migration fallback. The initial version contains only a foundation metadata table; milestone-specific entities begin in Milestone 1.

Large attachments will live outside SQLite and be referenced by URI. Backups and exports will operate on a consistent database snapshot rather than synchronizing an open database file.

Database version 7 composes separate reminder, routine/occurrence, medication/occurrence, event, standalone alarm, timer-preset, standalone-timer, and timer-history records. Version 8 composes trips, itinerary entries, transport/lodging details, reference cards, and a task association table. Version 11 adds live alarm profiles, alarm snooze-session state, and favorite/ordered timer-preset metadata. Existing tasks remain authoritative and are projected with these records by `UnifiedTimelineService`; the recommendation engine is unchanged.

Database version 9 adds a persistent project color. The automatic 8-to-9 migration assigns a safe default to existing projects, while new and imported projects retain the user's palette selection.

Travel timestamps are epoch milliseconds paired with explicit IANA time-zone identifiers, preserving both the instant and its local display context. JSON format 3 includes the new records and remaps relationships on import. Ordinary JSON redacts sensitive reference content and lodging access codes; raw database backup is the explicit full backup. Binary attachments are deferred until a safe bundled archive format exists.

## State management

Screens will use immutable state exposed through `StateFlow`. Events move from composables to a ViewModel or presentation model, then to a use case or repository. Coroutines perform asynchronous work. Dependency construction remains explicit until manual wiring becomes materially burdensome.

## Alarm and notification lifecycle

Every system alert will use a stable ID stored with its domain reminder. A single coordinator will own scheduling and cancellation.

Pending system work is canceled when an associated task or reminder is completed, canceled, disabled, or rescheduled. Standalone alarms remain distinct from ordinary reminders. Multiple standalone timers use stable per-timer alarm IDs. Boot/package-update handling restores task alarms, standalone alarms, reminders, productivity timers, and standalone timers.

Exact user-requested alarms use `AlarmManager`; without exact-alarm access Android falls back to an inexact while-idle alarm. Alarm audio uses `USAGE_ALARM`. The manifest declares notification, exact-alarm, boot, and vibration permissions. Manufacturer battery management may still delay delivery.

## Error handling and privacy

The core depends on an `ErrorLogger` interface. No telemetry, remote logging, advertising, account, or network service is permitted for core functionality. User-readable failures should explain whether data was changed and offer a recovery action.

## Architecture risks

- Exact alarms and full-screen intents are governed by Android permissions and store policy.
- Manufacturer battery management can delay improperly configured background work.
- Android and Windows notification behavior cannot be made identical.
- Recurrence, snooze, rescheduling, and completion must be transactional to prevent duplicate alerts.
- Alarm profiles are live references: editing a custom profile changes future sessions for alarms using it. Built-ins are restored from stable keys and copied before customization. Active alarms queue in one app-owned activity so bundled audio does not overlap.
- Shared UI must acquire desktop-specific navigation and keyboard affordances in Milestone 7.
