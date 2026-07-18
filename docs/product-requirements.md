# Life Minder — Product Requirements and Development Plan

## 1. Project Summary

Build a local-first personal productivity application called **Life Minder**.

The application combines:

* Task management
* Project management
* Context-aware task recommendations
* Daily planning
* Alarms and notifications
* Stopwatch and countdown timers
* Pomodoro-style work sessions
* Work-session tracking
* Progress reviews and productivity analytics

The application must work without ChatGPT, Codex, an online API, or a language model after installation.

Codex is being used to develop the software, but the finished application must be fully standalone.

## 2. Primary Product Goal

The application should answer:

> Given my available time, energy, location, equipment, priorities, and deadlines, what should I work on right now?

The application should reduce the cognitive effort required to:

* Remember open tasks
* Decide what matters most
* Choose a task that fits the current situation
* Begin a focused work session
* Track the work performed
* Resume projects after time away
* Recognize neglected projects
* Review progress over time

This is not merely a TODO list. It is a personal execution and work-session system.

---

# 3. Product Principles

## 3.1 Local First

The application must:

* Store its primary data locally
* Work without internet access
* Require no account
* Require no subscription
* Require no remote server
* Require no API key
* Require no language-model download
* Preserve user ownership of all data

## 3.2 Android First

The first production target is Android.

The architecture should avoid unnecessary Android-specific coupling so that a Windows desktop version can be added later.

Initial platform priority:

1. Android
2. Windows
3. Other platforms only if practical later

## 3.3 Fast Capture

Adding a task should take only a few seconds.

The user must be able to quickly record an idea without completing every metadata field.

## 3.4 Progressive Detail

Tasks may begin as simple inbox entries and later receive:

* A project
* Duration estimate
* Energy requirement
* Context
* Deadline
* Priority
* Dependencies
* Recurrence
* Notes

Metadata should be helpful, not mandatory.

## 3.5 Deterministic Recommendations

Task recommendations must use transparent, rule-based scoring.

The application should be able to explain why it recommended a task.

Example:

> Recommended because it fits your available 45 minutes, requires medium energy, is part of a high-priority project, and is due in four days.

## 3.6 Reliable Time Tracking

Timers must use stored timestamps rather than depending on the interface remaining open.

A timer should remain accurate when:

* The screen turns off
* The user opens another application
* The application is backgrounded
* The application is restarted

Where supported, scheduled alarms should be restored after device restart.

---

# 4. Explicitly Out of Scope

Do not implement these during the initial milestones:

* ChatGPT integration
* Codex integration inside the application
* Cloud language-model APIs
* Local language models
* Natural-language task extraction
* Voice assistants
* Email integration
* Calendar integration
* GitHub integration
* Collaborative task sharing
* Team workspaces
* Social features
* Complex gamification
* Paid subscription systems
* Advertising
* Mandatory cloud synchronization

These may be considered later, but the core product must not depend on them.

---

# 5. Recommended Technical Direction

Use a cross-platform application framework capable of producing a proper Android application and later supporting Windows.

Recommended general architecture:

* Cross-platform mobile and desktop UI framework
* Local SQLite database
* Repository/service architecture separating UI and persistence
* Platform notification and alarm integration
* Background-safe timer calculations
* Automated unit and integration tests
* Exportable human-readable data format
* No required backend

Do not hard-code the application around a single database implementation. Use a repository layer so storage and synchronization strategies can change later.

## 5.1 Data Storage

Use SQLite for primary local data.

Store attachments or large files separately and reference them by path or URI.

The database should support migrations so future releases can change the schema without erasing user data.

## 5.2 Backup and Portability

Support:

* Manual database backup
* JSON export
* JSON import
* CSV task export
* Restore from backup
* Automatic local backup rotation later

Do not rely on synchronizing an actively open SQLite database file through Syncthing or a similar file synchronization tool. File-level synchronization can be added only through a safe backup/export workflow.

---

# 6. Main Navigation

The primary navigation should contain:

1. **Do Now**
2. **Today**
3. **Inbox**
4. **Projects**
5. **Timers**
6. **Review**
7. **Settings**

On a phone, use bottom navigation for the most frequent screens and place less frequent screens in a menu if necessary.

---

# 7. Core Features

