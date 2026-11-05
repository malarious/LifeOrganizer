package com.geepscontrol.app

import android.app.NotificationManager
import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.test.platform.app.InstrumentationRegistry
import com.geepscontrol.core.database.StandaloneAlarmEntity
import com.geepscontrol.core.database.buildDatabase
import com.geepscontrol.core.repository.LifeItemRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlarmWeatherFlowTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext

    @Test fun oneTapAcknowledgeBypassesOptionalChallengeAndOpensWeather() =
        runBlocking {
            withAlarm { id, repository ->
                val monitor = instrumentation.addMonitor(WeatherActivity::class.java.name, null, false)
                var weather: WeatherActivity? = null
                val activity =
                    instrumentation.startActivitySync(
                        Intent(context, AlarmActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            .putExtra(AlarmReceiver.EXTRA_ID, 3_000_000L + id)
                            .putExtra(AlarmReceiver.EXTRA_LABEL, "Acknowledgment integration test")
                            .putExtra(AlarmReceiver.EXTRA_SOUND, false)
                            .putExtra(AlarmReceiver.EXTRA_VIBRATION, false)
                            .putExtra(AlarmReceiver.EXTRA_DISMISSAL_METHOD, "NUMBER"),
                    ) as AlarmActivity
                try {
                    instrumentation.runOnMainSync {
                        val button = findButton(activity.window.decorView, "Acknowledge")
                        assertNotNull(button)
                        button!!.performClick()
                    }
                    weather = instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as? WeatherActivity
                    assertNotNull(weather)
                    assertTrue(weather!!.intent.getBooleanExtra(WeatherActivity.EXTRA_FROM_ALARM, false))
                    withTimeout(5_000) { while (repository.getAlarm(id)?.lastDismissedAt == null) delay(25) }
                    assertFalse(repository.getAlarm(id)!!.enabled)
                } finally {
                    instrumentation.removeMonitor(monitor)
                    instrumentation.runOnMainSync {
                        weather?.finish()
                        if (!activity.isFinishing) activity.finish()
                    }
                }
            }
        }

    @Test fun notificationAcknowledgeStopsAlarmAndOpensWeather() =
        runBlocking {
            instrumentation.uiAutomation.executeShellCommand("pm grant ${context.packageName} android.permission.POST_NOTIFICATIONS").use { descriptor ->
                android.os.ParcelFileDescriptor
                    .AutoCloseInputStream(descriptor)
                    .use { it.readBytes() }
            }
            withAlarm { id, repository ->
                val alarm = repository.getAlarm(id)!!
                repository.save(alarm.copy(lastTriggeredAt = null))
                val monitor = instrumentation.addMonitor(WeatherActivity::class.java.name, null, false)
                var weather: WeatherActivity? = null
                val manager = context.getSystemService(NotificationManager::class.java)
                try {
                    context.sendBroadcast(
                        Intent(context, AlarmReceiver::class.java)
                            .setAction(AlarmReceiver.ACTION_FIRE)
                            .putExtra(AlarmReceiver.EXTRA_ID, 3_000_000L + id)
                            .putExtra(UpcomingAlarmNotifications.EXTRA_SCHEDULED_AT, alarm.scheduledAt),
                    )
                    val notification =
                        withTimeout(5_000) {
                            var found = manager.activeNotifications.firstOrNull { it.id == (3_000_000L + id).toInt() && it.tag == null }
                            while (found == null) {
                                delay(25)
                                found = manager.activeNotifications.firstOrNull { it.id == (3_000_000L + id).toInt() && it.tag == null }
                            }
                            found.notification
                        }
                    notification.actions
                        .first { it.title.toString() == "Acknowledge" }
                        .actionIntent
                        .send()
                    weather = instrumentation.waitForMonitorWithTimeout(monitor, 5_000) as? WeatherActivity
                    assertNotNull(weather)
                    withTimeout(5_000) { while (repository.getAlarm(id)?.lastDismissedAt == null) delay(25) }
                    assertFalse(repository.getAlarm(id)!!.enabled)
                } finally {
                    instrumentation.removeMonitor(monitor)
                    instrumentation.runOnMainSync { weather?.finish() }
                }
            }
        }

    private suspend fun withAlarm(block: suspend (Long, LifeItemRepository) -> Unit) {
        val weather = AndroidWeatherRepository(context)
        val previousEnabled = weather.enabled
        val previousAfter = weather.afterAlarm
        weather.enabled = false // The UI test must not request a real location or make a network call.
        weather.afterAlarm = true
        val database = buildDatabase(context)
        val repository = LifeItemRepository(database.lifeItemDao())
        val now = System.currentTimeMillis()
        val id = repository.save(StandaloneAlarmEntity(label = "Weather flow test", scheduledAt = now - 1_000, sound = false, vibration = false, lastTriggeredAt = now - 500, createdAt = now, updatedAt = now))
        try {
            block(id, repository)
        } finally {
            AndroidAlarmScheduler(context).cancel(3_000_000L + id)
            repository.removeAlarm(id)
            database.close()
            weather.enabled = previousEnabled
            weather.afterAlarm = previousAfter
        }
    }

    private fun findButton(
        view: View,
        text: String,
    ): Button? {
        if (view is Button && view.text.toString() == text) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) findButton(view.getChildAt(i), text)?.let { return it }
        return null
    }
}
