package com.geepscontrol.core.settings

import android.content.Context

class AndroidAppSettings(
    context: Context,
) : AppSettings {
    private val preferences =
        context.applicationContext
            .getSharedPreferences("geeps-control-settings", Context.MODE_PRIVATE)

    override var themeMode: ThemeMode
        get() =
            runCatching {
                ThemeMode.valueOf(preferences.getString("theme_mode", ThemeMode.SYSTEM.name)!!)
            }.getOrDefault(ThemeMode.SYSTEM)
        set(value) {
            preferences.edit().putString("theme_mode", value.name).apply()
        }

    override var developmentSeedDataEnabled: Boolean
        get() = preferences.getBoolean("development_seed_data", false)
        set(value) {
            preferences.edit().putBoolean("development_seed_data", value).apply()
        }

    override var taskRemindersEnabled: Boolean by booleanPreference("task_reminders", true)
    override var dueNotificationsEnabled: Boolean by booleanPreference("due_notifications", true)
    override var planningNotificationsEnabled: Boolean by booleanPreference("planning_notifications", false)
    override var reviewNotificationsEnabled: Boolean by booleanPreference("review_notifications", false)
    override var quietHoursEnabled: Boolean by booleanPreference("quiet_hours", true)
    override var quietStartHour: Int by intPreference("quiet_start", 22)
    override var quietEndHour: Int by intPreference("quiet_end", 7)
    override var onboardingCompleted: Boolean by booleanPreference("onboarding_completed", false)
    override var calendarPrivacyMode: Boolean by booleanPreference("calendar_privacy_mode", true)
    override var selectedCalendarIds: Set<Long>
        get() = preferences.getStringSet("selected_calendar_ids", emptySet()).orEmpty().mapNotNull(String::toLongOrNull).toSet()
        set(value) { preferences.edit().putStringSet("selected_calendar_ids", value.map(Long::toString).toSet()).apply() }

    private fun booleanPreference(
        key: String,
        default: Boolean,
    ) = object : kotlin.properties.ReadWriteProperty<Any?, Boolean> {
        override fun getValue(
            thisRef: Any?,
            property: kotlin.reflect.KProperty<*>,
        ) = preferences.getBoolean(key, default)

        override fun setValue(
            thisRef: Any?,
            property: kotlin.reflect.KProperty<*>,
            value: Boolean,
        ) {
            preferences.edit().putBoolean(key, value).apply()
        }
    }

    private fun intPreference(
        key: String,
        default: Int,
    ) = object : kotlin.properties.ReadWriteProperty<Any?, Int> {
        override fun getValue(
            thisRef: Any?,
            property: kotlin.reflect.KProperty<*>,
        ) = preferences.getInt(key, default)

        override fun setValue(
            thisRef: Any?,
            property: kotlin.reflect.KProperty<*>,
            value: Int,
        ) {
            preferences.edit().putInt(key, value).apply()
        }
    }
}
