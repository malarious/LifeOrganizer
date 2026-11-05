package com.geepscontrol.app

import android.app.Activity
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
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
import android.nfc.NfcAdapter
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
import android.widget.ScrollView
import kotlin.random.Random
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
    private var challengeCompletion: (() -> Unit)? = null
    private var challengeTimeout: Runnable? = null
    private var acknowledgedAlarmInQueue = false
    private val stopReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val id = intent.getLongExtra(AlarmReceiver.EXTRA_ID, 0)
            waitingAlarms.removeAll { it.getLongExtra(AlarmReceiver.EXTRA_ID, 0) == id }
            if (displayingAlarm && alarmId == id) {
                stopAlert()
                finishCurrentAlarm()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextCompat.registerReceiver(this, stopReceiver, IntentFilter(AndroidAlarmScheduler.ACTION_STOP_ALERT), ContextCompat.RECEIVER_NOT_EXPORTED)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON,
        )
        if (Build.VERSION.SDK_INT == 26) {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        acknowledgedAlarmInQueue = savedInstanceState?.getBoolean("acknowledged_alarm") ?: false
        if (intent.getBooleanExtra(EXTRA_ACKNOWLEDGE_ON_OPEN, false)) {
            alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_ID, 0)
            perform(AlarmReceiver.ACTION_DISMISS)
        } else showAlarm(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.getBooleanExtra(EXTRA_ACKNOWLEDGE_ON_OPEN, false)) {
            val target = intent.getLongExtra(AlarmReceiver.EXTRA_ID, 0)
            if (target == alarmId) perform(AlarmReceiver.ACTION_DISMISS) else {
                waitingAlarms.removeAll { it.getLongExtra(AlarmReceiver.EXTRA_ID, 0) == target }
                sendBroadcast(Intent(this, AlarmReceiver::class.java).setAction(AlarmReceiver.ACTION_DISMISS).putExtra(AlarmReceiver.EXTRA_ID, target))
                acknowledgedAlarmInQueue = true
                if (!displayingAlarm) finishCurrentAlarm()
            }
            return
        }
        if (displayingAlarm && intent.getLongExtra(AlarmReceiver.EXTRA_ID, 0) == alarmId) return
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
        val method = intent.getStringExtra(AlarmReceiver.EXTRA_DISMISSAL_METHOD).orEmpty()
        val difficulty = runCatching { AlarmDifficulty.valueOf(intent.getStringExtra(AlarmReceiver.EXTRA_DISMISSAL_DIFFICULTY).orEmpty()) }.getOrDefault(AlarmDifficulty.NORMAL)
        val challenged = method !in setOf("", "STANDARD")
        root.addView(actionButton("Acknowledge") { perform(dismissalAction) }.apply {
            minHeight = (64 * resources.displayMetrics.density).toInt()
            setBackgroundColor(Color.rgb(49, 92, 73))
            setTextColor(Color.WHITE)
        })
        if (!isTimer) root.addView(TextView(this).apply { text = "Stop this alarm. Repeating alarms keep their normal schedule."; textSize = 15f; setTextColor(Color.LTGRAY); gravity = Gravity.CENTER })
        if (!isTimer && intent.getBooleanExtra(AlarmReceiver.EXTRA_SNOOZE_AVAILABLE, true)) root.addView(actionButton("Snooze") { if (challenged) beginChallenge(root, method, difficulty, AlarmReceiver.ACTION_SNOOZE) else perform(AlarmReceiver.ACTION_SNOOZE) })
        if (challenged) root.addView(actionButton("Use dismissal challenge") { beginChallenge(root, method, difficulty, dismissalAction) })
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(root) })
    }

    private fun beginChallenge(root: LinearLayout, method: String, difficulty: AlarmDifficulty, action: String) {
        stopAlert()
        root.removeAllViews()
        val seconds = AlarmExperience.challengeGraceSeconds(difficulty)
        root.addView(TextView(this).apply { text = "$seconds seconds of quiet — complete the challenge"; textSize = 22f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        val complete = { challengeTimeout?.let(handler::removeCallbacks); challengeTimeout = null; challengeCompletion = null; perform(action) }
        challengeCompletion = complete
        when (method) {
            "HOLD" -> root.addView(holdButton("Hold to finish", if (difficulty == AlarmDifficulty.EASY) 3 else if (difficulty == AlarmDifficulty.HARD) 8 else 5, complete))
            "NUMBER" -> addNumberChallenge(root, difficulty, complete)
            "MATH" -> addMathChallenge(root, difficulty, complete)
            "MEMORY" -> addMemoryChallenge(root, difficulty, complete)
            "SORT" -> addSortingChallenge(root, difficulty, complete)
            "TRACE" -> root.addView(TraceChallengeView(this, difficulty, complete), LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, (300 * resources.displayMetrics.density).toInt()))
            "NFC" -> startNfcChallenge(root)
            "SCAN" -> startScanChallenge(root)
            else -> complete()
        }
        root.addView(holdButton("Emergency hold", 5, complete))
        challengeTimeout = Runnable {
            startAlert(
                intent.getBooleanExtra(AlarmReceiver.EXTRA_SOUND, true),
                intent.getBooleanExtra(AlarmReceiver.EXTRA_VIBRATION, true),
                intent.getStringExtra(AlarmReceiver.EXTRA_SOUND_ID),
                intent.getBooleanExtra(AlarmReceiver.EXTRA_LOOP_SOUND, true),
                intent.getStringExtra(AlarmReceiver.EXTRA_VIBRATION_MODE).orEmpty(),
                intent.getIntExtra(AlarmReceiver.EXTRA_VIBRATION_DELAY, 0),
                intent.getIntExtra(AlarmReceiver.EXTRA_START_VOLUME, 100),
                intent.getIntExtra(AlarmReceiver.EXTRA_MAX_VOLUME, 100),
                intent.getIntExtra(AlarmReceiver.EXTRA_RAMP_SECONDS, 0),
            )
        }.also { handler.postDelayed(it, seconds * 1000L) }
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
        if (action == AlarmReceiver.ACTION_DISMISS) acknowledgedAlarmInQueue = true
        finishCurrentAlarm()
    }

    private fun finishCurrentAlarm() {
        challengeTimeout?.let(handler::removeCallbacks)
        challengeTimeout = null
        challengeCompletion = null
        val next = waitingAlarms.removeFirstOrNull()
        if (next == null) {
            displayingAlarm = false
            if (acknowledgedAlarmInQueue) {
                if (AndroidWeatherRepository(this).afterAlarm) startActivity(Intent(this, WeatherActivity::class.java).putExtra(WeatherActivity.EXTRA_FROM_ALARM, true))
                else startActivity(Intent(this, MainActivity::class.java).putExtra(MainActivity.EXTRA_OPEN_TODAY, true).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            finish()
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

    private fun addMathChallenge(root: LinearLayout, difficulty: AlarmDifficulty, action: () -> Unit) {
        val (problem, answer) = AlarmExperience.mathChallenge(difficulty)
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_SIGNED; textSize = 24f; setTextColor(Color.WHITE); hint = "Answer"; setHintTextColor(Color.GRAY) }
        root.addView(TextView(this).apply { text = "Solve $problem"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        root.addView(input)
        root.addView(actionButton("Submit") { if (input.text.toString().toIntOrNull() == answer) action() else input.error = "Try again" })
    }

    private fun addMemoryChallenge(root: LinearLayout, difficulty: AlarmDifficulty, action: () -> Unit) {
        val expected = buildString { repeat(AlarmExperience.memoryLength(difficulty)) { append(Random.nextInt(0, 10)) } }
        val prompt = TextView(this).apply { text = "Remember: $expected"; textSize = 28f; setTextColor(Color.WHITE); gravity = Gravity.CENTER }
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; textSize = 24f; setTextColor(Color.WHITE); visibility = android.view.View.GONE }
        root.addView(prompt); root.addView(input)
        handler.postDelayed({ prompt.text = "Enter the sequence"; input.visibility = android.view.View.VISIBLE }, 1800)
        root.addView(actionButton("Submit") { if (input.text.toString() == expected) action() else input.error = "Try again" })
    }

    private fun addSortingChallenge(root: LinearLayout, difficulty: AlarmDifficulty, action: () -> Unit) {
        val count = AlarmExperience.memoryLength(difficulty)
        val values = List(count) { Random.nextInt(1, 100) }.distinct().let { if (it.size < count) (1..count).shuffled() else it }
        val answer = values.sorted().joinToString("")
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_NUMBER; textSize = 24f; setTextColor(Color.WHITE); hint = "Enter without spaces"; setHintTextColor(Color.GRAY) }
        root.addView(TextView(this).apply { text = "Smallest to largest: ${values.joinToString(", ")}"; textSize = 24f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        root.addView(input)
        root.addView(actionButton("Submit") { if (input.text.toString() == answer) action() else input.error = "Try again" })
    }

    private fun startNfcChallenge(root: LinearLayout) {
        val adapter = NfcAdapter.getDefaultAdapter(this)
        if (adapter == null) root.addView(TextView(this).apply { text = "NFC is not available on this device"; setTextColor(Color.WHITE) })
        else {
            root.addView(TextView(this).apply { text = "Tap an NFC tag"; textSize = 24f; setTextColor(Color.WHITE) })
            adapter.enableReaderMode(this, { runOnUiThread { challengeCompletion?.invoke() } }, NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_NFC_F or NfcAdapter.FLAG_READER_NFC_V, null)
        }
    }

    private fun startScanChallenge(root: LinearLayout) {
        root.addView(TextView(this).apply { text = "Open a barcode scanner and scan any code"; textSize = 22f; setTextColor(Color.WHITE) })
        root.addView(actionButton("Open scanner") {
            val scan = Intent("com.google.zxing.client.android.SCAN")
            if (scan.resolveActivity(packageManager) != null) startActivityForResult(scan, 4401)
            else root.addView(TextView(this).apply { text = "No compatible scanner is installed"; setTextColor(Color.rgb(255, 150, 150)) })
        })
    }

    @Deprecated("Compatibility callback for external barcode scanners")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 4401 && resultCode == RESULT_OK) challengeCompletion?.invoke()
    }

    override fun onDestroy() {
        unregisterReceiver(stopReceiver)
        runCatching { NfcAdapter.getDefaultAdapter(this)?.disableReaderMode(this) }
        challengeTimeout?.let(handler::removeCallbacks)
        stopAlert()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("acknowledged_alarm", acknowledgedAlarmInQueue)
        super.onSaveInstanceState(outState)
    }

    companion object { const val EXTRA_ACKNOWLEDGE_ON_OPEN = "acknowledge_on_open" }
}
