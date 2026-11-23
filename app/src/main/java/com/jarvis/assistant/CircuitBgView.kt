package com.jarvis.assistant

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class CircuitBgView(ctx: Context) : View(ctx) {
    var isGlowing: () -> Boolean = { false }
    var focusX = -1f
    var focusY = -1f
    private var bmp: Bitmap? = null
    private val dimPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val brightPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val maskPaint = Paint()
    private var level = 0f
    private var spread = 0f
    private var clock = 0f
    private var last = 0L

    init {
        val id = resources.getIdentifier("circuit_bg", "drawable", ctx.packageName)
        if (id != 0) bmp = BitmapFactory.decodeResource(resources, id)
        dimPaint.alpha = 95
        brightPaint.colorFilter = ColorMatrixColorFilter(
            ColorMatrix(
                floatArrayOf(
                    1.5f, 0f, 0f, 0f, 0f,
                    0f, 1.5f, 0f, 0f, 0f,
                    0f, 0f, 1.5f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
        maskPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }

    private fun drawFit(c: Canvas, b: Bitmap, p: Paint) {
        val w = width.toFloat()
        val h = height.toFloat()
        val s = max(w / b.width, h / b.height)
        val sw = b.width * s
        val sh = b.height * s
        val fx = if (focusX >= 0f) focusX else w / 2f
        val fy = if (focusY >= 0f) focusY else h / 2f
        var tx = fx - 0.57f * sw
        var ty = fy - 0.54f * sh
        tx = min(0f, max(w - sw, tx))
        ty = min(0f, max(h - sh, ty))
        c.save()
        c.translate(tx, ty)
        c.scale(s, s)
        c.drawBitmap(b, 0f, 0f, p)
        c.restore()
    }

    override fun onDraw(c: Canvas) {
        val b = bmp ?: return
        val now = System.nanoTime()
        val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.1f)
        last = now
        clock += dt
        val on = isGlowing()
        level += ((if (on) 1f else 0f) - level) * min(1f, dt * 4f)
        spread = if (on) min(1f, spread + dt * 1.1f) else max(0f, spread - dt * 2f)

        drawFit(c, b, dimPaint)

        if (level > 0.01f) {
            val w = width.toFloat()
            val h = height.toFloat()
            val cx = if (focusX >= 0f) focusX else w / 2f
            val cy = if (focusY >= 0f) focusY else h / 2f
            val maxR = hypot(max(cx, w - cx), max(cy, h - cy))
            val e = 1f - (1f - spread) * (1f - spread)
            val r = max(1f, maxR * (0.12f + 0.88f * e))
            val n = 16
            val colors = IntArray(n)
            val pos = FloatArray(n)
            val ringP = (clock * 0.5f) % 1f
            for (i in 0 until n) {
                val x = i / (n - 1f)
                pos[i] = x
                val base = (1f - x).pow(1.3f) * 0.9f
                val dd = x - ringP
                val ring = exp(-(dd * dd) / (2f * 0.06f * 0.06f)) * 0.8f * (1f - ringP)
                val a = (base + ring).coerceIn(0f, 1f) * level
                colors[i] = Color.argb((a * 255f).toInt(), 255, 255, 255)
            }
            maskPaint.shader = RadialGradient(cx, cy, r, colors, pos, Shader.TileMode.CLAMP)
            val sc = c.saveLayer(0f, 0f, w, h, null)
            drawFit(c, b, brightPaint)
            c.drawRect(0f, 0f, w, h, maskPaint)
            c.restoreToCount(sc)
        }
        postInvalidateOnAnimation()
    }
}