## 7.1 Inbox and Quick Capture

The Inbox is the default place for unprocessed tasks and ideas.

Quick capture must allow:

* Task title
* Optional note
* Optional project
* Optional due date
* Optional reminder

The minimum required field is the task title.

Quick-capture options:

* Floating add button
* Home-screen add button
* Android share target later
* Android home-screen widget later
* Persistent quick-add notification later

Inbox tasks should remain visible until they are:

* Assigned to a project
* Scheduled
* Completed
* Deleted
* Deliberately left in the inbox

## 7.2 Projects

A project represents a desired outcome requiring multiple actions.

Project fields:

* ID
* Name
* Description
* Desired outcome
* Status
* Priority
* Start date
* Target date
* Area or category
* Color or icon
* Notes
* Created date
* Updated date
* Archived date

Project statuses:

* Active
* Waiting
* Parked
* Completed
* Archived

A project should display:

* Next recommended action
* Open task count
* Completed task count
* Completion percentage
* Last activity date
* Target date
* Recent work sessions
* Project notes
* Blockers
* Tasks without dependencies
* Tasks currently blocked

Example projects:

* PhD Preparation
* Cooling Vest
* OmniDeck
* Japanese Study Application
* Home Repairs
* Japan Travel
* Fitness
* Tactical Grid Engine

## 7.3 Tasks

Task fields:

* ID
* Title
* Description
* Project
* Status
* Priority
* Estimated duration
* Minimum useful duration
* Maximum useful duration
* Energy requirement
* Contexts
* Location
* Equipment
* Due date
* Scheduled date
* Start-after date
* Reminder
* Recurrence rule
* Dependencies
* Tags
* Notes
* Links
* Creation date
* Completion date
* Last worked date
* Actual accumulated time
* Interruption suitability
* Focus requirement
* Importance
* User-defined order

Task statuses:

* Inbox
* Ready
* Scheduled
* In progress
* Waiting
* Blocked
* Completed
* Cancelled
* Archived

Priority levels:

* Critical
* High
* Normal
* Low
* Someday

Energy requirements:

* Very low
* Low
* Medium
* High
* Deep focus

Interruption suitability:

* Easily interruptible
* Moderately interruptible
* Should not be interrupted

## 7.4 Contexts

Contexts describe conditions required to perform a task.

Default contexts:

* Anywhere
* Home
* Work
* Errands
* Computer
* Phone
* Workshop
* Quiet environment
* Internet required
* Offline capable
* Reading
* Writing
* Coding
* Physical activity

Users must be able to add, edit, and delete custom contexts.

A task may have multiple contexts.

## 7.5 Task Dependencies

Tasks may depend on other tasks.

A task is blocked when one or more required dependencies are incomplete.

The recommendation system must not recommend blocked tasks.

The interface should clearly show:

* What is blocking the task
* Which tasks will become available after completion
* Whether a dependency was cancelled

## 7.6 Recurring Tasks

Support recurring tasks such as:

* Daily
* Weekdays
* Weekly
* Monthly
* Custom interval
* Selected days of the week
* After completion
* Fixed calendar recurrence

Completing a recurring task should generate or activate the next occurrence without losing the history of the completed occurrence.

---

# 8. Do Now Recommendation System

## 8.1 User Inputs

The Do Now screen should let the user specify:

* Available time
* Current energy
* Current location
* Available equipment or context
* Desired work mode
* Whether interruptible tasks are required
* Project preference, if any
* Whether overdue tasks should dominate
* Whether the user wants productive, maintenance, or recreational work

Suggested quick time choices:

* 5 minutes
* 10 minutes
* 15 minutes
* 30 minutes
* 45 minutes
* 60 minutes
* 90 minutes
* 2 hours
* Custom

## 8.2 Work Modes

Suggested work modes:

* Important
* Quick wins
* Deep work
* Low energy
* Maintenance
* Neglected projects
* Deadline rescue
* Any productive task

## 8.3 Eligibility Rules

A task is eligible only when:

* Its status is Ready, Scheduled, or In Progress
* Its start-after date has passed
* It is not blocked
* Required contexts are available
* Its minimum useful duration fits the available time
* Its energy requirement does not greatly exceed current energy
* It is not cancelled or archived

