package com.geepscontrol.core.domain

interface PlatformActions {
    fun openMap(address: String): Boolean

    fun openWebsite(url: String): Boolean

    fun call(phone: String): Boolean

    fun copy(
        label: String,
        value: String,
    )

    fun previewAlarmSound(soundId: String): Boolean

    fun stopAlarmSoundPreview()
}
