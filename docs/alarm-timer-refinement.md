# Alarm and timer refinement

## Implemented scope

- Five bottom destinations remain unchanged.
- A stable global quick-add menu routes Task, Timer, Reminder, Event, Routine, Medication, Alarm, Project, Trip, and contextual trip Place actions.
- Timers has internal Timers/Alarms segmentation, one-tap 5/10/15/30/45/60-minute launch, optional names, hours/minutes/seconds custom entry, favorite presets, preset edit/pin/order/duplicate/delete, and recent restart/save actions.
- Schema 11 adds live alarm profiles, alarm snooze counts/profile references, and timer-preset sound/favorite/order fields without changing the application ID or database name.
- Built-in profiles are Gentle, Standard, Heavy Sleeper, Silent Vibration, and Reminder. Hidden compatibility profiles preserve sound-only and fully silent legacy alarms. Built-ins are copied before editing.
- Five MP3, WAV, and FLAC music tracks are packaged under `assets/audio/music` and exposed through friendly names. The former Kenney jingle collection is no longer packaged or selectable.
- Alarm playback uses one `MediaPlayer`, elapsed-time volume interpolation, optional looping, simultaneous or delayed vibration, snooze limits, and an in-memory queue to prevent overlapping alarm audio.
- Standard, Hold, Number, and Trace dismissal are available. Challenge profiles always expose a documented five-second emergency hold; when an accessibility service is active, a standard accessible acknowledgment is also shown.
- Export format 4 includes alarm profiles and validates alarm/profile relationships. Versions 1–3 remain accepted.

## Migration mapping

- Existing sound + vibration alarm → Standard
- Existing vibration-only alarm → Silent Vibration
- Existing sound-only alarm → hidden Legacy Sound Only compatibility profile
- Existing fully silent alarm → hidden Legacy Silent compatibility profile

The mapping runs after the schema migration and only assigns alarms whose `profileId` is null. Existing alarm rows, schedules, recurrence rules, labels, sound flags, and vibration flags are retained.

## Android limitations

- Exact short timers require Android's Alarms & reminders access on Android 12+.
- Direct alarm-screen launch can still be affected by manufacturer background-start and battery policies.
- Application volume percentages are player levels relative to the alarm audio stream; they do not force global system volume or guarantee physical loudness.
- System Do Not Disturb and vibration settings remain authoritative.
- The active-alarm queue is process-local. Scheduled alarms remain in Room/AlarmManager, but queue presentation is reconstructed as alarms fire after process loss.
- Hold and trace behavior needs physical-device TalkBack, magnification, large-text, and small-screen verification.

## Manual device checklist

1. Upgrade an installation containing sound+vibration, vibration-only, sound-only, and silent alarms; verify profile mapping and unchanged schedules.
2. Start quick, custom, preset, unnamed, and simultaneous timers with the app foreground/background, removed from recents, screen off, and device locked.
3. Preview two sounds in sequence and close the editor; confirm only one preview plays and it stops on close.
4. Trigger music plus vibration, delayed vibration, silent vibration, and 0/30/90-second ramps.
5. Exhaust a finite snooze limit and confirm Snooze disappears while dismissal remains.
6. Exercise Standard, Hold, Number, and Trace at each difficulty; test early hold release and incorrect number feedback.
7. Enable TalkBack and verify Accessible acknowledge, labels, large targets, and emergency hold.
8. Trigger two alarms close together and verify sequential presentation without overlapping audio.
9. Reboot and verify enabled alarms and running timers restore.
10. Export format 4, import it as new local data, and verify profile references and preset favorites.