## 8.4 Scoring Formula

Use a deterministic weighted score.

Initial conceptual formula:

```text
score =
    priority_score
  + project_priority_score
  + deadline_urgency_score
  + scheduled_today_score
  + duration_match_score
  + energy_match_score
  + context_match_score
  + neglected_task_score
  + neglected_project_score
  + active_session_resume_score
  + user_preference_score
  + dependency_unlock_score
  - context_switch_penalty
  - duration_mismatch_penalty
  - energy_mismatch_penalty
  - recently_deferred_penalty
```

The exact weights should live in a configurable scoring service rather than being scattered through UI code.

## 8.5 Initial Scoring Guidance

Example initial values:

| Factor                         |    Example Effect |
| ------------------------------ | ----------------: |
| Critical priority              |               +50 |
| High priority                  |               +30 |
| Normal priority                |               +15 |
| Low priority                   |                +5 |
| Overdue                        |        +20 to +60 |
| Due today                      |               +35 |
| Due within three days          |               +20 |
| Scheduled today                |               +25 |
| Excellent duration match       |               +20 |
| Acceptable duration match      |               +10 |
| Excellent energy match         |               +15 |
| Required context available     |               +10 |
| Project untouched for 14 days  |               +10 |
| Task untouched for 30 days     |               +10 |
| Completing task unlocks others |               +10 |
| Resume active task             |               +15 |
| Major energy mismatch          |               -40 |
| Duration does not fit          | Ineligible or -50 |
| Missing required context       |        Ineligible |
| Blocked                        |        Ineligible |

These values are starting points, not immutable requirements.

## 8.6 Recommendation Output

Show the best three to five tasks.

Each recommendation should display:

* Task
* Project
* Estimated time
* Energy requirement
* Due date
* Recommendation score
* Brief explanation
* Start button
* Defer button
* Not now button
* View details button

Example:

> **Review Laplace-transform examples**
> 45 minutes · Medium energy · PhD Preparation
> Recommended because it fits your available time, belongs to a high-priority project, and is due soon.

## 8.7 Recommendation Feedback

The user may choose:

* Start
* Not now
* Defer until later
* Wrong duration
* Wrong energy estimate
* Not relevant in this context
* Hide for today

This feedback should adjust the current session and optionally update task metadata.

The system should not silently make permanent changes based on one rejection.

---

# 9. Today Screen

The Today screen is the user’s operational plan.

It should show:

* Scheduled tasks
* Selected focus tasks
* Due tasks
* Overdue tasks
* Active timers
* Completed-today tasks
* Total planned time
* Total completed time
* Available unscheduled time, when entered manually

Sections:

1. Top priorities
2. Scheduled timeline
3. Flexible tasks
4. Overdue
5. Completed today

Features:

* Drag to reorder
* Add task to today
* Remove task from today without deleting it
* Mark top three priorities
* Start timer directly
* Move incomplete task to tomorrow
* Reschedule task
* View total estimated workload
* Warn when planned workload is unrealistic

The application should distinguish:

* Due today
* Scheduled for today
* Chosen for today

These are not the same concept.

---

# 10. Timers and Work Sessions

## 10.1 Timer Modes

Support:

* Stopwatch
* Countdown
* Pomodoro
* Open-ended focus session
* Task-linked timer
* Unlinked general timer
* Alarm

## 10.2 Stopwatch

Features:

* Start
* Pause
* Resume
* Stop
* Record laps or checkpoints
* Attach to a task
* Add notes
* Convert elapsed time into a work session

## 10.3 Countdown

Features:

* Preset durations
* Custom duration
* Pause and resume
* Add time
* Subtract time
* Notification on completion
* Optional vibration
* Optional sound
* Optional repeat
* Continue as overtime stopwatch

## 10.4 Pomodoro

Default configuration:

* 25-minute focus
* 5-minute short break
* 15-minute long break
* Long break after four focus sessions

All values must be configurable.

Options:

* Automatically start breaks
* Automatically start next focus session
* Require confirmation
* Skip break
* Extend session
* Record interrupted sessions

## 10.5 Alarms

Support one-time alarms associated with:

* A task
* A project
* A work session
* A standalone reminder

Alarm requirements:

