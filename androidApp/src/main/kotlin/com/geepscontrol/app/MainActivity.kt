package com.geepscontrol.app

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.geepscontrol.core.data.DataPortabilityService
import com.geepscontrol.core.database.buildDatabase
import com.geepscontrol.core.database.ensureDefaultContexts
import com.geepscontrol.core.repository.LifeItemRepository
import com.geepscontrol.core.repository.ensureDefaultAlarmProfiles
import com.geepscontrol.core.repository.RoomContextRepository
import com.geepscontrol.core.repository.RoomPlanningRepository
import com.geepscontrol.core.repository.RoomProjectRepository
import com.geepscontrol.core.repository.RoomReviewRepository
import com.geepscontrol.core.repository.RoomTagRepository
import com.geepscontrol.core.repository.RoomTaskRepository
import com.geepscontrol.core.repository.RoomTimerRepository
import com.geepscontrol.core.settings.AndroidAppSettings
import com.geepscontrol.ui.LifeMinderApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    companion object { const val EXTRA_OPEN_TODAY = "open_today" }
    private val activityScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_OPEN_TODAY, false)) recreate()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val database = buildDatabase(applicationContext)
        val projectRepository = RoomProjectRepository(database.projectDao())
        val taskRepository = RoomTaskRepository(database.taskDao())
        val contextRepository = RoomContextRepository(database.contextDao())
        val tagRepository = RoomTagRepository(database.tagDao())
        val planningRepository = RoomPlanningRepository(database.planningDao())
        val timerRepository = RoomTimerRepository(database.timerDao())
        val lifeItemRepository = LifeItemRepository(database.lifeItemDao())
        val alarmScheduler = AndroidAlarmScheduler(applicationContext)
        val appSettings = AndroidAppSettings(applicationContext)
        val reviewRepository = RoomReviewRepository(database.reviewDao())
        val dataPortability = DataPortabilityService(projectRepository, taskRepository, contextRepository, tagRepository, timerRepository, reviewRepository, lifeItemRepository)
        val dataFiles = AndroidDataFileGateway(applicationContext, database)
        createChannels(applicationContext)
        scheduleRoutineReminders(applicationContext, alarmScheduler)
        if (android.os.Build.VERSION.SDK_INT >= 31 && !getSystemService(AlarmManager::class.java).canScheduleExactAlarms()) {
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                    .setData(Uri.parse("package:$packageName")),
            )
        }
        if (android.os.Build.VERSION.SDK_INT >= 33) registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.POST_NOTIFICATIONS)
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}.launch(Manifest.permission.READ_CALENDAR)
        activityScope.launch {
            ensureDefaultContexts(contextRepository)
            ensureDefaultAlarmProfiles(lifeItemRepository, System.currentTimeMillis())
        }
        setContent {
            LifeMinderApp(
                projectRepository = projectRepository,
                taskRepository = taskRepository,
                contextRepository = contextRepository,
                tagRepository = tagRepository,
                planningRepository = planningRepository,
                timerRepository = timerRepository,
                lifeItemRepository = lifeItemRepository,
                alarmScheduler = alarmScheduler,
                appSettings = appSettings,
                reviewRepository = reviewRepository,
                dataPortability = dataPortability,
                dataFiles = dataFiles,
                platformActions = AndroidPlatformActions(applicationContext),
            )
        }
    }

    override fun onDestroy() {
        activityScope.cancel()
        super.onDestroy()
    }
}
