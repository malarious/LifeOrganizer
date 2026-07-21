package com.geepscontrol.app

object AlarmSoundCatalog {
    private val paths = mapOf(
        "battle_winter" to "audio/music/battle_in_the_winter.mp3",
        "happy" to "audio/music/happy.mp3",
        "high_tech_lab" to "audio/music/high_tech_lab.flac",
        "epic_boss" to "audio/music/epic_boss_battle.wav",
        "n_dimensions" to "audio/music/n_dimensions.mp3",
    )

    private val legacyAliases = mapOf(
        "gentle_pizzicato" to "high_tech_lab",
        "bright_pizzicato" to "happy",
        "standard_steel" to "n_dimensions",
        "calm_steel" to "high_tech_lab",
        "retro_rise" to "battle_winter",
        "retro_alert" to "epic_boss",
        "sax_wakeup" to "battle_winter",
        "short_hit" to "happy",
        "timer_hit" to "happy",
    )

    fun assetPath(id: String?) = paths[legacyAliases[id] ?: id]
}
