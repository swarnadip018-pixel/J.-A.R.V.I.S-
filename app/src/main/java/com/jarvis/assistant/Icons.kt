package com.jarvis.assistant

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.min

class IconView(ctx: Context, private val type: Int, private val color: Int = Ui.CYAN) : View(ctx) {
    companion object {
        const val NEW_CHAT = 0
        const val HISTORY = 1
        const val MIC = 2
        const val ABOUT = 3
        const val GEAR = 4
        const val KEY = 5
        const val WAVE = 6
        const val IDCARD = 7
        const val SPARK = 8
        const val ACCESS = 9
        const val GLOBE = 10
        const val IMAGE = 11
        const val CHEVRON = 12
    }

    private val p = Paint(Paint.ANTI_ALIAS_FLAG)
    private val f = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()

    init {
        p.style = Paint.Style.STROKE
        p.strokeCap = Paint.Cap.ROUND
        p.strokeJoin = Paint.Join.ROUND
        p.strokeWidth = 1.7f
        p.color = color
        f.style = Paint.Style.FILL
        f.color = color
    }

    override fun onDraw(c: Canvas) {
        val s = min(width, height) / 24f
        c.save()
        c.translate((width - 24f * s) / 2f, (height - 24f * s) / 2f)
        c.scale(s, s)
        path.reset()
        when (type) {
            NEW_CHAT -> {
                c.drawRoundRect(RectF(3f, 4f, 21f, 17f), 3f, 3f, p)
                path.moveTo(8f, 17f)
                path.lineTo(8f, 21f)
                path.lineTo(12.5f, 17f)
                c.drawPath(path, p)
                c.drawLine(12f, 7.5f, 12f, 13.5f, p)
                c.drawLine(9f, 10.5f, 15f, 10.5f, p)
            }
            HISTORY -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawLine(12f, 7f, 12f, 12f, p)
                c.drawLine(12f, 12f, 15.5f, 14f, p)
            }
            MIC -> {
                c.drawRoundRect(RectF(9f, 3f, 15f, 13f), 3f, 3f, p)
                c.drawArc(RectF(6f, 7f, 18f, 17f), 0f, 180f, false, p)
                c.drawLine(12f, 17f, 12f, 21f, p)
                c.drawLine(9f, 21f, 15f, 21f, p)
            }
            ABOUT -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawCircle(12f, 7.8f, 1.1f, f)
                c.drawLine(12f, 11f, 12f, 17f, p)
            }
            GEAR -> {
                c.drawCircle(12f, 12f, 6f, p)
                c.drawCircle(12f, 12f, 2.4f, p)
                for (i in 0 until 8) {
                    c.save()
                    c.rotate(i * 45f, 12f, 12f)
                    c.drawLine(12f, 3f, 12f, 6f, p)
                    c.restore()
                }
            }
            KEY -> {
                c.drawCircle(7.5f, 15.5f, 3.6f, p)
                c.drawLine(10.2f, 13f, 20f, 3.5f, p)
                c.drawLine(17f, 6.5f, 19.5f, 9f, p)
                c.drawLine(14.5f, 9f, 16.5f, 11f, p)
            }
            WAVE -> {
                val xs = floatArrayOf(4f, 8f, 12f, 16f, 20f)
                val hs = floatArrayOf(2f, 6f, 9f, 5f, 2f)
                for (i in xs.indices) c.drawLine(xs[i], 12f - hs[i], xs[i], 12f + hs[i], p)
            }
            IDCARD -> {
                c.drawRoundRect(RectF(3f, 5f, 21f, 19f), 2.5f, 2.5f, p)
                c.drawCircle(8.5f, 11f, 1.8f, p)
                c.drawLine(5.5f, 16f, 11.5f, 16f, p)
                c.drawLine(14f, 10f, 18f, 10f, p)
                c.drawLine(14f, 13.5f, 18f, 13.5f, p)
            }
            SPARK -> {
                path.moveTo(12f, 3f)
                path.quadTo(12f, 12f, 21f, 12f)
                path.quadTo(12f, 12f, 12f, 21f)
                path.quadTo(12f, 12f, 3f, 12f)
                path.quadTo(12f, 12f, 12f, 3f)
                path.close()
                c.drawPath(path, p)
            }
            ACCESS -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawCircle(12f, 7.7f, 1.2f, f)
                c.drawLine(7.5f, 10.8f, 16.5f, 10.8f, p)
                c.drawLine(12f, 10.8f, 12f, 14.5f, p)
                c.drawLine(12f, 14.5f, 9.5f, 18.5f, p)
                c.drawLine(12f, 14.5f, 14.5f, 18.5f, p)
            }
            GLOBE -> {
                c.drawCircle(12f, 12f, 9f, p)
                c.drawOval(RectF(8f, 3f, 16f, 21f), p)
                c.drawLine(3f, 12f, 21f, 12f, p)
                c.drawLine(4.5f, 7.5f, 19.5f, 7.5f, p)
                c.drawLine(4.5f, 16.5f, 19.5f, 16.5f, p)
            }
            IMAGE -> {
                c.drawRoundRect(RectF(3.5f, 3.5f, 20.5f, 20.5f), 3f, 3f, p)
                c.drawCircle(8.7f, 9f, 1.6f, p)
                path.moveTo(4f, 18f)
                path.lineTo(9.5f, 12.5f)
                path.lineTo(13f, 16f)
                path.lineTo(15.5f, 13.5f)
                path.lineTo(20f, 18f)
                c.drawPath(path, p)
            }
            CHEVRON -> {
                path.moveTo(9f, 5f)
                path.lineTo(16f, 12f)
                path.lineTo(9f, 19f)
                c.drawPath(path, p)
            }
        }
        c.restore()
    }
}

