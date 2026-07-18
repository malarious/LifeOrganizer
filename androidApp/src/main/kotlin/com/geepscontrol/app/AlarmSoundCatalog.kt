package com.geepscontrol.app

object AlarmSoundCatalog {
    private val paths = mapOf(
        "gentle_pizzicato" to "audio/Pizzicato jingles/jingles_PIZZI07.ogg",
        "bright_pizzicato" to "audio/Pizzicato jingles/jingles_PIZZI03.ogg",
        "standard_steel" to "audio/Steel jingles/jingles_STEEL07.ogg",
        "calm_steel" to "audio/Steel jingles/jingles_STEEL01.ogg",
        "retro_rise" to "audio/8-Bit jingles/jingles_NES00.ogg",
        "retro_alert" to "audio/8-Bit jingles/jingles_NES13.ogg",
        "sax_wakeup" to "audio/Sax jingles/jingles_SAX07.ogg",
        "short_hit" to "audio/Hit jingles/jingles_HIT00.ogg",
        "timer_hit" to "audio/Hit jingles/jingles_HIT15.ogg",
    )

    fun assetPath(id: String?) = paths[id]
}
