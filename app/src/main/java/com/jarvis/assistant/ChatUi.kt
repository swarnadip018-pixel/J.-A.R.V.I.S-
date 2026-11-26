package com.jarvis.assistant

import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import java.text.BreakIterator

object Ui {
    val BG = Color.parseColor("#050B12")
    val CYAN = Color.parseColor("#00E5FF")
    val SOFT = Color.parseColor("#7FDBE8")
    val PANEL = Color.parseColor("#0A1A24")
    val RED = Color.parseColor("#FF4D4D")
    val MUTED = Color.parseColor("#7C97A3")

    fun dp(ctx: Context, v: Int): Int = (v * ctx.resources.displayMetrics.density).toInt()

    fun quiet(v: View) {
        v.isSoundEffectsEnabled = false
        v.isHapticFeedbackEnabled = false
    }
}

object ChatUi {
    class Msg(val root: View, val tv: TextView)

    private val handler = Handler(Looper.getMainLooper())

    // role: 0 = আপনি (ডানে), 1 = জার্ভিস (বামে), 2 = সিস্টেম বার্তা
    fun makeMsg(ctx: Context, role: Int, text: String, ts: Long, showTime: Boolean): Msg {
        val wrap = LinearLayout(ctx)
        wrap.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = Ui.dp(ctx, 6)
        lp.bottomMargin = Ui.dp(ctx, 6)

        val t = TextView(ctx)
        t.text = text
        t.textSize = 16f
        t.maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.82f).toInt()
        t.setLineSpacing(0f, 1.15f)
        t.setShadowLayer(8f, 0f, 0f, Color.BLACK)

        when (role) {
            0 -> {
                lp.gravity = Gravity.END
                t.setTextColor(Ui.CYAN)
                t.textAlignment = View.TEXT_ALIGNMENT_VIEW_END
            }
            1 -> {
                lp.gravity = Gravity.START
                t.setTextColor(Color.WHITE)
            }
            else -> {
                lp.gravity = Gravity.CENTER_HORIZONTAL
                t.setTextColor(Color.parseColor("#FFB4B4"))
                t.textSize = 13f
                t.gravity = Gravity.CENTER
                t.maxWidth = (ctx.resources.displayMetrics.widthPixels * 0.9f).toInt()
            }
        }
        wrap.addView(t)

        if (showTime && role != 2) {
            val time = TextView(ctx)
            time.text = L.time(ts)
            time.textSize = 10f
            time.setTextColor(Ui.MUTED)
            time.setShadowLayer(6f, 0f, 0f, Color.BLACK)
            val tlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            tlp.gravity = if (role == 0) Gravity.END else Gravity.START
            tlp.topMargin = Ui.dp(ctx, 2)
            wrap.addView(time, tlp)
        }
        wrap.layoutParams = lp
        return Msg(wrap, t)
    }

    // টাইপিং অ্যানিমেশন: অক্ষর অক্ষর করে লেখা ওঠে
    fun typeInto(tv: TextView, text: String, perCharMs: Long, onTick: () -> Unit) {
        val bi = BreakIterator.getCharacterInstance()
        bi.setText(text)
        val ends = ArrayList<Int>()
        var e = bi.next()
        while (e != BreakIterator.DONE) {
            ends.add(e)
            e = bi.next()
        }
        if (ends.isEmpty()) {
            tv.text = text
            return
        }
        val ms = (12000L / ends.size).coerceIn(8L, perCharMs)
        var i = 0
        val r = object : Runnable {
            override fun run() {
                if (i < ends.size) {
                    tv.text = text.substring(0, ends[i])
                    i++
                    onTick()
                    handler.postDelayed(this, ms)
                } else {
                    tv.text = text
                    onTick()
                }
            }
        }
        tv.text = ""
        handler.post(r)
    }
}