* Date and time
* Label
* Sound selection
* Vibration option
* Snooze
* Dismiss
* Open associated task
* Repeat options
* Restore scheduled alarms after restart where supported

The application should clearly distinguish:

* A task due date
* A reminder notification
* An alarm requiring explicit dismissal

## 10.6 Persistent Timer State

Store:

* Start timestamp
* Pause periods
* Intended duration
* Task ID
* Session ID
* Timer mode
* Current state

Do not calculate elapsed time only by incrementing an in-memory counter.

## 10.7 Work Sessions

Each work session should record:

* Task
* Project
* Start time
* End time
* Paused duration
* Active duration
* Planned duration
* Timer mode
* Completion status
* Session notes
* Accomplishment summary
* Interruptions
* Energy before
* Energy after
* Focus rating
* Optional mood rating

At session completion, ask:

* What did you accomplish?
* Is the task complete?
* What is the next action?
* Should another session be scheduled?
* Was the duration estimate accurate?

The completion dialog should be quick and skippable.

---

# 11. Notifications

Support notifications for:

* Task reminders
* Due-soon tasks
* Overdue tasks
* Timer completion
* Alarm activation
* Scheduled work sessions
* Daily planning reminder
* Evening review reminder
* Weekly review reminder
* Neglected priority project
* Task waiting for follow-up

Notification categories must be individually configurable.

Include quiet hours.

Avoid excessive notifications by default.

---

# 12. Review and Analytics

## 12.1 Daily Review

Show:

* Tasks completed
* Work sessions completed
* Total focused time
* Planned versus actual time
* Tasks deferred
* Tasks added
* Top project worked on
* Unfinished tasks selected for today

Daily review questions:

* What went well?
* What caused friction?
* What should move to tomorrow?
* What is tomorrow’s most important task?

## 12.2 Weekly Review

Show:

* Time by project
* Tasks completed by project
* Active projects with no progress
* Overdue tasks
* Waiting tasks needing follow-up
* Inbox items needing processing
* Estimated versus actual duration
* Frequently deferred tasks
* Projects without a defined next action
* Tasks completed outside planned priorities

Weekly review workflow:

1. Empty or process inbox
2. Review active projects
3. Review waiting items
4. Review deadlines
5. Identify stalled projects
6. Select next week’s priorities
7. Park projects that are not currently active

## 12.3 Productivity Insights

Initial rule-based insights:

* You consistently underestimate tasks in this category.
* This project has not received a work session in 21 days.
* This task has been deferred five times.
* Your high-energy tasks are rarely completed in the evening.
* Your average 30-minute estimate takes 48 minutes.
* You planned more work than available on four of the last seven days.
* Most completed sessions occur between specific hours.
* A project has no ready next action.

Do not present these as medical, psychological, or absolute conclusions.

## 12.4 Analytics Views

Support:

* Focus time by day
* Focus time by week
* Focus time by project
* Completed tasks by project
* Estimate accuracy
* Session duration distribution
* Deferral count
* Task completion trends
* Project inactivity

Use simple, readable charts.

---

# 13. Search and Filtering

Global search should cover:

* Task titles
* Task descriptions
* Project names
* Notes
* Tags
* Session notes

Task filters:

* Project
* Status
* Priority
* Due date
* Scheduled date
* Duration
* Energy
* Context
* Tag
* Recurrence
* Blocked state
* Completion date

Allow saved filters later.

---

# 14. Settings

Settings should include:

## General

* Theme
* Dark mode
* Time format
* First day of week
* Default task duration
* Default energy level
* Start screen

## Recommendations

* Scoring weights
* Neglect threshold
* Deadline urgency behavior
* Whether overdue tasks dominate
* Default number of recommendations
* Duration flexibility

## Timers

* Pomodoro durations
* Sounds
* Vibration
* Overtime behavior
* Persistent notification
* Keep screen awake option

## Notifications

* Notification categories
* Quiet hours
* Daily planning time
* Evening review time
* Weekly review day and time

## Data

* Export
* Import
* Backup
* Restore
* Clear completed history
* Delete all local data

---

# 15. User Experience Requirements

The application must:

