package com.jarvis.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.min
import kotlin.math.sin

class HudView(ctx: Context) : View(ctx) {
    var active = false
    private val d = resources.displayMetrics.density
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private var rot = 0f
    private var pulse = 0f
    private var last = 0L

    private fun cyan(a: Int) = Color.argb(a, 0, 229, 255)

    override fun onMeasure(w: Int, h: Int) {
        val s = MeasureSpec.getSize(w)
        setMeasuredDimension(s, s)
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        rot += dt * (if (active) 110f else 28f)
        pulse += dt * (if (active) 7f else 2f)

        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 8 * d

        line.strokeWidth = 3 * d
        line.color = cyan(210)
        c.drawCircle(cx, cy, r, line)

        c.save()
        c.rotate(rot, cx, cy)
        for (i in 0 until 60) {
            val big = i % 5 == 0
            line.strokeWidth = if (big) 3 * d else 1.5f * d
            line.color = cyan(if (big) 255 else 130)
            val len = if (big) 14 * d else 8 * d
            c.drawLine(cx, cy - r * 0.9f, cx, cy - r * 0.9f + len, line)
            c.rotate(6f, cx, cy)
        }
        c.restore()

        line.strokeWidth = 7 * d
        line.color = cyan(230)
        val rect2 = RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f)
        c.save()
        c.rotate(-rot * 1.5f, cx, cy)
        for (i in 0 until 3) c.drawArc(rect2, i * 120f, 80f, false, line)
        c.restore()

        line.strokeWidth = 3 * d
        line.color = cyan(180)
        val rect3 = RectF(cx - r * 0.6f, cy - r * 0.6f, cx + r * 0.6f, cy + r * 0.6f)
        c.save()
        c.rotate(rot * 2f, cx, cy)
        for (i in 0 until 6) c.drawArc(rect3, i * 60f, 24f, false, line)
        c.restore()

        line.strokeWidth = 2 * d
        line.color = cyan(160)
        c.drawCircle(cx, cy, r * 0.44f, line)

        val glow = r * 0.38f * (1f + 0.08f * sin(pulse))
        fill.shader = RadialGradient(
            cx, cy, glow,
            intArrayOf(cyan(if (active) 255 else 190), cyan(0)),
            null, Shader.TileMode.CLAMP
        )
        c.drawCircle(cx, cy, glow, fill)
        fill.shader = null
        fill.color = cyan(255)
        c.drawCircle(cx, cy, r * 0.07f, fill)

        postInvalidateOnAnimation()
    }
}
