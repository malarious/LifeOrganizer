package com.geepscontrol.core.domain

enum class AppDestination(
    val title: String,
    val symbol: String,
    val primary: Boolean,
) {
    TODAY("Today", "☀", true),
    DO_NOW("Do Now", "▶", true),
    UPCOMING("Upcoming", "→", false),
    INBOX("Tasks", "□", true),
    PROJECTS("Plans", "▦", true),
    TIMERS("Timers", "◷", true),
    LIFE_ITEMS("Life Items", "+", false),
    TRIPS("Trips", "✈", false),
    REVIEW("Review", "✓", false),
    SETTINGS("Settings", "⚙", false),
}
