package com.geepscontrol.core.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface AppSettings {
    var themeMode: ThemeMode
    var developmentSeedDataEnabled: Boolean
    var taskRemindersEnabled: Boolean
    var dueNotificationsEnabled: Boolean
    var planningNotificationsEnabled: Boolean
    var reviewNotificationsEnabled: Boolean
    var quietHoursEnabled: Boolean
    var quietStartHour: Int
    var quietEndHour: Int
    var onboardingCompleted: Boolean
}
