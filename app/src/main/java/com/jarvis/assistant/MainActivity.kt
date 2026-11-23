package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.ExifInterface
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.AlarmClock
import android.provider.ContactsContract
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
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val BG = Color.parseColor("#050B12")
    private val CYAN = Color.parseColor("#00E5FF")
    private val SOFT = Color.parseColor("#7FDBE8")
    private val PANEL = Color.parseColor("#0A1A24")
    private val RED = Color.parseColor("#FF4D4D")
    private val MODELS = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.7-flash",
        "gemini-3.8-flash"
    )
    private val PICK_PHOTO = 77

    private val SYS = """তুমি J.A.R.V.I.S, ব্যবহারকারীর ব্যক্তিগত ভয়েস সহকারী। শান্ত, ভদ্র ও সংক্ষিপ্তভাবে উত্তর দাও (সাধারণত ১-৩ বাক্য), কারণ উত্তরটা জোরে পড়ে শোনানো হবে।
ব্যবহারকারী যে ভাষায় কথা বলেছে, ঠিক সেই ভাষাতেই উত্তর দাও।
উত্তর শুধু এই JSON ফরম্যাটে দাও, অন্য কিছু নয়:
{"lang":"ভাষার কোড (bn, en, hi, ur, ar, ta ইত্যাদি)","reply":"তোমার উত্তর","action":"none","arg":"","arg2":""}
reply-তে মার্কডাউন, তারকা চিহ্ন বা ইমোজি ব্যবহার করো না।

ফোনে কাজ করানোর জন্য action ঘরে এগুলোর একটা দাও (না লাগলে "none"):
- open_app: arg = অ্যাপের নাম ইংরেজিতে, যেমন YouTube, WhatsApp, Camera, Settings, Chrome, Gallery
- dial: arg = ফোন নম্বর (ডায়ালারে নম্বর সাজিয়ে দেয়, কল ব্যবহারকারী নিজে করে)
- call_contact: arg = কন্টাক্টের নাম (ফোনে যেভাবে সেভ থাকার সম্ভাবনা, বাংলা নাম হলে ইংরেজি বানানও ভাবো)
- sms: arg = নম্বর বা কন্টাক্টের নাম, arg2 = মেসেজের লেখা (মেসেজ লেখার স্ক্রিন খোলে, পাঠায় ব্যবহারকারী)
- torch: arg = "on" বা "off"
- alarm: arg = "HH:MM" (২৪ ঘণ্টার ফরম্যাট), arg2 = লেবেল
- timer: arg = সেকেন্ড সংখ্যা
- search: arg = গুগলে খোঁজার শব্দ
- youtube: arg = ইউটিউবে খোঁজার শব্দ
- maps: arg = জায়গার নাম

নিয়ম: reply-তে শুধু বলো তুমি কী করছ, যেমন "ইউটিউব খুলছি"। কল বা SMS-এর ক্ষেত্রে বলো "ডায়ালারে সাজিয়ে দিচ্ছি, কল বাটন আপনি চাপুন"। বার্তা বা কলের নম্বর না জানলে আন্দাজ করো না, action none দিয়ে জিজ্ঞেস করো। এই তালিকার বাইরের কাজ (Wi-Fi/ব্লুটুথ অন-অফ, অন্য অ্যাপের ভেতরে ট্যাপ, ক্যামেরায় ছবি তোলা, সার্চের ফল পড়ে শোনানো) এখনও পারো না, সৎভাবে বলো। আন্দাজে ভুল তথ্য দিও না।"""

    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var hud: HudView
    private lateinit var circuit: CircuitBgView
    private lateinit var drawer: LinearLayout
    private lateinit var scrim: View
    private lateinit var chatBox: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var avatarImg: ImageView
    private lateinit var avatarEmoji: TextView
    private lateinit var profilePage: FrameLayout
    private lateinit var photoView: ImageView
    private lateinit var photoPlaceholder: TextView
    private lateinit var historyPage: FrameLayout
    private lateinit var histBox: LinearLayout
    private lateinit var histScroll: ScrollView
    private lateinit var fUser: EditText
    private lateinit var fFull: EditText
    private lateinit var fMail: EditText
    private lateinit var fPhone: EditText
    private var recognizer: SpeechRecognizer? = null
    private var player: MediaPlayer? = null
    private var voiceGen = 0
    private val history = ArrayList<Pair<String, String>>()
    private var chatMode = false
    private var silent = 0
    private var pendingAction: JSONObject? = null
    private var pendingPerm: JSONObject? = null
    private var torchOn = false
    private val timeFmt = SimpleDateFormat("hh:mm a", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("d MMMM yyyy, EEEE", Locale.getDefault())

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("jarvis", Context.MODE_PRIVATE)
    private fun photoFile() = File(filesDir, "profile.jpg")
    private fun histFile() = File(filesDir, "chat_history.jsonl")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this)
        root.setBackgroundColor(BG)

        circuit = CircuitBgView(this)
        circuit.isGlowing = { hud.active || chatMode }
        root.addView(circuit, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val content = LinearLayout(this)
        content.orientation = LinearLayout.VERTICAL
        content.gravity = Gravity.CENTER_HORIZONTAL
        content.setPadding(0, dp(96), 0, 0)

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
        hudLp.setMargins(dp(76), dp(16), dp(76), 0)
        hud.setOnClickListener {
            if (chatMode) {
                stopChat("কথোপকথন বন্ধ। আবার শুরু করতে গোলকে চাপুন।")
            } else {
                chatMode = true
                silent = 0
                micTap()
            }
        }
        hud.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateFocus() }

        status = TextView(this)
        status.text = "গোলকে একবার চাপুন, তারপর স্বাভাবিকভাবে কথা বলুন।"
        status.textSize = 13f
        status.setTextColor(SOFT)
        status.gravity = Gravity.CENTER
        status.maxLines = 3
        status.minHeight = dp(24)
        status.setPadding(dp(24), dp(6), dp(24), dp(6))

        chatBox = LinearLayout(this)
        chatBox.orientation = LinearLayout.VERTICAL
        chatBox.setPadding(dp(12), dp(4), dp(12), dp(16))
        chatScroll = ScrollView(this)
        chatScroll.isVerticalScrollBarEnabled = false
        chatScroll.addView(chatBox)

        content.addView(title)
        content.addView(sub)
        content.addView(hud, hudLp)
        content.addView(status, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        content.addView(chatScroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(content, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val menuBtn = TextView(this)
        menuBtn.text = "☰"
        menuBtn.textSize = 28f
        menuBtn.setTextColor(CYAN)
        menuBtn.setPadding(dp(16), dp(8), dp(16), dp(8))
        val mLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        mLp.topMargin = dp(36)
        menuBtn.setOnClickListener { openDrawer() }
        root.addView(menuBtn, mLp)

        val avatar = FrameLayout(this)
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setStroke(dp(2), CYAN)
        ring.setColor(PANEL)
        avatar.background = ring
        avatar.clipToOutline = true
        avatarEmoji = TextView(this)
        avatarEmoji.text = "👤"
        avatarEmoji.textSize = 20f
        avatarEmoji.gravity = Gravity.CENTER
        avatarImg = ImageView(this)
        avatarImg.scaleType = ImageView.ScaleType.CENTER_CROP
        avatar.addView(avatarEmoji, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        avatar.addView(avatarImg, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val pLp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
        pLp.topMargin = dp(42)
        pLp.rightMargin = dp(16)
        avatar.setOnClickListener { openProfile() }
        root.addView(avatar, pLp)

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

        buildHistoryPage()
        root.addView(historyPage, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        setContentView(root)
        refreshAvatar()
        tts = TextToSpeech(this, this)
    }

    private fun updateFocus() {
        val a = IntArray(2)
        val b = IntArray(2)
        hud.getLocationInWindow(a)
        circuit.getLocationInWindow(b)
        circuit.focusX = a[0] - b[0] + hud.width / 2f
        circuit.focusY = a[1] - b[1] + hud.height / 2f
        circuit.invalidate()
    }

    // ---------- মেনু ----------
    private fun buildDrawer() {
        val head = TextView(this)
        head.text = "মেনু"
        head.textSize = 22f
        head.setTextColor(CYAN)
        head.setPadding(0, 0, 0, dp(16))
        drawer.addView(head)

        item("🕘", "কথোপকথনের ইতিহাস") { openHistory() }
        item("✨", "নতুন কথোপকথন") {
            history.clear()
            chatBox.removeAllViews()
            status.text = "নতুন কথোপকথন শুরু। গোলকে চাপুন।"
        }
        item("🌐", "শোনার ভাষা") { showLangDialog() }
        item("🔊", "ক্লোন ভয়েস (Fish Audio)") { showFishDialog() }
        item("🔑", "API Key") { showKeyDialog() }
        item("🔒", "অনুমতি") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        item("ℹ️", "অ্যাপ সম্পর্কে") {
            val m = prefs().getString("model", "") ?: ""
            darkDialog()
                .setTitle("J.A.R.V.I.S")
                .setMessage("আপনার ব্যক্তিগত সহকারী\nসংস্করণ 2.6\nAI মডেল: " + (if (m.isEmpty()) "এখনও বাছা হয়নি" else m))
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
            historyPage.visibility == View.VISIBLE -> closeHistory()
            profilePage.visibility == View.VISIBLE -> closeProfile()
            scrim.visibility == View.VISIBLE -> closeDrawer()
            else -> super.onBackPressed()
        }
    }

    private fun darkDialog() = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    // ---------- চ্যাট বুদবুদ ও ইতিহাস ----------
    // role: 0 = আপনি, 1 = জার্ভিস, 2 = সিস্টেম বার্তা
    private fun bubbleView(role: Int, text: String, ts: Long): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.topMargin = dp(4)
        lp.bottomMargin = dp(4)
        val maxW = (resources.displayMetrics.widthPixels * 0.76f).toInt()

        if (role == 2) {
            lp.gravity = Gravity.CENTER_HORIZONTAL
            val t = TextView(this)
            t.text = text
            t.textSize = 12.5f
            t.setTextColor(Color.parseColor("#FFB4B4"))
            t.gravity = Gravity.CENTER
            t.maxWidth = (resources.displayMetrics.widthPixels * 0.9f).toInt()
            val sh = GradientDrawable()
            sh.cornerRadius = dp(12).toFloat()
            sh.setColor(Color.parseColor("#CC1A0E10"))
            t.background = sh
            t.setPadding(dp(12), dp(8), dp(12), dp(8))
            wrap.addView(t)
            wrap.layoutParams = lp
            return wrap
        }

        val user = role == 0
        lp.gravity = if (user) Gravity.END else Gravity.START
        val t = TextView(this)
        t.text = text
        t.textSize = 15f
        t.setTextColor(Color.WHITE)
        t.maxWidth = maxW
        val big = dp(16).toFloat()
        val small = dp(4).toFloat()
        val sh = GradientDrawable()
        if (user) {
            sh.cornerRadii = floatArrayOf(big, big, big, big, small, small, big, big)
            sh.setColor(Color.parseColor("#E6103A52"))
            sh.setStroke(dp(1), Color.parseColor("#7000E5FF"))
        } else {
            sh.cornerRadii = floatArrayOf(big, big, big, big, big, big, small, small)
            sh.setColor(Color.parseColor("#E60B141C"))
            sh.setStroke(dp(1), Color.parseColor("#507FDBE8"))
        }
        t.background = sh
        t.setPadding(dp(14), dp(10), dp(14), dp(10))

        val time = TextView(this)
        time.text = timeFmt.format(Date(ts))
        time.textSize = 10f
        time.setTextColor(Color.parseColor("#7C97A3"))
        time.setPadding(dp(6), dp(2), dp(6), 0)
        val tlp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tlp.gravity = if (user) Gravity.END else Gravity.START

        wrap.addView(t)
        wrap.addView(time, tlp)
        wrap.layoutParams = lp
        return wrap
    }

    private fun saveMsg(role: Int, text: String, ts: Long) {
        try {
            val line = JSONObject()
                .put("t", ts)
                .put("r", if (role == 0) "u" else "j")
                .put("m", text)
                .toString()
            histFile().appendText(line + "\n")
        } catch (e: Exception) {
        }
    }

    private fun loadSaved(): List<Triple<Int, String, Long>> {
        val out = ArrayList<Triple<Int, String, Long>>()
        try {
            val f = histFile()
            if (!f.exists()) return out
            for (line in f.readLines()) {
                if (line.isBlank()) continue
                try {
                    val j = JSONObject(line)
                    out.add(Triple(if (j.optString("r") == "u") 0 else 1, j.optString("m"), j.optLong("t")))
                } catch (e: Exception) {
                }
            }
        } catch (e: Exception) {
        }
        return out
    }

    private fun addMsg(role: Int, text: String) {
        val ts = System.currentTimeMillis()
        if (role != 2) saveMsg(role, text, ts)
        chatBox.addView(bubbleView(role, text, ts))
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun note(msg: String) = addMsg(2, msg)

    private fun err(msg: String) {
        chatMode = false
        hud.active = false
        status.text = ""
        addMsg(2, msg)
    }

    private fun buildHistoryPage() {
        historyPage = FrameLayout(this)
        historyPage.setBackgroundColor(Color.parseColor("#F5050B12"))
        historyPage.isClickable = true
        historyPage.visibility = View.GONE

        histBox = LinearLayout(this)
        histBox.orientation = LinearLayout.VERTICAL
        histBox.setPadding(dp(14), dp(96), dp(14), dp(24))
        histScroll = ScrollView(this)
        histScroll.addView(histBox)
        historyPage.addView(histScroll, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val back = TextView(this)
        back.text = "‹ BACK"
        back.setTextColor(CYAN)
        back.textSize = 16f
        mono(back)
        back.setPadding(dp(16), dp(10), dp(16), dp(10))
        val bLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        bLp.topMargin = dp(38)
        back.setOnClickListener { closeHistory() }
        historyPage.addView(back, bLp)

        val head = TextView(this)
        head.text = "// HISTORY"
        head.setTextColor(CYAN)
        head.textSize = 16f
        head.letterSpacing = 0.2f
        mono(head)
        val hLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.CENTER_HORIZONTAL)
        hLp.topMargin = dp(48)
        historyPage.addView(head, hLp)

        val del = TextView(this)
        del.text = "🗑 মুছুন"
        del.setTextColor(RED)
        del.textSize = 14f
        del.setPadding(dp(16), dp(10), dp(16), dp(10))
        val dLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END)
        dLp.topMargin = dp(40)
        del.setOnClickListener {
            darkDialog()
                .setTitle("ইতিহাস মুছুন")
                .setMessage("সব সেভ করা কথোপকথন চিরতরে মুছে যাবে। নিশ্চিত?")
                .setPositiveButton("হ্যাঁ, মুছুন") { _, _ ->
                    histFile().delete()
                    fillHistory()
                    toast("ইতিহাস মুছে ফেলা হয়েছে")
                }
                .setNegativeButton("না", null).show()
        }
        historyPage.addView(del, dLp)
    }

    private fun fillHistory() {
        histBox.removeAllViews()
        var all = loadSaved()
        if (all.isEmpty()) {
            val e = TextView(this)
            e.text = "এখনও কোনো কথোপকথন সেভ হয়নি।"
            e.setTextColor(SOFT)
            e.textSize = 15f
            e.gravity = Gravity.CENTER
            e.setPadding(0, dp(60), 0, 0)
            histBox.addView(e)
            return
        }
        if (all.size > 500) {
            all = all.takeLast(500)
            val m = TextView(this)
            m.text = "সর্বশেষ ৫০০টা বার্তা দেখানো হচ্ছে"
            m.setTextColor(Color.parseColor("#7C97A3"))
            m.textSize = 11f
            m.gravity = Gravity.CENTER
            histBox.addView(m)
        }
        var lastDay = ""
        for (x in all) {
            val day = dateFmt.format(Date(x.third))
            if (day != lastDay) {
                lastDay = day
                val h = TextView(this)
                h.text = day
                h.setTextColor(SOFT)
                h.textSize = 12f
                h.gravity = Gravity.CENTER
                val sh = GradientDrawable()
                sh.cornerRadius = dp(12).toFloat()
                sh.setColor(Color.parseColor("#CC0A1A24"))
                h.background = sh
                h.setPadding(dp(14), dp(6), dp(14), dp(6))
                val hl = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                hl.gravity = Gravity.CENTER_HORIZONTAL
                hl.topMargin = dp(14)
                hl.bottomMargin = dp(6)
                histBox.addView(h, hl)
            }
            histBox.addView(bubbleView(x.first, x.second, x.third))
        }
    }

    private fun openHistory() {
        fillHistory()
        val w = resources.displayMetrics.widthPixels.toFloat()
        historyPage.translationX = w
        historyPage.visibility = View.VISIBLE
        historyPage.animate().translationX(0f).setDuration(240).start()
        histScroll.post { histScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun closeHistory() {
        val w = resources.displayMetrics.widthPixels.toFloat()
        historyPage.animate().translationX(w).setDuration(220).withEndAction {
            historyPage.visibility = View.GONE
        }.start()
    }

    // ---------- প্রোফাইল পেজ ----------
    private fun mono(t: TextView) { t.typeface = Typeface.MONOSPACE }

    private fun field(parent: LinearLayout, label: String, hint: String, type: Int): EditText {
        val l = TextView(this)
        l.text = label
        l.setTextColor(SOFT)
        l.textSize = 11f
        l.letterSpacing = 0.2f
        mono(l)
        l.setPadding(dp(4), dp(18), 0, dp(2))
        val e = EditText(this)
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
        saveLp.topMargin = dp(28)
        col.addView(save, saveLp)

        val sv = ScrollView(this)
        sv.isFillViewport = true
        sv.addView(col)
        profilePage.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

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

        val out = boxButton("⏻  LOG OUT", RED)
        out.setOnClickListener { confirmLogout() }
        val oLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM)
        oLp.setMargins(dp(24), 0, dp(24), dp(28))
        profilePage.addView(out, oLp)
    }

    private fun refreshAvatar() {
        val f = photoFile()
        if (f.exists()) {
            avatarImg.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
            avatarEmoji.visibility = View.GONE
        } else {
            avatarImg.setImageDrawable(null)
            avatarEmoji.visibility = View.VISIBLE
        }
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
        refreshAvatar()
        val w = resources.displayMetrics.widthPixels.toFloat()
        profilePage.animate().translationX(w).setDuration(220).withEndAction {
            profilePage.visibility = View.GONE
        }.start()
    }

    private fun confirmLogout() {
        darkDialog()
            .setTitle("LOG OUT")
            .setMessage("আপনার প্রোফাইলের তথ্য ও ছবি এই ফোন থেকে মুছে যাবে। (কথোপকথনের ইতিহাস, API Key ও ক্লোন ভয়েস সেটিং থাকবে।) নিশ্চিত?")
            .setPositiveButton("হ্যাঁ, মুছুন") { _, _ ->
                prefs().edit().remove("username").remove("fullname").remove("email").remove("phone").apply()
                photoFile().delete()
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
                refreshAvatar()
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
        key.hint = "AQ... বা AIza..."
        key.setTextColor(Color.WHITE)
        key.setHintTextColor(Color.GRAY)
        box.addView(info)
        box.addView(key)
        val has = !prefs().getString("key", "").isNullOrEmpty()
        darkDialog().setTitle(if (has) "🔑 API Key (আগেরটা সেভ আছে)" else "🔑 API Key").setView(box)
            .setPositiveButton("সেভ") { _, _ ->
                val k = key.text.toString().trim()
                if (k.isNotEmpty()) {
                    prefs().edit().putString("key", k).remove("model").apply()
                    toast("Key সেভ হয়েছে")
                }
            }
            .setNegativeButton("বন্ধ", null).show()
    }

    private fun showFishDialog() {
        val p = prefs()
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(8), dp(24), 0)
        val info = TextView(this)
        info.text = "Fish Audio-র API Key ও ক্লোন ভয়েসের Reference ID বসান। উত্তরের লেখা Fish Audio-র সার্ভারে যাবে। শুধু নিজের বা অনুমতি পাওয়া কণ্ঠ ব্যবহার করুন।"
        info.setTextColor(Color.WHITE)
        val k = EditText(this)
        k.hint = "Fish Audio API Key"
        k.setSingleLine(true)
        k.setTextColor(Color.WHITE)
        k.setHintTextColor(Color.GRAY)
        val id = EditText(this)
        id.hint = "Reference ID"
        id.setSingleLine(true)
        id.setTextColor(Color.WHITE)
        id.setHintTextColor(Color.GRAY)
        box.addView(info)
        box.addView(k)
        box.addView(id)
        val has = !p.getString("fish_key", "").isNullOrEmpty() && !p.getString("fish_id", "").isNullOrEmpty()
        val on = p.getBoolean("fish_on", true)
        val title = if (has) (if (on) "🔊 ক্লোন ভয়েস (চালু)" else "🔊 ক্লোন ভয়েস (বন্ধ)") else "🔊 ক্লোন ভয়েস"
        val b = darkDialog().setTitle(title).setView(box)
            .setPositiveButton("সেভ ও চালু") { _, _ ->
                val kk = k.text.toString().trim()
                val ii = id.text.toString().trim()
                val e = prefs().edit()
                if (kk.isNotEmpty()) e.putString("fish_key", kk)
                if (ii.isNotEmpty()) e.putString("fish_id", ii)
                e.putBoolean("fish_on", true)
                e.apply()
                toast("সেভ হয়েছে। এবার জার্ভিসকে কিছু জিজ্ঞেস করুন।")
            }
            .setNegativeButton("বন্ধ", null)
        if (has) {
            b.setNeutralButton(if (on) "ক্লোন বন্ধ করুন" else "ক্লোন চালু করুন") { _, _ ->
                prefs().edit().putBoolean("fish_on", !on).apply()
                toast(if (on) "ফোনের সাধারণ ভয়েসে ফিরলাম" else "ক্লোন ভয়েস চালু")
            }
        }
        b.show()
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
    private fun stopVoice() {
        voiceGen++
        tts.stop()
        try { player?.release() } catch (e: Exception) { }
        player = null
    }

    private fun stopChat(msg: String) {
        chatMode = false
        pendingAction = null
        stopVoice()
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
        val p = pendingAction
        pendingAction = null
        if (p != null) {
            val left = runAction(p)
            if (left) {
                chatMode = false
            } else if (chatMode) {
                hud.postDelayed({ if (chatMode) listen() }, 500)
            }
            return
        }
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

    private fun speakReply(lang: String, text: String): Boolean {
        val p = prefs()
        val fk = p.getString("fish_key", "").orEmpty().trim()
        val fid = p.getString("fish_id", "").orEmpty().trim()
        if (fk.isNotEmpty() && fid.isNotEmpty() && p.getBoolean("fish_on", true)) {
            speakFish(lang, text, fk, fid)
            return true
        }
        return speakIn(lang, text)
    }

    private fun speakFish(lang: String, text: String, fk: String, fid: String) {
        val gen = voiceGen
        Thread {
            var code = 0
            var ok = false
            val f = File(cacheDir, "jarvis_voice.mp3")
            try {
                val body = JSONObject()
                    .put("text", text)
                    .put("reference_id", fid)
                    .put("format", "mp3")
                for (m in listOf("s2-pro", "s2.1-pro")) {
                    val conn = URL("https://api.fish.audio/v1/tts").openConnection() as HttpURLConnection
                    conn.requestMethod = "POST"
                    conn.connectTimeout = 15000
                    conn.readTimeout = 40000
                    conn.setRequestProperty("Authorization", "Bearer " + fk)
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.setRequestProperty("model", m)
                    conn.doOutput = true
                    conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                    code = conn.responseCode
                    if (code in 200..299) {
                        conn.inputStream.use { ins -> f.outputStream().use { ins.copyTo(it) } }
                        ok = true
                        break
                    }
                    if (code == 401 || code == 402 || code == 429) break
                }
            } catch (e: Exception) {
                code = -1
            }
            runOnUiThread {
                if (gen == voiceGen) {
                    if (ok && f.length() > 0) playFile(f, lang, text) else fishFailed(code, lang, text)
                }
            }
        }.start()
    }

    private fun playFile(f: File, lang: String, text: String) {
        try {
            try { player?.release() } catch (e: Exception) { }
            val mp = MediaPlayer()
            player = mp
            mp.setDataSource(f.absolutePath)
            mp.setOnPreparedListener { hud.active = true; it.start() }
            mp.setOnCompletionListener { hud.active = false; afterSpeech() }
            mp.setOnErrorListener { _, _, _ -> fishFailed(-2, lang, text); true }
            mp.prepareAsync()
        } catch (e: Exception) {
            fishFailed(-2, lang, text)
        }
    }

    private fun fishFailed(code: Int, lang: String, text: String) {
        val why = when (code) {
            401 -> "Fish Audio API Key ঠিক নয়"
            402 -> "Fish Audio-র ক্রেডিট শেষ"
            404, 422 -> "Reference ID বা মডেলে সমস্যা"
            429 -> "Fish Audio-র সীমা শেষ"
            else -> "ক্লোন ভয়েস কাজ করেনি"
        }
        note("($why, কোড $code। ফোনের ভয়েসে পড়ছি।)")
        if (!speakIn(lang, text)) afterSpeech()
    }

    // ---------- ফোনের কাজ ----------
    private fun isNumber(s: String): Boolean =
        s.replace(" ", "").replace("-", "").matches(Regex("^\\+?[0-9]{5,15}$"))

    private fun findNumber(q: String): String? {
        if (isNumber(q)) return q.replace(" ", "").replace("-", "")
        try {
            val c = contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER),
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                arrayOf("%" + q + "%"),
                null
            )
            c?.use { if (it.moveToFirst()) return it.getString(0) }
        } catch (e: Exception) {
        }
        return null
    }

    private fun openApp(name: String): Boolean {
        val pm = packageManager
        val main = Intent(Intent.ACTION_MAIN)
        main.addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = pm.queryIntentActivities(main, 0)
        val q = name.lowercase()
        for (a in apps) {
            val label = a.loadLabel(pm).toString().lowercase()
            if (label == q || label.contains(q) || q.contains(label)) {
                val launch = pm.getLaunchIntentForPackage(a.activityInfo.packageName)
                if (launch != null) {
                    startActivity(launch)
                    return true
                }
            }
        }
        note("“$name” নামে কোনো অ্যাপ খুঁজে পাইনি।")
        return false
    }

    private fun setTorch(on: Boolean) {
        val cm = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        var id: String? = null
        for (i in cm.cameraIdList) {
            if (cm.getCameraCharacteristics(i).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true) {
                id = i
                break
            }
        }
        if (id == null) {
            note("এই ফোনে টর্চ পাওয়া যায়নি।")
            return
        }
        cm.setTorchMode(id, on)
        torchOn = on
    }

    // সত্য ফেরত দিলে বোঝায় অন্য অ্যাপ খুলেছে
    private fun runAction(o: JSONObject): Boolean {
        val a = o.optString("action", "none")
        val arg = o.optString("arg", "").trim()
        val arg2 = o.optString("arg2", "").trim()
        try {
            if ((a == "call_contact" || a == "sms") && !isNumber(arg) &&
                checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                pendingPerm = o
                requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 2)
                return false
            }
            when (a) {
                "open_app" -> return openApp(arg)
                "dial" -> {
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(arg))))
                    return true
                }
                "call_contact" -> {
                    val n = findNumber(arg)
                    if (n == null) {
                        note("“$arg” নামে কোনো কন্টাক্ট পাইনি। ফোনে নামটা কীভাবে সেভ আছে বলুন।")
                        return false
                    }
                    startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + Uri.encode(n))))
                    return true
                }
                "sms" -> {
                    val n = findNumber(arg)
                    if (n == null) {
                        note("“$arg” নামে কোনো কন্টাক্ট বা নম্বর পাইনি।")
                        return false
                    }
                    val i = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(n)))
                    i.putExtra("sms_body", arg2)
                    startActivity(i)
                    return true
                }
                "torch" -> {
                    setTorch(arg.lowercase() != "off")
                    return false
                }
                "alarm" -> {
                    val parts = arg.split(":")
                    val h = parts[0].trim().toInt()
                    val m = if (parts.size > 1) parts[1].trim().toInt() else 0
                    val i = Intent(AlarmClock.ACTION_SET_ALARM)
                    i.putExtra(AlarmClock.EXTRA_HOUR, h)
                    i.putExtra(AlarmClock.EXTRA_MINUTES, m)
                    i.putExtra(AlarmClock.EXTRA_MESSAGE, if (arg2.isEmpty()) "Jarvis" else arg2)
                    i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    startActivity(i)
                    return false
                }
                "timer" -> {
                    val i = Intent(AlarmClock.ACTION_SET_TIMER)
                    i.putExtra(AlarmClock.EXTRA_LENGTH, arg.toInt())
                    i.putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                    startActivity(i)
                    return false
                }
                "search" -> {
                    val u = "https://www.google.com/search?q=" + URLEncoder.encode(arg, "UTF-8")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    return true
                }
                "youtube" -> {
                    val u = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(arg, "UTF-8")
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)))
                    return true
                }
                "maps" -> {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(arg))))
                    return true
                }
            }
        } catch (e: ActivityNotFoundException) {
            note("এই কাজের জন্য ফোনে কোনো অ্যাপ পাওয়া যায়নি।")
        } catch (e: Exception) {
            note("কাজটা করা গেল না। (${e.javaClass.simpleName})")
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
        val granted = res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED
        if (code == 2) {
            val p = pendingPerm
            pendingPerm = null
            if (granted && p != null) {
                runAction(p)
            } else {
                note("কন্টাক্টের অনুমতি ছাড়া নাম দিয়ে খুঁজতে পারব না। নম্বর বললেও চলবে।")
            }
            return
        }
        if (granted) {
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
        stopVoice()
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
                    status.text = "মাইক বুঝতে পারেনি, আবার গোলকে চেপে বলুন (মাইক এরর কোড $e)"
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
    private fun callGemini(model: String, key: String, body: JSONObject): Pair<Int, String> {
        val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent")
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
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        return Pair(code, text)
    }

    private fun listModels(key: String): String {
        return try {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models?pageSize=100")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("x-goog-api-key", key)
            val code = conn.responseCode
            if (code !in 200..299) return "(তালিকা আনা যায়নি, কোড $code)"
            val text = conn.inputStream.bufferedReader().use { it.readText() }
            val arr = JSONObject(text).getJSONArray("models")
            val names = ArrayList<String>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val methods = o.optJSONArray("supportedGenerationMethods")
                var ok = false
                if (methods != null) {
                    for (k in 0 until methods.length()) {
                        if (methods.getString(k) == "generateContent") ok = true
                    }
                }
                val n = o.getString("name").removePrefix("models/")
                if (ok && n.startsWith("gemini")) names.add(n)
            }
            if (names.isEmpty()) "(কোনো মডেল পাওয়া যায়নি)" else names.take(15).joinToString("\n")
        } catch (e: Exception) {
            "(তালিকা আনা যায়নি)"
        }
    }

    private fun askAi(userText: String) {
        val key = prefs().getString("key", "")?.trim().orEmpty()
        addMsg(0, userText)
        if (key.isEmpty()) {
            err("AI চালাতে আগে মেনু → API Key-তে Key বসান।")
            showKeyDialog()
            return
        }
        hud.active = true
        status.text = "ভাবছি…"
        val p = prefs()
        var name = p.getString("fullname", "").orEmpty()
        if (name.isEmpty()) name = p.getString("username", "").orEmpty()
        val sys = if (name.isEmpty()) SYS else SYS + "\nব্যবহারকারীর নাম: " + name
        val saved = p.getString("model", "").orEmpty()

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
                        .put("responseMimeType", "application/json"))

                val order = ArrayList<String>()
                if (saved.isNotEmpty()) order.add(saved)
                for (m in MODELS) if (!order.contains(m)) order.add(m)

                var code = 0
                var text = ""
                var used = ""
                for (m in order) {
                    val r = callGemini(m, key, body)
                    code = r.first
                    text = r.second
                    used = m
                    if (code != 404) break
                }

                if (code in 200..299) {
                    val out = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    val clean = out.replace("```json", "").replace("```", "").trim()
                    val j = try { JSONObject(clean) } catch (e: Exception) {
                        JSONObject().put("lang", "bn").put("reply", out).put("action", "none")
                    }
                    val reply = j.optString("reply", out)
                    val lang = j.optString("lang", "bn")
                    val action = j.optString("action", "none")
                    history.add(Pair("user", userText))
                    history.add(Pair("model", reply))
                    prefs().edit().putString("model", used).apply()
                    runOnUiThread {
                        status.text = ""
                        if (reply.isNotBlank()) addMsg(1, reply)
                        val hasAction = action.isNotEmpty() && action != "none"
                        if (hasAction) pendingAction = j
                        val spoke = reply.isNotBlank() && speakReply(lang, reply)
                        if (!spoke) {
                            hud.active = false
                            if (reply.isNotBlank()) {
                                note("(এই ভাষার ভয়েস ফোনে নেই। Settings → Text-to-speech → Google Speech Services থেকে ডাউনলোড করুন।)")
                            }
                            chatMode = false
                            val pa = pendingAction
                            pendingAction = null
                            if (pa != null) runAction(pa)
                        }
                    }
                } else if (code == 404) {
                    prefs().edit().remove("model").apply()
                    val list = listModels(key)
                    runOnUiThread {
                        err("AI মডেল পাওয়া যায়নি (সার্ভার কোড 404)।\n\nআপনার Key যে মডেলগুলো ব্যবহার করতে পারে:\n$list\n\nএই তালিকার স্ক্রিনশট আমাকে পাঠান।")
                    }
                } else {
                    runOnUiThread { showHttpError(code) }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    err("ইন্টারনেট বা সংযোগে সমস্যা। নেট চালু আছে কি না দেখুন।\n(${e.javaClass.simpleName})")
                }
            }
        }.start()
    }

    private fun showHttpError(code: Int) {
        val msg = when (code) {
            400, 403 -> "API Key ঠিক নয় বা অনুমতি নেই (সার্ভার কোড $code)। মেনু → API Key-তে আবার বসান।"
            429 -> "ফ্রি সীমা শেষ। কিছুক্ষণ পরে আবার চেষ্টা করুন (সার্ভার কোড 429)।"
            else -> "সার্ভারে সমস্যা হয়েছে (সার্ভার কোড $code)। আবার চেষ্টা করুন।"
        }
        err(msg)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        try { player?.release() } catch (e: Exception) { }
        tts.shutdown()
        super.onDestroy()
    }
}