* Be comfortable to operate one-handed
* Avoid small touch targets
* Support dark mode
* Support large text
* Provide clear contrast
* Avoid requiring excessive metadata
* Allow common actions within one or two taps
* Preserve unsaved text when navigating away accidentally
* Confirm destructive actions
* Support undo for common operations
* Provide empty-state guidance
* Avoid modal dialogs for every minor action

The interface should feel like a practical tool, not a corporate project-management platform.

---

# 16. Core Data Model

## Project

```text
Project
- id
- name
- description
- desiredOutcome
- status
- priority
- area
- color
- icon
- startDate
- targetDate
- createdAt
- updatedAt
- archivedAt
```

## Task

```text
Task
- id
- projectId
- parentTaskId
- title
- description
- status
- priority
- importance
- estimatedMinutes
- minimumUsefulMinutes
- maximumUsefulMinutes
- energyRequirement
- location
- scheduledDate
- dueDate
- startAfterDate
- reminderDate
- recurrenceRule
- interruptionSuitability
- focusRequirement
- createdAt
- updatedAt
- startedAt
- completedAt
- lastWorkedAt
- accumulatedMinutes
- manualOrder
```

## Context

```text
Context
- id
- name
- icon
- color
- isSystemDefault
```

## TaskContext

```text
TaskContext
- taskId
- contextId
```

## Tag

```text
Tag
- id
- name
- color
```

## TaskTag

```text
TaskTag
- taskId
- tagId
```

## Dependency

```text
TaskDependency
- taskId
- dependsOnTaskId
- dependencyType
```

## WorkSession

```text
WorkSession
- id
- taskId
- projectId
- mode
- startTime
- endTime
- plannedMinutes
- activeSeconds
- pausedSeconds
- completionState
- accomplishment
- notes
- interruptionCount
- energyBefore
- energyAfter
- focusRating
- createdAt
- updatedAt
```

## Timer

```text
Timer
- id
- taskId
- workSessionId
- mode
- state
- startTimestamp
- intendedDurationSeconds
- accumulatedPausedSeconds
- pauseStartedAt
- sound
- vibrationEnabled
- createdAt
- updatedAt
```

## Reminder

```text
Reminder
- id
- taskId
- projectId
- label
- scheduledTime
- reminderType
- recurrenceRule
- snoozeDuration
- sound
- vibrationEnabled
- enabled
```

## DailyPlan

```text
DailyPlan
- id
- date
- notes
- plannedAvailableMinutes
- createdAt
- updatedAt
```

## DailyPlanTask

```text
DailyPlanTask
- dailyPlanId
- taskId
- order
- priorityRank
- plannedStartTime
```

## RecommendationFeedback

```text
RecommendationFeedback
- id
- taskId
- timestamp
- action
- reason
- availableMinutes
- currentEnergy
- activeContexts
```

---

# 17. Development Milestones

Do not attempt to implement the entire product in one pass.

Complete milestones sequentially.

## Milestone 0 — Repository and Architecture

### Goal

Create a stable project foundation.

### Tasks

* Create the application repository
* Add README
* Add architecture documentation
* Establish folder structure
* Add local database
* Add migration system
* Add repository interfaces
* Add application theme
* Add navigation shell
* Add automated test framework
* Add linting and formatting
* Add development seed-data support
* Add error logging
* Add basic settings storage

### Acceptance Criteria

* Application launches on Android
* Navigation shell works
* Empty database initializes correctly
* Database migration test passes
* Basic automated tests run successfully
* README explains how to build and run the project

---

## Milestone 1 — Projects, Tasks, and Inbox

### Goal

Produce a useful offline task and project manager.

### Tasks

* Create project data model
* Create task data model
* Create context and tag models
* Create task dependency model
* Implement project repository
* Implement task repository
* Implement Inbox screen
* Implement project list
* Implement project detail
* Implement task detail
* Implement create and edit task
* Implement create and edit project
* Implement task completion
* Implement task statuses
* Implement filtering
* Implement search
* Implement quick capture
* Add undo for task completion and deletion
* Add sample seed data

### Acceptance Criteria

* User can create, edit, complete, and delete tasks
* User can create, edit, complete, park, and archive projects
* Tasks can belong to projects
* Tasks can remain in Inbox without a project
* Tasks can have duration, energy, contexts, priority, and due date
* Blocked tasks are visibly marked
* Data persists after application restart
* Core repository and model tests pass