object Rows {
    fun row(ctx: Context, icon: Int, title: String, sub: String?, onTap: () -> Unit): View {
        val r = LinearLayout(ctx)
        r.orientation = LinearLayout.HORIZONTAL
        r.gravity = Gravity.CENTER_VERTICAL
        r.setPadding(Ui.dp(ctx, 4), Ui.dp(ctx, 13), Ui.dp(ctx, 4), Ui.dp(ctx, 13))
        r.addView(IconView(ctx, icon), LinearLayout.LayoutParams(Ui.dp(ctx, 26), Ui.dp(ctx, 26)))

        val col = LinearLayout(ctx)
        col.orientation = LinearLayout.VERTICAL
        val t = TextView(ctx)
        t.text = title
        t.textSize = 17f
        t.setTextColor(Color.WHITE)
        col.addView(t)
        if (!sub.isNullOrEmpty()) {
            val s = TextView(ctx)
            s.text = sub
            s.textSize = 12.5f
            s.setTextColor(Ui.MUTED)
            col.addView(s)
        }
        val clp = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
        clp.leftMargin = Ui.dp(ctx, 16)
        r.addView(col, clp)
        Ui.quiet(r)
        r.setOnClickListener { onTap() }
        return r
    }

    fun divider(ctx: Context): View {
        val v = View(ctx)
        v.setBackgroundColor(Color.parseColor("#1F3340"))
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(ctx, 1))
        lp.topMargin = Ui.dp(ctx, 10)
        lp.bottomMargin = Ui.dp(ctx, 10)
        v.layoutParams = lp
        return v
    }
}

object Bars {
    fun back(act: Activity, root: FrameLayout, onBack: () -> Unit) {
        val b = TextView(act)
        b.text = "‹ " + L.t("back")
        b.setTextColor(Ui.CYAN)
        b.textSize = 16f
        b.typeface = Typeface.MONOSPACE
        b.setPadding(Ui.dp(act, 16), Ui.dp(act, 10), Ui.dp(act, 16), Ui.dp(act, 10))
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        lp.topMargin = Ui.dp(act, 38)
        b.setOnClickListener { onBack() }
        Ui.quiet(b)
        root.addView(b, lp)
    }

    fun title(act: Activity, root: FrameLayout, text: String, gravity: Int) {
        val h = TextView(act)
        h.text = "// " + text
        h.setTextColor(Ui.CYAN)
        h.textSize = 16f
        h.letterSpacing = 0.2f
        h.typeface = Typeface.MONOSPACE
        val lp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, gravity)
        lp.topMargin = Ui.dp(act, 48)
        lp.rightMargin = Ui.dp(act, 20)
        root.addView(h, lp)
    }
}
