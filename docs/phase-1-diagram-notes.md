# Bricks Phase 1 Diagram Notes

These files are planning artifacts for Phase 1 of the Bricks Android project.

They are not generated Kotlin source files and should not be treated as implementation code yet.

## Package Root

Current project package root:

```text
com.pck.bricks
```

## Diagram Files

### 00-package-architecture.puml

Shows the intended modular package layout for the app.

Main areas:

- core
- data
- features
- future.widget

This diagram is meant to prevent the project from becoming a monolith.

### 01-use-case-diagram.puml

Shows the main user/system interactions:

- create habit
- view library
- open habit
- complete tasks
- earn brick
- view wall
- fortify habit
- delete habit
- receive reminder
- receive missed-day notification

Actors:

- User
- Android System Scheduler / Notification System

### 02-class-diagram.puml

Shows the conceptual domain/service UML.

Core domain models:

- Habit
- HabitTask
- HabitProgress
- HabitDayRecord
- TaskCompletionRecord

Core services:

- HabitRepository
- ReminderScheduler
- DailyRolloverProcessor
- MissedDayPolicyEngine
- TierTransitionEngine
- WallRenderer
- ImageWallMapper

### 03-erd-room-schema.puml

Shows the planned Room database schema.

Planned tables:

- HabitEntity
- HabitTaskEntity
- HabitProgressEntity
- HabitDayRecordEntity
- TaskCompletionEntity

Important relationships:

- One habit has many tasks.
- One habit has one progress record.
- One habit has many day records.
- One task has many completion records.

### 04-user-flow-diagram.puml

Shows the main app flow from launch to library, habit creation, task completion, wall display, fortify, and deletion.

### 05-habit-state-machine.puml

Shows strict habit tier transitions.

States include:

- Active Bronze
- Bronze Completed
- Active Silver
- Silver Completed
- Active Gold
- Gold Completed
- Reset Bronze
- Reverted Silver
- Deleted

This is one of the most important diagrams because the missed-day rules are tier-specific.

### 06-completion-sequence.puml

Shows what happens when a user checks all tasks for a scheduled day.

Key rule:

> A brick is earned only when all tasks are completed during the scheduled local calendar day.

### 07-rollover-sequence.puml

Shows midnight and app-launch missed-day processing.

Important rule:

> The app must reconcile missed scheduled days on launch because Android background work may be delayed.
