package com.jarvis.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class ProfilePage(
    private val act: Activity,
    private val store: Store,
    private val pickPhoto: () -> Unit,
    private val onChanged: () -> Unit
) {
    val root = FrameLayout(act)
    private val photoView = ImageView(act)
    private val photoPlaceholder = TextView(act)
    private lateinit var fUser: EditText
    private lateinit var fFull: EditText
    private lateinit var fMail: EditText
    private lateinit var fPhone: EditText

    val isOpen: Boolean get() = root.visibility == View.VISIBLE

    private fun dp(v: Int) = Ui.dp(act, v)
    private fun mono(t: TextView) { t.typeface = Typeface.MONOSPACE }

    init {
        build()
    }

    private fun field(parent: LinearLayout, label: String, hint: String, type: Int): EditText {
        val l = TextView(act)
        l.text = label
        l.setTextColor(Ui.SOFT)
        l.textSize = 11f
        l.letterSpacing = 0.2f
        mono(l)
        l.setPadding(dp(4), dp(18), 0, dp(2))
        val e = EditText(act)
        e.hint = hint
        e.inputType = type
        e.setSingleLine(true)
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(Color.parseColor("#4A6572"))
        e.textSize = 17f
        mono(e)
        e.background = null
        e.setPadding(dp(4), dp(6), dp(4), dp(6))
        parent.addView(l, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        parent.addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return e
    }

    private fun boxButton(text: String, color: Int): TextView {
        val b = TextView(act)
        b.text = text
        b.textSize = 15f
        b.setTextColor(color)
        b.gravity = Gravity.CENTER
        b.letterSpacing = 0.2f
        mono(b)
        b.setPadding(dp(16), dp(14), dp(16), dp(14))
        val bg = GradientDrawable()
        bg.setColor(Color.parseColor("#08121A"))
        bg.setStroke(dp(2), color)
        bg.cornerRadius = dp(6).toFloat()
        b.background = bg
        Ui.quiet(b)
        return b
    }

    private fun build() {
        root.setBackgroundColor(Ui.BG)
        root.isClickable = true
        root.visibility = View.GONE

        val col = LinearLayout(act)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.setPadding(dp(24), dp(96), dp(24), dp(120))

        val tag = TextView(act)
        tag.text = L.t("verified")
        tag.setTextColor(Color.parseColor("#39FF88"))
        tag.textSize = 12f
        mono(tag)
        tag.setPadding(0, 0, 0, dp(16))
        col.addView(tag)

        val holder = FrameLayout(act)
        val oval = GradientDrawable()
        oval.shape = GradientDrawable.OVAL
        oval.setColor(Ui.PANEL)
        oval.setStroke(dp(3), Ui.CYAN)
        holder.background = oval
        holder.clipToOutline = true
        photoPlaceholder.text = "👤"
        photoPlaceholder.textSize = 64f
        photoPlaceholder.gravity = Gravity.CENTER
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP
        holder.addView(photoPlaceholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        holder.addView(photoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        holder.setOnClickListener { pickPhoto() }
        Ui.quiet(holder)
        col.addView(holder, LinearLayout.LayoutParams(dp(150), dp(150)))

        val change = TextView(act)
        change.text = L.t("change_photo")
        change.setTextColor(Ui.CYAN)
        change.textSize = 14f
        mono(change)
        change.setPadding(0, dp(12), 0, dp(4))
        change.setOnClickListener { pickPhoto() }
        Ui.quiet(change)
        col.addView(change)

        fUser = field(col, L.t("f_user"), L.t("h_user"), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        fFull = field(col, L.t("f_full"), L.t("h_full"), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        fMail = field(col, L.t("f_mail"), L.t("h_mail"), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        fPhone = field(col, L.t("f_phone"), L.t("h_phone"), InputType.TYPE_CLASS_PHONE)

        val save = boxButton(L.t("save"), Ui.CYAN)
        save.setOnClickListener {
            saveFields()
            Toast.makeText(act, L.t("saved"), Toast.LENGTH_SHORT).show()
        }
        val saveLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        saveLp.topMargin = dp(28)
        col.addView(save, saveLp)

        val sv = ScrollView(act)
        sv.isFillViewport = true
        sv.addView(col)
        root.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        Bars.back(act, root) { close() }
        Bars.title(act, root, L.t("profile"), Gravity.TOP or Gravity.END)

        val out = boxButton("⏻  " + L.t("logout"), Ui.RED)
        out.setOnClickListener { confirmLogout() }
        val oLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        oLp.setMargins(dp(24), 0, dp(24), dp(28))
        root.addView(out, oLp)
    }

    private fun load() {
        fUser.setText(store.str("username"))
        fFull.setText(store.str("fullname"))
        fMail.setText(store.str("email"))
        fPhone.setText(store.str("phone"))
        val f = store.photoFile()
        if (f.exists()) {
            photoView.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
            photoPlaceholder.visibility = View.GONE
        } else {
            photoView.setImageDrawable(null)
            photoPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun saveFields() {
        store.put("username", fUser.text.toString().trim())
        store.put("fullname", fFull.text.toString().trim())
        store.put("email", fMail.text.toString().trim())
        store.put("phone", fPhone.text.toString().trim())
    }

    fun open() {
        load()
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.translationX = w
        root.visibility = View.VISIBLE
        root.animate().translationX(0f).setDuration(240).start()
    }

    fun close() {
        saveFields()
        onChanged()
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.animate().translationX(w).setDuration(220).withEndAction {
            root.visibility = View.GONE
        }.start()
    }

    private fun confirmLogout() {
        AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle(L.t("lo_title"))
            .setMessage(L.t("lo_msg"))
            .setPositiveButton(L.t("yes_del")) { _, _ ->
                store.remove("username", "fullname", "email", "phone")
                store.photoFile().delete()
                load()
                close()
                Toast.makeText(act, L.t("logged_out"), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(L.t("no"), null).show()
    }

    fun setPhoto(uri: Uri) {
        try {
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            act.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var s = 1
            while (bounds.outWidth / s > 1024 || bounds.outHeight / s > 1024) s *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = s
            var bmp = act.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return
            val ori = act.contentResolver.openInputStream(uri)?.use {
                ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            } ?: ExifInterface.ORIENTATION_NORMAL
            val deg = when (ori) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (deg != 0f) {
                val m = Matrix()
                m.postRotate(deg)
                bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
            }
            store.photoFile().outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
            load()
            onChanged()
        } catch (e: Exception) {
            Toast.makeText(act, L.t("photo_fail"), Toast.LENGTH_SHORT).show()
        }
    }
}

class HistoryPage(private val act: Activity, private val store: Store) {
    val root = FrameLayout(act)
    private val box = LinearLayout(act)
    private val scroll = ScrollView(act)

    val isOpen: Boolean get() = root.visibility == View.VISIBLE

    private fun dp(v: Int) = Ui.dp(act, v)

    init {
        root.setBackgroundColor(Color.parseColor("#F5050B12"))
        root.isClickable = true
        root.visibility = View.GONE

        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(14), dp(96), dp(14), dp(24))
        scroll.addView(box)
        root.addView(scroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        Bars.back(act, root) { close() }

        val head = TextView(act)
        head.text = "// " + L.t("hist_title")
        head.setTextColor(Ui.CYAN)
        head.textSize = 16f
        head.letterSpacing = 0.2f
        head.typeface = Typeface.MONOSPACE
        val hLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        hLp.topMargin = dp(48)
        root.addView(head, hLp)

        val del = TextView(act)
        del.text = L.t("del")
        del.setTextColor(Ui.RED)
        del.textSize = 14f
        del.typeface = Typeface.MONOSPACE
        del.setPadding(dp(16), dp(10), dp(16), dp(10))
        val dLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
        dLp.topMargin = dp(40)
        del.setOnClickListener {
            AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle(L.t("hist_del_title"))
                .setMessage(L.t("hist_del_msg"))
                .setPositiveButton(L.t("yes_del")) { _, _ ->
                    store.clearHistory()
                    fill()
                    Toast.makeText(act, L.t("hist_deleted"), Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton(L.t("no"), null).show()
        }
        Ui.quiet(del)
        root.addView(del, dLp)
    }

    private fun fill() {
        box.removeAllViews()
        var all = store.loadMsgs()
        if (all.isEmpty()) {
            val e = TextView(act)
            e.text = L.t("hist_empty")
            e.setTextColor(Ui.SOFT)
            e.textSize = 15f
            e.gravity = Gravity.CENTER
            e.setPadding(0, dp(60), 0, 0)
            box.addView(e)
            return
        }
        if (all.size > 500) {
            all = all.takeLast(500)
            val m = TextView(act)
            m.text = L.t("hist_cap")
            m.setTextColor(Ui.MUTED)
            m.textSize = 11f
            m.gravity = Gravity.CENTER
            box.addView(m)
        }
        var lastDay = ""
        for (x in all) {
            val day = L.date(x.third)
            if (day != lastDay) {
                lastDay = day
                val h = TextView(act)
                h.text = day
                h.setTextColor(Ui.SOFT)
                h.textSize = 12f
                h.gravity = Gravity.CENTER
                h.setShadowLayer(6f, 0f, 0f, Color.BLACK)
                val hl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                hl.topMargin = dp(16)
                hl.bottomMargin = dp(6)
                box.addView(h, hl)
            }
            box.addView(ChatUi.makeMsg(act, x.first, x.second, x.third, true).root)
        }
    }

    fun open() {
        fill()
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.translationX = w
        root.visibility = View.VISIBLE
        root.animate().translationX(0f).setDuration(240).start()
        scroll.post { scroll.fullScroll(View.FOCUS_DOWN) }
    }

    fun close() {
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.animate().translationX(w).setDuration(220).withEndAction {
            root.visibility = View.GONE
        }.start()
    }
}
