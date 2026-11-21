package com.jarvis.assistant

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.View
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

class HudView(ctx: Context) : View(ctx) {
    var active = false
    private val d = resources.displayMetrics.density
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    private class Bolt(val pts: FloatArray, val born: Float, val life: Float)
    private val bolts = ArrayList<Bolt>()

    private var rot = 0f
    private var pulse = 0f
    private var clock = 0f
    private var glowLevel = 0f
    private var spawnAcc = 0f
    private var last = 0L

    private fun cyan(a: Int) = Color.argb(a, 0, 229, 255)

    override fun onMeasure(w: Int, h: Int) {
        val s = MeasureSpec.getSize(w)
        setMeasuredDimension(s, s)
    }

    private fun spawn() {
        val steps = 9 + Random.nextInt(5)
        var a = Random.nextFloat() * 6.2832f
        val main = FloatArray((steps + 1) * 2)
        val branchAt = 3 + Random.nextInt(steps - 4)
        var bx = 0f
        var by = 0f
        var ba = 0f
        for (i in 0..steps) {
            val rad = 0.1f + 0.88f * i / steps
            a += (Random.nextFloat() - 0.5f) * 0.45f
            main[i * 2] = cos(a) * rad
            main[i * 2 + 1] = sin(a) * rad
            if (i == branchAt) { bx = main[i * 2]; by = main[i * 2 + 1]; ba = a }
        }
        val life = 0.25f + Random.nextFloat() * 0.35f
        bolts.add(Bolt(main, clock, life))
        if (Random.nextFloat() < 0.7f) {
            val n = 3 + Random.nextInt(3)
            val br = FloatArray((n + 1) * 2)
            br[0] = bx
            br[1] = by
            var aa = ba + (if (Random.nextBoolean()) 0.7f else -0.7f)
            var rr = hypot(bx, by)
            for (i in 1..n) {
                rr = min(rr + 0.07f, 0.98f)
                aa += (Random.nextFloat() - 0.5f) * 0.5f
                br[i * 2] = cos(aa) * rr
                br[i * 2 + 1] = sin(aa) * rr
            }
            bolts.add(Bolt(br, clock, life * 0.9f))
        }
    }

    private fun drawBolts(c: Canvas, cx: Float, cy: Float, r: Float) {
        for (b in bolts) {
            val age = (clock - b.born) / b.life
            if (age >= 1f) continue
            val flick = 0.75f + 0.25f * sin(clock * 40f + b.born * 50f)
            val a = ((1f - age) * flick * glowLevel).coerceIn(0f, 1f)
            path.reset()
            path.moveTo(cx + b.pts[0] * r, cy + b.pts[1] * r)
            var i = 2
            while (i < b.pts.size) {
                path.lineTo(cx + b.pts[i] * r, cy + b.pts[i + 1] * r)
                i += 2
            }
            line.strokeWidth = 9 * d
            line.color = Color.argb((a * 40).toInt(), 255, 255, 255)
            c.drawPath(path, line)
            line.strokeWidth = 4 * d
            line.color = Color.argb((a * 110).toInt(), 200, 245, 255)
            c.drawPath(path, line)
            line.strokeWidth = 1.6f * d
            line.color = Color.argb((a * 255).toInt(), 255, 255, 255)
            c.drawPath(path, line)
        }
    }

    override fun onDraw(c: Canvas) {
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        clock += dt
        rot += dt * (if (active) 110f else 28f)
        pulse += dt * (if (active) 7f else 2f)

        val target = if (active) 1f else 0f
        glowLevel += (target - glowLevel) * min(1f, dt * 5f)
        if (glowLevel > 0.02f) {
            spawnAcc += dt * 16f * glowLevel
            while (spawnAcc >= 1f) { spawn(); spawnAcc -= 1f }
        } else {
            bolts.clear()
        }
        bolts.removeAll { clock - it.born >= it.life }

        val cx = width / 2f
        val cy = height / 2f
        val r = min(width, height) / 2f - 8 * d

        // পেছনের সাদা আভা ও শিরার মতো আলো
        if (glowLevel > 0.02f) {
            fill.shader = RadialGradient(
                cx, cy, r,
                intArrayOf(Color.argb((70 * glowLevel).toInt(), 255, 255, 255), Color.argb(0, 255, 255, 255)),
                null, Shader.TileMode.CLAMP
            )
            c.drawCircle(cx, cy, r, fill)
            fill.shader = null
            drawBolts(c, cx, cy, r * 0.98f)
        }

        // বাইরের রিং
        line.strokeWidth = 3 * d
        line.color = cyan(210)
        c.drawCircle(cx, cy, r, line)

        // টিক-চিহ্নের রিং
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

        // বড় চাপ
        line.strokeWidth = 7 * d
        line.color = cyan(230)
        val rect2 = RectF(cx - r * 0.74f, cy - r * 0.74f, cx + r * 0.74f, cy + r * 0.74f)
        c.save()
        c.rotate(-rot * 1.5f, cx, cy)
        for (i in 0 until 3) c.drawArc(rect2, i * 120f, 80f, false, line)
        c.restore()

        // মাঝের ছোট চাপ
        line.strokeWidth = 3 * d
        line.color = cyan(180)
        val rect3 = RectF(cx - r * 0.6f, cy - r * 0.6f, cx + r * 0.6f, cy + r * 0.6f)
        c.save()
        c.rotate(rot * 2f, cx, cy)
        for (i in 0 until 6) c.drawArc(rect3, i * 60f, 24f, false, line)
        c.restore()

        // ভেতরের রিং
        line.strokeWidth = 2 * d
        line.color = cyan(160)
        c.drawCircle(cx, cy, r * 0.44f, line)

        // জ্বলজ্বলে কেন্দ্র
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
