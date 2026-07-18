package com.geepscontrol.app

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.media.AudioAttributes
import android.media.MediaPlayer
import com.geepscontrol.core.domain.PlatformActions

class AndroidPlatformActions(
    private val context: Context,
) : PlatformActions {
    private var previewPlayer: MediaPlayer? = null
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

    private fun open(intent: Intent): Boolean =
        runCatching {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        }.getOrDefault(false)
}