---

## Milestone 2 — Today and Do Now

### Goal

Help the user decide what to work on.

### Tasks

* Implement daily-plan data model
* Implement Today screen
* Add tasks to and remove tasks from Today
* Mark top-three daily priorities
* Implement recommendation eligibility service
* Implement recommendation scoring service
* Implement Do Now input controls
* Display ranked recommendations
* Display recommendation explanations
* Implement Start, Defer, Hide for Today, and Not Now
* Record recommendation feedback
* Add duration-fit calculations
* Add energy-fit calculations
* Add context filtering
* Add project-neglect scoring
* Add deadline urgency scoring
* Add dependency-unlock scoring
* Add scoring settings

### Acceptance Criteria

* User can enter available time and energy
* Blocked or impossible tasks are not recommended
* Recommended tasks fit the selected time and context
* Each recommendation includes an explanation
* Scoring behavior is covered by unit tests
* Today distinguishes due, scheduled, and selected tasks
* User can start a recommended task

---

## Milestone 3 — Timers, Alarms, and Work Sessions

### Goal

Turn selected tasks into trackable work sessions.

### Tasks

* Implement timer data model
* Implement work-session data model
* Implement stopwatch
* Implement countdown
* Implement Pomodoro
* Link timers to tasks
* Add pause and resume
* Add overtime
* Add timer-complete notification
* Add persistent active-timer display
* Restore timer state after app restart
* Implement alarm creation
* Implement snooze and dismiss
* Implement work-session completion dialog
* Record accomplishments and notes
* Update accumulated task time
* Compare estimated and actual duration
* Add session history to tasks and projects

### Acceptance Criteria

* Timers remain accurate while the app is backgrounded
* Active timer survives app restart
* Timer completion produces a notification
* Alarms can be snoozed and dismissed
* Sessions may be linked to tasks or remain unlinked
* Completed sessions update task and project statistics
* Stopwatch, countdown, and Pomodoro tests pass

---

## Milestone 4 — Recurrence and Notifications

### Goal

Support repeated responsibilities and useful reminders.

### Tasks

* Implement recurring tasks
* Implement fixed recurrence
* Implement recurrence-after-completion
* Implement recurring reminders
* Add due-soon notification
* Add overdue notification
* Add daily planning reminder
* Add evening review reminder
* Add weekly review reminder
* Add quiet hours
* Add notification-category settings
* Restore scheduled notifications when required

### Acceptance Criteria

* Completing a recurring task preserves history
* The next occurrence is created correctly
* Notifications respect quiet hours
* Notification categories can be disabled individually
* Recurrence calculations have automated tests

---

## Milestone 5 — Review and Analytics

### Goal

Help the user understand progress and improve estimates.

### Tasks

* Implement daily review
* Implement weekly review
* Add project inactivity detection
* Add deferred-task detection
* Add estimate-versus-actual analytics
* Add time-by-project analytics
* Add completion trends
* Add inbox-processing workflow
* Add projects-without-next-action warning
* Add rule-based insight generator
* Add simple charts
* Add review notes

### Acceptance Criteria

* User can complete daily and weekly reviews
* Application identifies stalled projects
* Application identifies frequently deferred tasks
* Application calculates estimate accuracy
* Application shows focus time by project and date
* Insights explain the underlying data

---

## Milestone 6 — Data Safety and Release Readiness

### Goal

Make the application safe for regular personal use.

### Tasks

* Implement JSON export
* Implement JSON import
* Implement CSV task export
* Implement database backup
* Implement restore workflow
* Add backup validation
* Add schema migration tests
* Add accessibility review
* Add performance review
* Add error-state handling
* Add release build configuration
* Add privacy documentation
* Add user-data deletion workflow
* Add first-run onboarding
* Add sample project templates
* Conduct manual Android testing

### Acceptance Criteria

* Data can be exported and restored
* Import failures do not corrupt existing data
* Application handles thousands of tasks without major slowdown
* Core workflows are usable with large text
* Release build installs successfully
* No network connection is required for core functionality

---

## Milestone 7 — Windows Version

### Goal

Make the same local productivity system available on Windows.

### Tasks

