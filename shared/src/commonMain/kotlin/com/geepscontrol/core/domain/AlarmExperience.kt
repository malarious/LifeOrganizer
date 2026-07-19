package com.geepscontrol.core.domain

import kotlin.math.roundToInt
import kotlin.random.Random

enum class AlarmDismissalMethod { STANDARD, HOLD, NUMBER, TRACE, MATH, MEMORY, SCAN, NFC, SORT }
enum class AlarmDifficulty { EASY, NORMAL, HARD }

data class AlarmSoundOption(val id: String, val name: String, val category: String)

val bundledAlarmSounds = listOf(
    AlarmSoundOption("gentle_pizzicato", "Gentle Pizzicato", "Gentle"),
    AlarmSoundOption("bright_pizzicato", "Bright Pizzicato", "Playful"),
    AlarmSoundOption("standard_steel", "Standard Steel", "Dramatic"),
    AlarmSoundOption("calm_steel", "Calm Steel", "Gentle"),
    AlarmSoundOption("retro_rise", "Retro Rise", "Retro"),
    AlarmSoundOption("retro_alert", "Retro Alert", "Energetic"),
    AlarmSoundOption("sax_wakeup", "Sax Wake-Up", "Energetic"),
    AlarmSoundOption("short_hit", "Short Hit", "Short alert"),
    AlarmSoundOption("timer_hit", "Timer Complete", "Timer completion"),
)

object AlarmExperience {
    fun challengeGraceSeconds(difficulty: AlarmDifficulty) = when (difficulty) { AlarmDifficulty.EASY -> 5; AlarmDifficulty.NORMAL -> 7; AlarmDifficulty.HARD -> 10 }

    fun mathChallenge(difficulty: AlarmDifficulty, random: Random = Random.Default): Pair<String, Int> = when (difficulty) {
        AlarmDifficulty.EASY -> random.nextInt(1, 10).let { a -> random.nextInt(1, 10).let { b -> "$a + $b" to a + b } }
        AlarmDifficulty.NORMAL -> random.nextInt(2, 13).let { a -> random.nextInt(2, 13).let { b -> "$a × $b" to a * b } }
        AlarmDifficulty.HARD -> random.nextInt(3, 15).let { a -> random.nextInt(2, 10).let { b -> random.nextInt(1, 20).let { c -> "($a × $b) + $c" to a * b + c } } }
    }

    fun memoryLength(difficulty: AlarmDifficulty) = when (difficulty) { AlarmDifficulty.EASY -> 3; AlarmDifficulty.NORMAL -> 5; AlarmDifficulty.HARD -> 7 }
    fun volumeAt(
        elapsedMillis: Long,
        startPercent: Int,
        maximumPercent: Int,
        rampSeconds: Int,
    ): Float {
        val start = startPercent.coerceIn(0, 100) / 100f
        val end = maximumPercent.coerceIn(startPercent.coerceIn(0, 100), 100) / 100f
        if (rampSeconds <= 0) return end
        val fraction = (elapsedMillis.toFloat() / (rampSeconds * 1000f)).coerceIn(0f, 1f)
        return start + (end - start) * fraction
    }

    fun vibrationShouldStart(elapsedMillis: Long, delaySeconds: Int) = elapsedMillis >= delaySeconds.coerceAtLeast(0) * 1000L

    fun canSnooze(currentCount: Int, maximumSnoozes: Int) = maximumSnoozes <= 0 || currentCount < maximumSnoozes

    fun holdComplete(elapsedMillis: Long, requiredSeconds: Int) = elapsedMillis >= requiredSeconds.coerceAtLeast(1) * 1000L

    fun numberLength(difficulty: AlarmDifficulty) = when (difficulty) {
        AlarmDifficulty.EASY -> 3
        AlarmDifficulty.NORMAL -> 4
        AlarmDifficulty.HARD -> 6
    }

    fun numberChallenge(difficulty: AlarmDifficulty, random: Random = Random.Default): String =
        buildString { repeat(numberLength(difficulty)) { append(random.nextInt(if (isEmpty()) 1 else 0, 10)) } }

    fun validateNumber(expected: String, entered: String) = expected == entered.trim()

    fun traceProgress(pointsHit: Int, totalPoints: Int): Float =
        if (totalPoints <= 0) 0f else (pointsHit.toFloat() / totalPoints).coerceIn(0f, 1f)

    fun traceComplete(pointsHit: Int, totalPoints: Int, threshold: Float = 0.9f) = traceProgress(pointsHit, totalPoints) >= threshold.coerceIn(0f, 1f)

    fun discretePercent(value: Float) = (value.coerceIn(0f, 1f) * 100).roundToInt()
}
