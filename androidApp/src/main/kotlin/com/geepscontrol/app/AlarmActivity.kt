package com.geepscontrol.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.MotionEvent
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.TextClock
import android.widget.EditText
import com.geepscontrol.core.domain.AlarmDifficulty
import com.geepscontrol.core.domain.AlarmExperience

/** The app-owned alarm surface. Future dismissal challenges can replace the action area. */
class AlarmActivity : Activity() {
    private var alarmId: Long = 0
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var alertStartedAt: Long = 0
    private var rampRunnable: Runnable? = null
    private var vibrationRunnable: Runnable? = null
    private val waitingAlarms = ArrayDeque<Intent>()
    private var displayingAlarm = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
        showAlarm(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (displayingAlarm) waitingAlarms.addLast(Intent(intent)) else { setIntent(intent); showAlarm(intent) }
    }

    private fun showAlarm(intent: Intent) {
        displayingAlarm = true
        stopAlert()
        alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ID, 0)
        val label = intent.getStringExtra(AlarmReceiver.EXTRA_LABEL).orEmpty().ifBlank { "Alarm" }
        val isTimer = intent.getBooleanExtra(AlarmReceiver.EXTRA_IS_TIMER, false)
        startAlert(
            sound = intent.getBooleanExtra(AlarmReceiver.EXTRA_SOUND, true),
            vibration = intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION, true),
            soundId = intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_ID),
            loop = intent.getBooleanExtra(AlarmReceiver.EXTRA_LOOP_SOUND, !isTimer),
            vibrationMode = intent.getStringExtra(AlarmReceiver.EXTRA_VIBRATION_MODE).orEmpty(),
            vibrationDelaySeconds = intent.getIntExtra(AlarmReceiver.EXTRA_VIBRATION_DELAY, 0),
            startingVolume = intent.getIntExtra(AlarmReceiver.EXTRA_START_VOLUME, 100),
            maximumVolume = intent.getIntExtra(AlarmReceiver.EXTRA_MAX_VOLUME, 100),
            rampSeconds = intent.getIntExtra(AlarmReceiver.EXTRA_RAMP_SECONDS, 0),
        )

        val padding = (24 * resources.displayMetrics.density).toInt()
        val root =
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(padding, padding, padding, padding)
                setBackgroundColor(Color.rgb(18, 18, 18))
            }
        root.addView(
            TextClock(this).apply { format12Hour = "h:mm a"; format24Hour = "HH:mm"; textSize = 54f; setTextColor(Color.WHITE); gravity = Gravity.CENTER },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            TextView(this).apply {
                text = if (isTimer) "Timer complete" else "Life Minder alarm"
                textSize = 20f
                setTextColor(Color.LTGRAY)
                gravity = Gravity.CENTER
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        root.addView(
            TextView(this).apply {
                text = label
                textSize = 36f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER
                setPadding(0, padding, 0, padding * 2)
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        intent.getStringExtra(AlarmReceiver.EXTRA_PROFILE_NAME)?.let { profileName -> root.addView(TextView(this).apply { text = profileName; textSize = 18f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER }) }
        val dismissalAction = if (isTimer) AlarmReceiver.ACTION_TIMER_DISMISS else AlarmReceiver.ACTION_DISMISS
        if (!isTimer && intent.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_AVAILABLE, true)) root.addView(actionButton("Snooze") { perform(AlarmReceiver.ACTION_SNOOZE) })
        val method = intent.getStringExtra(AlarmReceiver.EXTRA_DISMISSAL_METHOD).orEmpty()
        val difficulty = runCatching { AlarmDifficulty.valueOf(intent.getStringExtra(AlarmReceiver.EXTRA_DISMISSAL_DIFFICULTY).orEmpty()) }.getOrDefault(AlarmDifficulty.NORMAL)
        when (method) {
            "HOLD" -> root.addView(holdButton("Hold to acknowledge", if (difficulty == AlarmDifficulty.EASY) 3 else if (difficulty == AlarmDifficulty.HARD) 10 else 5) { perform(dismissalAction) })
            "NUMBER" -> addNumberChallenge(root, difficulty) { perform(dismissalAction) }
            "TRACE" -> {
                root.addView(TextView(this).apply { text = "Trace from green to red"; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER })
                root.addView(TraceChallengeView(this, difficulty) { perform(dismissalAction) }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (300 * resources.displayMetrics.density).toInt()))
            }
            else -> root.addView(actionButton("Acknowledge") { perform(dismissalAction) })
        }
        if (method in setOf("HOLD", "NUMBER", "TRACE")) root.addView(holdButton("Emergency hold to acknowledge", 5) { perform(dismissalAction) })
        if (method != "STANDARD" && getSystemService(AccessibilityManager::class.java).isEnabled) root.addView(actionButton("Accessible acknowledge") { perform(dismissalAction) })
        setContentView(root)
    }

    private fun actionButton(
        label: String,
        action: () -> Unit,
    ) = Button(this).apply {
        text = label
        textSize = 18f
        isAllCaps = false
        setOnClickListener { action() }
        layoutParams =
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                val spacing = (8 * resources.displayMetrics.density).toInt()
                setMargins(0, spacing, 0, spacing)
            }
    }

    private fun perform(action: String) {
        stopAlert()
        sendBroadcast(
            Intent(this, AlarmReceiver::class.java)
                .setAction(action)
                .putExtra(AlarmReceiver.EXTRA_ID, alarmId),
        )
        val next = waitingAlarms.removeFirstOrNull()
        if (next == null) {
            displayingAlarm = false
            finishAndRemoveTask()
        } else {
            setIntent(next)
            showAlarm(next)
        }
    }

    private fun startAlert(
        sound: Boolean,
        vibration: Boolean,
        soundId: String?,
        loop: Boolean,
        vibrationMode: String,
        vibrationDelaySeconds: Int,
        startingVolume: Int,
        maximumVolume: Int,
        rampSeconds: Int,
    ) {
        alertStartedAt = System.currentTimeMillis()
        if (sound) {
            player = runCatching {
                MediaPlayer().apply {
                    setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build())
                    AlarmSoundCatalog.assetPath(soundId)?.let { path -> assets.openFd(path).use { setDataSource(it.fileDescriptor, it.startOffset, it.length) } }
                        ?: setDataSource(this@AlarmActivity, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                    isLooping = loop
                    prepare()
                    val initial = AlarmExperience.volumeAt(0, startingVolume, maximumVolume, rampSeconds)
                    setVolume(initial, initial)
                    start()
                }
            }.getOrNull()
            rampRunnable = object : Runnable {
                override fun run() {
                    val volume = AlarmExperience.volumeAt(System.currentTimeMillis() - alertStartedAt, startingVolume, maximumVolume, rampSeconds)
                    player?.setVolume(volume, volume)
                    if (volume < maximumVolume.coerceIn(0, 100) / 100f) handler.postDelayed(this, 250)
                }
            }.also { handler.post(it) }
        }
        if (vibration) {
            vibrationRunnable = Runnable {
                @Suppress("DEPRECATION")
                vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator
                val pattern = when (vibrationMode) {
                    "LIGHT" -> longArrayOf(0, 200, 800)
                    "STRONG" -> longArrayOf(0, 900, 250)
                    "ESCALATING" -> longArrayOf(0, 150, 600, 300, 400, 600, 250)
                    "CONTINUOUS" -> longArrayOf(0, 1200, 100)
                    else -> longArrayOf(0, 700, 400)
                }
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }.also { handler.postDelayed(it, vibrationDelaySeconds.coerceAtLeast(0) * 1000L) }
        }
    }

    private fun stopAlert() {
        rampRunnable?.let(handler::removeCallbacks)
        vibrationRunnable?.let(handler::removeCallbacks)
        rampRunnable = null
        vibrationRunnable = null
        player?.let { runCatching { it.stop() } }
        player?.release()
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun holdButton(label: String, seconds: Int, action: () -> Unit) = Button(this).apply {
        text = label
        isAllCaps = false
        val completion = Runnable { action() }
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> { text = "Keep holding… ${seconds}s"; handler.postDelayed(completion, seconds * 1000L); true }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { handler.removeCallbacks(completion); text = label; true }
                else -> true
            }
        }
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun addNumberChallenge(root: LinearLayout, difficulty: AlarmDifficulty, action: () -> Unit) {
        val expected = AlarmExperience.numberChallenge(difficulty)
        val prompt = TextView(this).apply { text = "Enter $expected to acknowledge"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; textSize = 24f; setTextColor(Color.WHITE); hint = "Number"; setHintTextColor(Color.GRAY) }
        val error = TextView(this).apply { setTextColor(Color.rgb(255, 120, 120)); gravity = Gravity.CENTER }
        root.addView(prompt)
        root.addView(input, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        root.addView(error)
        root.addView(actionButton("Acknowledge") { if (AlarmExperience.validateNumber(expected, input.text.toString())) action() else error.text = "That number does not match" })
    }

    override fun onDestroy() {
        stopAlert()
        super.onDestroy()
    }
}