* Adapt navigation for desktop
* Add keyboard shortcuts
* Add system-tray timer
* Add desktop notifications
* Add resizable layouts
* Add compact task-entry window
* Add desktop backup location settings
* Test shared JSON import and export
* Test database migrations on Windows

### Acceptance Criteria

* Windows version supports core task workflows
* Active timer is visible from the system tray
* Keyboard task entry works
* Android exports can be imported on Windows
* Windows exports can be imported on Android

Do not add automatic multi-device synchronization during this milestone.

---

# 18. Suggested Initial Seed Data

Use development seed data representing realistic projects.

## PhD Preparation

* Review Laplace-transform examples
* Review linear algebra fundamentals
* Read the course syllabus
* Create weekly study schedule

## Cooling Vest

* Record pump test temperatures
* Compare tube thickness
* Document battery-runtime test
* Review compressor dimensions

## OmniDeck

* Review general class archetypes
* Update icon positioning
* Evaluate weather and time-of-day fields
* Prepare next prototype changes

## Home Repairs

* Seal patio-house joint
* Clear large weeds
* Organize workshop
* Improve yard grading

## Japanese Study Application

* Review vocabulary data
* Add Kanji Battle prototype
* Test phone layout
* Review progress tracking

This data is for development and demonstration only. It must be removable.

---

# 19. Testing Requirements

## Unit Tests

Test:

* Task scoring
* Deadline urgency
* Duration matching
* Energy matching
* Context eligibility
* Dependency blocking
* Recurrence calculations
* Timer elapsed-time calculations
* Pause calculations
* Estimate accuracy
* Project inactivity
* Import validation
* Database migrations

## Integration Tests

Test:

* Create project and task
* Add task to Today
* Get task recommendation
* Start timer
* Complete session
* Complete task
* Generate recurring task
* Export and import data
* Restart application with active timer

## Manual Tests

Test:

* Background timer behavior
* Screen-off timer behavior
* Notification behavior
* Device-restart alarm restoration
* Dark mode
* Large text
* One-handed task creation
* Database backup and restore

---

# 20. Definition of Done

A feature is complete only when:

* The user-facing workflow works
* Data persists correctly
* Error states are handled
* Automated tests cover important logic
* Accessibility has been considered
* Documentation is updated
* No placeholder behavior remains in the production path
* Existing features continue to pass tests
* The feature works without internet access

---

# 21. Codex Working Instructions

Treat this document as the product source of truth.

Development rules:

1. Work on one milestone at a time.
2. Do not implement future milestones prematurely.
3. Begin by inspecting the repository before changing files.
4. Explain architectural decisions in the project documentation.
5. Keep business logic separate from interface code.
6. Put recommendation scoring in a dedicated testable service.
7. Put timer calculations in a dedicated testable service.
8. Use database migrations for every schema change.
9. Do not erase local data during upgrades.
10. Add tests with each major feature.
11. Avoid unnecessary dependencies.
12. Do not add analytics, telemetry, advertisements, or remote services.
13. Do not add AI or language-model features.
14. Keep all core features usable offline.
15. Maintain a milestone checklist in the repository.
16. Update the changelog as milestones are completed.
17. Stop after the requested milestone and summarize:

    * Files changed
    * Features completed
    * Tests added
    * Tests run
    * Known limitations
    * Recommended next task

---

# 22. First Codex Assignment

Begin with **Milestone 0** only.

Tasks for the first development pass:

1. Select and document the cross-platform architecture.
2. Create the application project.
3. Configure Android as the first target.
4. Establish a clean folder structure.
5. Create the navigation shell.
6. Add the local SQLite database.
7. Add database migrations.
8. Create repository interfaces.
9. Add theme and dark-mode support.
10. Add automated testing, linting, and formatting.
11. Add a README with build and run instructions.
12. Add `docs/product-requirements.md` containing this specification.
13. Add `docs/architecture.md`.
14. Add `docs/milestones.md`.
15. Add a visible placeholder screen for each main navigation destination.
16. Do not implement the complete task-management system yet.

Before coding, provide:

* Proposed framework
* Proposed directory structure
* Database approach
* State-management approach
* Notification strategy
* Testing approach
* Any architectural risks

Then implement Milestone 0 and report the results.
