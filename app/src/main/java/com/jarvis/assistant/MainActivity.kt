package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val BG = Color.parseColor("#050B12")
    private val CYAN = Color.parseColor("#00E5FF")
    private val SOFT = Color.parseColor("#7FDBE8")
    private val PANEL = Color.parseColor("#0A1A24")
    private val RED = Color.parseColor("#FF4D4D")
    private val MODEL = "gemini-2.5-flash"
    private val PICK_PHOTO = 77

    private val SYS = """তুমি J.A.R.V.I.S, ব্যবহারকারীর ব্যক্তিগত ভয়েস সহকারী। শান্ত, ভদ্র ও সংক্ষিপ্তভাবে উত্তর দাও (সাধারণত ১-৩ বাক্য), কারণ উত্তরটা জোরে পড়ে শোনানো হবে।
ব্যবহারকারী যে ভাষায় কথা বলেছে, ঠিক সেই ভাষাতেই উত্তর দাও।
উত্তর শুধু এই JSON ফরম্যাটে দাও, অন্য কিছু নয়:
{"lang":"ভাষার কোড (bn, en, hi, ur, ar, ta ইত্যাদি)","reply":"তোমার উত্তর"}
reply-তে মার্কডাউন, তারকা চিহ্ন বা ইমোজি ব্যবহার করো না।
তুমি এখনও ফোন নিয়ন্ত্রণ (কল, অ্যাপ খোলা) বা লাইভ ইন্টারনেট সার্চ করতে পারো না। এমন কিছু চাইলে সৎভাবে বলো যে এই সুবিধা এখনও যোগ হয়নি। আন্দাজে ভুল তথ্য দিও না।"""

    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var hud: HudView
    private lateinit var drawer: LinearLayout
    private lateinit var scrim: View
    private lateinit var profilePage: FrameLayout
    private lateinit var photoView: ImageView
    private lateinit var photoPlaceholder: TextView
    private lateinit var fUser: EditText
    private lateinit var fFull: EditText
    private lateinit var fMail: EditText
    private lateinit var fPhone: EditText
    private var recognizer: SpeechRecognizer? = null
    private val history = ArrayList<Pair<String, String>>()
    private var chatMode = false
    private var silent = 0

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    private fun photoFile() = File(filesDir, "profile.jpg")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this)
        root.setBackgroundColor(BG)

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL
        content.setPadding(0, dp(110), 0, 0)

        val title = TextView(this)
        title.text = "J.A.R.V.I.S"
        title.textSize = 34f
        title.setTextColor(CYAN)
        title.letterSpacing = 0.25f
        title.typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        title.gravity = Gravity.CENTER

        val sub = TextView(this)
        sub.text = "your personal assistant"
        sub.textSize = 14f
        sub.setTextColor(SOFT)
        sub.letterSpacing = 0.15f
        sub.gravity = Gravity.CENTER

        hud = HudView(this)
        val hudLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        hudLp.setMargins(dp(60), dp(24), dp(60), 0)
        hud.setOnClickListener {
            if (chatMode) {
                stopChat("কথোপকথন বন্ধ। আবার শুরু করতে গোলকে চাপুন।")
            } else {
                chatMode = true
                silent = 0
                micTap()
            }
        }

        status = TextView(this)
        status.text = "গোলকে একবার চাপুন, তারপর স্বাভাবিকভাবে কথা বলুন। বন্ধ করতে আবার চাপুন।"
        status.textSize = 16f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER
        status.setPadding(dp(24), dp(16), dp(24), dp(16))
        val scroll = ScrollView(this)
        scroll.addView(status)

        content.addView(title)
        content.addView(sub)
        content.addView(hud, hudLp)
        content.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(content)

        val menuBtn = TextView(this)
        menuBtn.text = "☰"
        menuBtn.textSize = 28f
        menuBtn.setTextColor(CYAN)
        menuBtn.setPadding(dp(16), dp(8), dp(16), dp(8))
        val mLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        mLp.topMargin = dp(36)
        menuBtn.setOnClickListener { openDrawer() }
        root.addView(menuBtn, mLp)

        val profile = TextView(this)
        profile.text = "👤"
        profile.textSize = 20f
        profile.gravity = Gravity.CENTER
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setStroke(dp(2), CYAN)
        ring.setColor(PANEL)
        profile.background = ring
        val pLp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
        pLp.topMargin = dp(42)
        pLp.rightMargin = dp(16)
        profile.setOnClickListener { openProfile() }
        root.addView(profile, pLp)

        scrim = View(this)
        scrim.setBackgroundColor(Color.parseColor("#99000000"))
        scrim.visibility = View.GONE
        scrim.setOnClickListener { closeDrawer() }
        root.addView(scrim, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        drawer = LinearLayout(this)
        drawer.orientation = LinearLayout.VERTICAL
        drawer.setBackgroundColor(Color.parseColor("#0A141D"))
        drawer.setPadding(dp(20), dp(70), dp(20), dp(20))
        drawer.translationX = -dp(290).toFloat()
        buildDrawer()
        root.addView(drawer, FrameLayout.LayoutParams(dp(280), ViewGroup.LayoutParams.MATCH_PARENT))

        buildProfilePage()
        root.addView(profilePage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(root)
        tts = TextToSpeech(this, this)
    }

    // ---------- মেনু ----------
    private fun buildDrawer() {
        val head = TextView(this)
        head.text = "মেনু"
        head.textSize = 22f
        head.setTextColor(CYAN)
        head.setPadding(0, 0, 0, dp(16))
        drawer.addView(head)

        item("💬", "কথোপকথন মুছুন") { history.clear(); toast("মুছে ফেলা হয়েছে") }
        item("🌐", "শোনার ভাষা") { showLangDialog() }
        item("🔊", "ভয়েস সেটিংস") { toast("শীঘ্রই আসছে") }
        item("🔑", "API Key") { showKeyDialog() }
        item("🔒", "অনুমতি") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        item("ℹ️", "অ্যাপ সম্পর্কে") {
            darkDialog()
                .setTitle("J.A.R.V.I.S")
                .setMessage("আপনার ব্যক্তিগত সহকারী\nসংস্করণ 2.2 (প্রোফাইল পেজ ও শিরা-আলো)")
                .setPositiveButton("ঠিক আছে", null).show()
        }
    }

    private fun item(icon: String, label: String, onTap: () -> Unit) {
        val t = TextView(this)
        t.text = "$icon   $label"
        t.textSize = 17f
        t.setTextColor(Color.WHITE)
        t.setPadding(0, dp(14), 0, dp(14))
        t.setOnClickListener { closeDrawer(); onTap() }
        drawer.addView(t)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun openDrawer() {
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
    }

    private fun closeDrawer() {
        scrim.visibility = View.GONE
        drawer.animate().translationX(-dp(290).toFloat()).setDuration(200).start()
    }

    override fun onBackPressed() {
        when {
            profilePage.visibility == View.VISIBLE -> closeProfile()
            scrim.visibility == View.VISIBLE -> closeDrawer()
            else -> super.onBackPressed()
        }
    }

    private fun darkDialog() = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    // ---------- প্রোফাইল পেজ ----------
    private fun mono(t: TextView) { t.typeface = Typeface.MONOSPACE }

    private fun field(parent: LinearLayout, label: String, hint: String, type: Int): EditText {
        val l = TextView(this)
        l.text = label
        l.setTextColor(SOFT)
        l.textSize = 11f
        l.letterSpacing = 0.2f
        mono(l)
        l.setPadding(dp(4), dp(16), 0, dp(6))
        val e = EditText(this)
        e.hint = hint
        e.inputType = type
        e.setSingleLine(true)
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(Color.parseColor("#4A6572"))
        e.textSize = 16f
        mono(e)
        val bg = GradientDrawable()
        bg.setColor(PANEL)
        bg.setStroke(dp(1), CYAN)
        bg.cornerRadius = dp(6).toFloat()
        e.background = bg
        e.setPadding(dp(14), dp(12), dp(14), dp(12))
        parent.addView(l, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        parent.addView(e, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        return e
    }

    private fun boxButton(text: String, color: Int): TextView {
        val b = TextView(this)
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
        return b
    }

    private fun buildProfilePage() {
        profilePage = FrameLayout(this)
        profilePage.setBackgroundColor(BG)
        profilePage.isClickable = true
        profilePage.visibility = View.GONE

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.gravity = Gravity.CENTER_HORIZONTAL
        col.setPadding(dp(24), dp(96), dp(24), dp(120))

        val tag = TextView(this)
        tag.text = "> IDENTITY VERIFIED_"
        tag.setTextColor(Color.parseColor("#39FF88"))
        tag.textSize = 12f
        mono(tag)
        tag.setPadding(0, 0, 0, dp(16))
        col.addView(tag)

        val holder = FrameLayout(this)
        val oval = GradientDrawable()
        oval.shape = GradientDrawable.OVAL
        oval.setColor(PANEL)
        oval.setStroke(dp(3), CYAN)
        holder.background = oval
        holder.clipToOutline = true
        photoPlaceholder = TextView(this)
        photoPlaceholder.text = "👤"
        photoPlaceholder.textSize = 64f
        photoPlaceholder.gravity = Gravity.CENTER
        photoView = ImageView(this)
        photoView.scaleType = ImageView.ScaleType.CENTER_CROP
        holder.addView(photoPlaceholder, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        holder.addView(photoView, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        holder.setOnClickListener { pickPhoto() }
        col.addView(holder, LinearLayout.LayoutParams(dp(150), dp(150)))

        val change = TextView(this)
        change.text = "[ ছবি বদলান ]"
        change.setTextColor(CYAN)
        change.textSize = 14f
        mono(change)
        change.setPadding(0, dp(12), 0, dp(4))
        change.setOnClickListener { pickPhoto() }
        col.addView(change)

        fUser = field(col, "USERNAME", "username", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
        fFull = field(col, "FULL NAME", "পুরো নাম", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        fMail = field(col, "GMAIL ID", "name@gmail.com", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        fPhone = field(col, "PHONE NUMBER", "+880...", InputType.TYPE_CLASS_PHONE)

        val save = boxButton("SAVE", CYAN)
        save.setOnClickListener { saveProfile(); toast("সেভ হয়েছে") }
        val saveLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        saveLp.topMargin = dp(24)
        col.addView(save, saveLp)

        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.addView(col)
        profilePage.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // ওপরের বার
        val back = TextView(this)
        back.text = "‹ BACK"
        back.setTextColor(CYAN)
        back.textSize = 16f
        mono(back)
        back.setPadding(dp(16), dp(10), dp(16), dp(10))
        val bLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        bLp.topMargin = dp(38)
        back.setOnClickListener { closeProfile() }
        profilePage.addView(back, bLp)

        val head = TextView(this)
        head.text = "// PROFILE"
        head.setTextColor(CYAN)
        head.textSize = 16f
        head.letterSpacing = 0.2f
        mono(head)
        val hLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
        hLp.topMargin = dp(48)
        hLp.rightMargin = dp(20)
        profilePage.addView(head, hLp)

        // সবার নিচে LOG OUT
        val out = boxButton("⏻  LOG OUT", RED)
        out.setOnClickListener { confirmLogout() }
        val oLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        oLp.setMargins(dp(24), 0, dp(24), dp(28))
        profilePage.addView(out, oLp)
    }

    private fun loadProfileUi() {
        val p = prefs()
        fUser.setText(p.getString("username", ""))
        fFull.setText(p.getString("fullname", ""))
        fMail.setText(p.getString("email", ""))
        fPhone.setText(p.getString("phone", ""))
        val f = photoFile()
        if (f.exists()) {
            photoView.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
            photoPlaceholder.visibility = View.GONE
        } else {
            photoView.setImageDrawable(null)
            photoPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun saveProfile() {
        prefs().edit()
            .putString("username", fUser.text.toString().trim())
            .putString("fullname", fFull.text.toString().trim())
            .putString("email", fMail.text.toString().trim())
            .putString("phone", fPhone.text.toString().trim())
            .apply()
    }

    private fun openProfile() {
        loadProfileUi()
        val w = resources.displayMetrics.widthPixels.toFloat()
        profilePage.translationX = w
        profilePage.visibility = View.VISIBLE
        profilePage.animate().translationX(0f).setDuration(240).start()
    }

    private fun closeProfile() {
        saveProfile()
        val w = resources.displayMetrics.widthPixels.toFloat()
        profilePage.animate().translationX(w).setDuration(220).withEndAction {
            profilePage.visibility = View.GONE
        }.start()
    }

    private fun confirmLogout() {
        darkDialog()
            .setTitle("LOG OUT")
            .setMessage("আপনার প্রোফাইলের তথ্য ও ছবি এই ফোন থেকে মুছে যাবে। (API Key থাকবে।) নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ ->
                prefs().edit().remove("username").remove("fullname").remove("email").remove("phone").apply()
                photoFile().delete()
                history.clear()
                loadProfileUi()
                closeProfile()
                toast("লগ আউট হয়েছে")
            }
            .setNegativeButton("না", null).show()
    }

    private fun pickPhoto() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, "ছবি বাছুন"), PICK_PHOTO)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req == PICK_PHOTO && res == RESULT_OK && data?.data != null) {
            try {
                savePhoto(data.data!!)
                loadProfileUi()
            } catch (e: Exception) {
                toast("ছবিটা নেওয়া যায়নি")
            }
        }
    }

    private fun savePhoto(uri: Uri) {
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var s = 1
        while (bounds.outWidth / s > 1024 || bounds.outHeight / s > 1024) s *= 2
        val opts = BitmapFactory.Options()
        opts.inSampleSize = s
        var bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return
        val ori = contentResolver.openInputStream(uri)?.use {
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
        photoFile().outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    }

    private fun showKeyDialog() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(8), dp(24), 0)
        val info = TextView(this)
        info.text = "aistudio.google.com থেকে নেওয়া Gemini API Key এখানে পেস্ট করুন। এটা শুধু আপনার ফোনে থাকবে।"
        info.setTextColor(Color.WHITE)
        val key = EditText(this)
        key.hint = "AIza..."
        key.setTextColor(Color.WHITE)
        key.setHintTextColor(Color.GRAY)
        box.addView(info)
        box.addView(key)
        val has = !prefs().getString("key", "").isNullOrEmpty()
        darkDialog().setTitle(if (has) "🔑 API Key (আগেরটা সেভ আছে)" else "🔑 API Key").setView(box)
            .setPositiveButton("সেভ") { _, _ ->
                val k = key.text.toString().trim()
                if (k.isNotEmpty()) {
                    prefs().edit().putString("key", k).apply()
                    toast("Key সেভ হয়েছে")
                }
            }
            .setNegativeButton("বন্ধ", null).show()
    }

    private fun showLangDialog() {
        val names = arrayOf("বাংলা", "English", "हिन्दी")
        val codes = arrayOf("bn-BD", "en-US", "hi-IN")
        darkDialog().setTitle("🌐 আপনি কোন ভাষায় কথা বলবেন?")
            .setItems(names) { _, i ->
                prefs().edit().putString("inlang", codes[i]).apply()
                toast("শোনার ভাষা: ${names[i]}")
            }.show()
    }

    // ---------- কথোপকথন নিয়ন্ত্রণ ----------
    private fun stopChat(msg: String) {
        chatMode = false
        tts.stop()
        recognizer?.cancel()
        hud.active = false
        status.text = msg
    }

    override fun onPause() {
        if (chatMode) stopChat("কথোপকথন বন্ধ। আবার শুরু করতে গোলকে চাপুন।")
        super.onPause()
    }

    // ---------- ভয়েস আউটপুট ----------
    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) {
            tts.setPitch(0.8f)
            tts.setSpeechRate(0.92f)
            tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) {
                    runOnUiThread { hud.active = true }
                }
                override fun onDone(id: String?) {
                    runOnUiThread { afterSpeech() }
                }
                override fun onError(id: String?) {
                    runOnUiThread { afterSpeech() }
                }
            })
        }
    }

    private fun afterSpeech() {
        hud.active = false
        if (chatMode) {
            hud.postDelayed({ if (chatMode) listen() }, 500)
        }
    }

    private fun speakIn(lang: String, text: String): Boolean {
        val l = lang.lowercase().take(2)
        val candidates = when (l) {
            "bn" -> listOf(Locale("bn", "BD"), Locale("bn", "IN"))
            "en" -> listOf(Locale.UK, Locale.US)
            "hi" -> listOf(Locale("hi", "IN"))
            else -> listOf(Locale.forLanguageTag(lang))
        }
        for (loc in candidates) {
            val r = tts.setLanguage(loc)
            if (r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u1")
                return true
            }
        }
        return false
    }

    // ---------- মাইক ----------
    private fun micTap() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listen()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) {
            listen()
        } else {
            chatMode = false
            status.text = "মাইকের অনুমতি ছাড়া শুনতে পারব না"
        }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            chatMode = false
            status.text = "এই ফোনে ভয়েস চেনার সুবিধা নেই"
            return
        }
        tts.stop()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle?) {
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t == null) { hud.active = false; return }
                silent = 0
                askAi(t)
            }
            override fun onError(e: Int) {
                hud.active = false
                val quiet = e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (chatMode && quiet) {
                    silent++
                    if (silent >= 3) {
                        stopChat("অনেকক্ষণ কিছু শুনিনি, কথোপকথন বন্ধ করলাম। আবার শুরু করতে গোলকে চাপুন।")
                    } else {
                        hud.postDelayed({ if (chatMode) listen() }, 300)
                    }
                } else {
                    chatMode = false
                    status.text = "বুঝতে পারিনি, আবার গোলকে চেপে বলুন (কোড $e)"
                }
            }
            override fun onReadyForSpeech(p: Bundle?) {
                hud.active = true
                status.text = if (chatMode) "শুনছি… (বন্ধ করতে গোলকে চাপুন)" else "শুনছি…"
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, prefs().getString("inlang", "bn-BD"))
        recognizer?.startListening(i)
    }

    // ---------- AI ব্রেইন ----------
    private fun askAi(userText: String) {
        val key = prefs().getString("key", "")?.trim().orEmpty()
        if (key.isEmpty()) {
            chatMode = false
            hud.active = false
            status.text = "আপনি বললেন: $userText\n\nAI চালাতে আগে মেনু → API Key-তে Key বসান।"
            showKeyDialog()
            return
        }
        hud.active = true
        status.text = "আপনি: $userText\n\nভাবছি…"
        val p = prefs()
        var name = p.getString("fullname", "").orEmpty()
        if (name.isEmpty()) name = p.getString("username", "").orEmpty()
        val sys = if (name.isEmpty()) SYS else SYS + "\nব্যবহারকারীর নাম: " + name

        Thread {
            try {
                val contents = JSONArray()
                for (h in history.takeLast(8)) {
                    contents.put(JSONObject()
                        .put("role", h.first)
                        .put("parts", JSONArray().put(JSONObject().put("text", h.second))))
                }
                contents.put(JSONObject()
                    .put("role", "user")
                    .put("parts", JSONArray().put(JSONObject().put("text", userText))))

                val body = JSONObject()
                    .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sys))))
                    .put("contents", contents)
                    .put("generationConfig", JSONObject()
                        .put("responseMimeType", "application/json")
                        .put("temperature", 0.7)
                        .put("thinkingConfig", JSONObject().put("thinkingBudget", 0)))

                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 45000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", key)
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val text = stream.bufferedReader().use { it.readText() }

                if (code !in 200..299) {
                    runOnUiThread { showHttpError(code) }
                } else {
                    val out = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    val clean = out.replace("```json", "").replace("```", "").trim()
                    val j = try { JSONObject(clean) } catch (e: Exception) {
                        JSONObject().put("lang", "bn").put("reply", out)
                    }
                    val reply = j.optString("reply", out)
                    val lang = j.optString("lang", "bn")
                    history.add(Pair("user", userText))
                    history.add(Pair("model", reply))
                    runOnUiThread {
                        status.text = "আপনি: $userText\n\n$reply"
                        if (!speakIn(lang, reply)) {
                            hud.active = false
                            chatMode = false
                            status.text = status.text.toString() + "\n\n(এই ভাষার ভয়েস ফোনে নেই। Settings → Text-to-speech → Google Speech Services থেকে ডাউনলোড করুন।)"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    chatMode = false
                    hud.active = false
                    status.text = "ইন্টারনেট বা সংযোগে সমস্যা। নেট চালু আছে কি না দেখুন।\n(${e.javaClass.simpleName})"
                }
            }
        }.start()
    }

    private fun showHttpError(code: Int) {
        chatMode = false
        hud.active = false
        status.text = when (code) {
            400, 403 -> "API Key ঠিক নয় বা অনুমতি নেই (কোড $code)। মেনু → API Key-তে আবার বসান।"
            404 -> "মডেলের নাম পাওয়া যায়নি (কোড 404)। আমাকে জানান।"
            429 -> "ফ্রি সীমা শেষ। কিছুক্ষণ পরে আবার চেষ্টা করুন (কোড 429)।"
            else -> "সার্ভারে সমস্যা হয়েছে (কোড $code)। আবার চেষ্টা করুন।"
        }
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
