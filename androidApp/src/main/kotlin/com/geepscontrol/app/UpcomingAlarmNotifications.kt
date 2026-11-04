package com.geepscontrol.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.domain.AlarmDefinition
import com.geepscontrol.core.domain.AlarmOccurrenceEngine
import java.text.DateFormat
import java.util.Date

/** Quiet, independent controls for each clock alarm due in the next two hours. */
object UpcomingAlarmNotifications {
    private const val CHANNEL = "upcoming_alarms"
    private const val TAG = "upcoming_alarm"
    private const val WINDOW = 2 * 60 * 60_000L
    const val ACTION_SHOW = "com.geepscontrol.UPCOMING_ALARM"
    const val ACTION_SKIP = "com.geepscontrol.SKIP_ALARM"
    const val ACTION_DELAY = "com.geepscontrol.DELAY_ALARM"
    const val EXTRA_SCHEDULED_AT = "scheduled_at"

    fun schedule(
        context: Context,
        alarm: AlarmDefinition,
    ) {
        if (alarm.id !in AlarmReceiver.STANDALONE_ALARM_OFFSET until 4_000_000L) return
        val now = System.currentTimeMillis()
        if (!alarm.enabled || alarm.triggerAt <= now) return
        val intent =
            Intent(context, AlarmReceiver::class.java)
                .setAction(ACTION_SHOW)
                .putExtra(AlarmReceiver.EXTRA_ID, alarm.id)
                .putExtra(EXTRA_SCHEDULED_AT, alarm.triggerAt)
        val operation = PendingIntent.getBroadcast(context, alarm.id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val manager = context.getSystemService(AlarmManager::class.java)
        manager.cancel(operation)
        if (alarm.triggerAt - now <= WINDOW) {
            context.sendBroadcast(intent)
        } else {
            // The heads-up controls are advisory; only the ringing alarm needs exact delivery.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAt - WINDOW, operation)
            NotificationManagerCompat.from(context).cancel(TAG, alarm.id.toInt())
        }
    }

    fun cancel(
        context: Context,
        id: Long,
    ) {
        val intent = Intent(context, AlarmReceiver::class.java).setAction(ACTION_SHOW)
        val operation = PendingIntent.getBroadcast(context, id.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        context.getSystemService(AlarmManager::class.java).cancel(operation)
        NotificationManagerCompat.from(context).cancel(TAG, id.toInt())
    }

    fun show(
        context: Context,
        alarm: StandaloneAlarmEntity,
        expectedAt: Long,
    ) {
        val now = System.currentTimeMillis()
        if (!alarm.enabled || alarm.scheduledAt != expectedAt || alarm.scheduledAt <= now || alarm.scheduledAt - now > WINDOW) return
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL, "Upcoming alarms", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Skip or delay individual upcoming alarms without changing their repeat schedule"
                setSound(null, null)
                enableVibration(false)
            },
        )
        val id = AlarmReceiver.STANDALONE_ALARM_OFFSET + alarm.id
        val open =
            PendingIntent.getActivity(
                context,
                id.toInt(),
                Intent(context, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_OPEN_ALARMS, true)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        val snoozed = AlarmOccurrenceEngine.isSnoozed(alarm)
        val time = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(alarm.scheduledAt))
        val text = "${if (snoozed) "Snoozed until" else "Upcoming at"} $time · ${if (alarm.recurrenceRule == null) "This alarm only" else "Changes affect this occurrence only"}"
        val notification =
            NotificationCompat
                .Builder(context, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(alarm.label.ifBlank { "Alarm" })
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setContentIntent(open)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setOnlyAlertOnce(true)
                .setSilent(true)
                .setOngoing(true)
                .setTimeoutAfter(alarm.scheduledAt - now)
                .addAction(0, if (snoozed) "Stop snooze" else "Skip", action(context, ACTION_SKIP, id, alarm.scheduledAt))
                .addAction(0, "+5 minutes", action(context, ACTION_DELAY, id, alarm.scheduledAt))
                .build()
        NotificationManagerCompat.from(context).notify(TAG, id.toInt(), notification)
    }

    private fun action(
        context: Context,
        action: String,
        id: Long,
        at: Long,
    ): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, AlarmReceiver::class.java)
                .setAction(action)
                .setData(Uri.parse("lifeminder://alarm/$id/$at/$action"))
                .putExtra(AlarmReceiver.EXTRA_ID, id)
                .putExtra(EXTRA_SCHEDULED_AT, at),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
