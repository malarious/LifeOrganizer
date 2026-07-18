package com.geepscontrol.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import android.view.View
import com.geepscontrol.core.domain.AlarmDifficulty
import com.geepscontrol.core.domain.AlarmExperience
import kotlin.math.hypot

class TraceChallengeView(
    context: Context,
    private val difficulty: AlarmDifficulty,
    private val onComplete: () -> Unit,
) : View(context) {
    private val pathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.LTGRAY; strokeWidth = 18f; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND }
    private val progressPaint = Paint(pathPaint).apply { color = Color.rgb(90, 210, 140); strokeWidth = 22f }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private var points = emptyList<PointF>()
    private var reached = 0
    private var tracing = false

    init { contentDescription = "Trace from the green start marker to the red end marker" }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val turns = when (difficulty) { AlarmDifficulty.EASY -> 3; AlarmDifficulty.NORMAL -> 5; AlarmDifficulty.HARD -> 7 }
        points = (0..turns).map { index -> PointF(w * (0.12f + 0.76f * index / turns), if (index % 2 == 0) h * 0.28f else h * 0.72f) }
        reached = 0
    }

    override fun onDraw(canvas: Canvas) {
        points.zipWithNext().forEach { (a, b) -> canvas.drawLine(a.x, a.y, b.x, b.y, pathPaint) }
        points.take((reached + 1).coerceAtMost(points.size)).zipWithNext().forEach { (a, b) -> canvas.drawLine(a.x, a.y, b.x, b.y, progressPaint) }
        points.firstOrNull()?.let { markerPaint.color = Color.GREEN; canvas.drawCircle(it.x, it.y, 22f, markerPaint) }
        points.lastOrNull()?.let { markerPaint.color = Color.RED; canvas.drawCircle(it.x, it.y, 22f, markerPaint) }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (points.isEmpty()) return false
        val tolerance = when (difficulty) { AlarmDifficulty.EASY -> 95f; AlarmDifficulty.NORMAL -> 75f; AlarmDifficulty.HARD -> 60f }
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                tracing = distance(event.x, event.y, points.first()) <= tolerance
                reached = if (tracing) 1 else 0
            }
            MotionEvent.ACTION_MOVE -> if (tracing && reached < points.size) {
                if (distance(event.x, event.y, points[reached]) <= tolerance) reached++
                if (AlarmExperience.traceComplete(reached, points.size, 1f)) { tracing = false; onComplete() }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> if (reached < points.size) { tracing = false; reached = 0 }
        }
        invalidate()
        return true
    }

    private fun distance(x: Float, y: Float, point: PointF) = hypot(x - point.x, y - point.y)
}
