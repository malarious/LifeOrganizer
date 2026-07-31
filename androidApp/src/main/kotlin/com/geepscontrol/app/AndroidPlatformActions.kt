package com.geepscontrol.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.geepscontrol.core.domain.PlatformActions
import com.geepscontrol.core.domain.DeviceCalendar
import com.geepscontrol.core.domain.DeviceCalendarEvent
import android.Manifest
import android.content.pm.PackageManager
import android.provider.CalendarContract
import android.app.AlarmManager
import androidx.core.content.ContextCompat

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {
    private var previewPlayer: MediaPlayer? = null
    override fun currentTimezoneId(): String = java.util.TimeZone.getDefault().id

    override fun localMinutesOfDay(epochMillis: Long): Int =
        java.time.Instant
            .ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .let { it.hour * 60 + it.minute }

    override fun localDateEpochMillis(epochMillis: Long): Long =
        java.time.Instant
            .ofEpochMilli(epochMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .atStartOfDay(java.time.ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()

    override fun localDateTimeToEpoch(
        dateEpochMillis: Long,
        hour: Int,
        minute: Int,
    ): Long {
        val date =
            java.time.Instant
                .ofEpochMilli(dateEpochMillis)
                .atZone(java.time.ZoneOffset.UTC)
                .toLocalDate()
        return date
            .atTime(hour.coerceIn(0, 23), minute.coerceIn(0, 59))
            .atZone(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
    }

    override fun openMap(address: String) = open(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=${Uri.encode(address)}")))

    override fun openWebsite(url: String) = open(Intent(Intent.ACTION_VIEW, Uri.parse(if (url.startsWith("http")) url else "https://$url")))

    override fun call(phone: String) = open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(phone)}")))

    override fun copy(
        label: String,
        value: String,
    ) {
        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(label, value))
    }

    override fun previewAlarmSound(soundId: String): Boolean {
        stopAlarmSoundPreview()
        val path = AlarmSoundCatalog.assetPath(soundId) ?: return false
        return runCatching {
            previewPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).build())
                context.assets.openFd(path).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) }
                isLooping = false
                prepare()
                setVolume(0.65f, 0.65f)
                setOnCompletionListener { stopAlarmSoundPreview() }
                start()
            }
            true
        }.getOrDefault(false)
    }

    override fun stopAlarmSoundPreview() {
        previewPlayer?.let { runCatching { it.stop() }; it.release() }
        previewPlayer = null
    }

    override fun deviceCalendars(): List<DeviceCalendar> {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val projection = arrayOf(CalendarContract.Calendars._ID, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME, CalendarContract.Calendars.ACCOUNT_NAME)
        return runCatching {
            context.contentResolver.query(CalendarContract.Calendars.CONTENT_URI, projection, "${CalendarContract.Calendars.VISIBLE}=1", null, CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)?.use { cursor ->
                buildList { while (cursor.moveToNext()) add(DeviceCalendar(cursor.getLong(0), cursor.getString(1).orEmpty(), cursor.getString(2).orEmpty())) }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun deviceCalendarEvents(startAt: Long, endAt: Long, calendarIds: Set<Long>): List<DeviceCalendarEvent> {
        if (calendarIds.isEmpty() || ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) return emptyList()
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().also { android.content.ContentUris.appendId(it, startAt); android.content.ContentUris.appendId(it, endAt) }.build()
        val projection = arrayOf(CalendarContract.Instances.EVENT_ID, CalendarContract.Instances.CALENDAR_ID, CalendarContract.Instances.TITLE, CalendarContract.Instances.BEGIN, CalendarContract.Instances.END, CalendarContract.Instances.ALL_DAY, CalendarContract.Instances.EVENT_LOCATION)
        return runCatching {
            context.contentResolver.query(uri, projection, null, null, CalendarContract.Instances.BEGIN)?.use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        val calendarId = cursor.getLong(1)
                        if (calendarId in calendarIds) {
                            add(DeviceCalendarEvent(cursor.getLong(0), calendarId, cursor.getString(2).orEmpty().ifBlank { "Calendar event" }, cursor.getLong(3), cursor.getLong(4), cursor.getInt(5) != 0, cursor.getString(6).orEmpty()))
                        }
                    }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    override fun launchTestAlarm(dismissalMethod: String, difficulty: String): Boolean = runCatching {
        val intent = Intent(context, AlarmActivity::class.java)
            .putExtra(AlarmReceiver.EXTRA_ID, TEST_ALARM_ID)
            .putExtra(AlarmReceiver.EXTRA_LABEL, "Test $dismissalMethod alarm")
            .putExtra(AlarmReceiver.EXTRA_IS_TIMER, true)
            .putExtra(AlarmReceiver.EXTRA_SOUND, true)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION, true)
            .putExtra(AlarmReceiver.EXTRA_SOUND_ID, "n_dimensions")
            .putExtra(AlarmReceiver.EXTRA_LOOP_SOUND, true)
            .putExtra(AlarmReceiver.EXTRA_VIBRATION_MODE, "STANDARD")
            .putExtra(AlarmReceiver.EXTRA_START_VOLUME, 30)
            .putExtra(AlarmReceiver.EXTRA_MAX_VOLUME, 100)
            .putExtra(AlarmReceiver.EXTRA_RAMP_SECONDS, 10)
            .putExtra(AlarmReceiver.EXTRA_DISMISSAL_METHOD, dismissalMethod)
            .putExtra(AlarmReceiver.EXTRA_DISMISSAL_DIFFICULTY, difficulty)
            .putExtra(AlarmReceiver.EXTRA_SNOOZE_AVAILABLE, false)
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP))
        true
    }.getOrDefault(false)

    override fun canScheduleExactAlarms(): Boolean =
        android.os.Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    private fun open(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)

    private companion object {
        const val TEST_ALARM_ID = -99_999L
    }
}
