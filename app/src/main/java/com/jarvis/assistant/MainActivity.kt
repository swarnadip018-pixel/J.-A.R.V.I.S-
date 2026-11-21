package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val BG = Color.parseColor("#050B12")
    private val CYAN = Color.parseColor("#00E5FF")
    private val SOFT = Color.parseColor("#7FDBE8")

    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private lateinit var hud: HudView
    private lateinit var drawer: LinearLayout
    private lateinit var scrim: View
    private var recognizer: SpeechRecognizer? = null
    private var ttsReady = false

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

        val root = FrameLayout(this)
        root.setBackgroundColor(BG)

        // মাঝের অংশ
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
        hudLp.setMargins(dp(28), dp(36), dp(28), 0)
        hud.setOnClickListener { micTap() }

        status = TextView(this)
        status.text = "মাঝের গোলকে চেপে কথা বলুন"
        status.textSize = 16f
        status.setTextColor(Color.WHITE)
        status.gravity = Gravity.CENTER
        status.setPadding(dp(24), dp(24), dp(24), 0)

        content.addView(title)
        content.addView(sub)
        content.addView(hud, hudLp)
        content.addView(status)
        root.addView(content)

        // বামে ওপরে মেনু বোতাম
        val menuBtn = TextView(this)
        menuBtn.text = "☰"
        menuBtn.textSize = 28f
        menuBtn.setTextColor(CYAN)
        menuBtn.setPadding(dp(16), dp(8), dp(16), dp(8))
        val mLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        mLp.topMargin = dp(36)
        menuBtn.setOnClickListener { openDrawer() }
        root.addView(menuBtn, mLp)

        // ডানে ওপরে প্রোফাইল আইকন
        val profile = TextView(this)
        profile.text = "👤"
        profile.textSize = 20f
        profile.gravity = Gravity.CENTER
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setStroke(dp(2), CYAN)
        ring.setColor(Color.parseColor("#0A1A24"))
        profile.background = ring
        val pLp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
        pLp.topMargin = dp(42)
        pLp.rightMargin = dp(16)
        profile.setOnClickListener { showProfile() }
        root.addView(profile, pLp)

        // মেনুর পর্দা ও ড্রয়ার
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

        setContentView(root)
        tts = TextToSpeech(this, this)
    }

    private fun buildDrawer() {
        val head = TextView(this)
        head.text = "মেনু"
        head.textSize = 22f
        head.setTextColor(CYAN)
        head.setPadding(0, 0, 0, dp(16))
        drawer.addView(head)

        item("💬", "কথোপকথনের ইতিহাস") { soon() }
        item("🌐", "ভাষা") { soon() }
        item("🔊", "ভয়েস সেটিংস") { soon() }
        item("🔑", "API Key") { soon() }
        item("🔒", "অনুমতি") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
        }
        item("ℹ️", "অ্যাপ সম্পর্কে") {
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("J.A.R.V.I.S")
                .setMessage("আপনার ব্যক্তিগত সহকারী\nসংস্করণ 1.0")
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

    private fun soon() = Toast.makeText(this, "শীঘ্রই আসছে", Toast.LENGTH_SHORT).show()

    private fun openDrawer() {
        scrim.visibility = View.VISIBLE
        drawer.animate().translationX(0f).setDuration(220).start()
    }

    private fun closeDrawer() {
        scrim.visibility = View.GONE
        drawer.animate().translationX(-dp(290).toFloat()).setDuration(200).start()
    }

    override fun onBackPressed() {
        if (scrim.visibility == View.VISIBLE) closeDrawer() else super.onBackPressed()
    }

    private fun showProfile() {
        val prefs = getSharedPreferences("jarvis", Context.MODE_PRIVATE)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(8), dp(24), 0)
        val info = TextView(this)
        info.text = "আপনার নাম লিখে রাখুন। জার্ভিস আপনাকে এই নামে ডাকবে।"
        info.setTextColor(Color.WHITE)
        val name = EditText(this)
        name.setText(prefs.getString("name", ""))
        name.hint = "আপনার নাম"
        name.setTextColor(Color.WHITE)
        name.setHintTextColor(Color.GRAY)
        box.addView(info)
        box.addView(name)
        AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
            .setTitle("👤 আমার তথ্য")
            .setView(box)
            .setPositiveButton("সেভ") { _, _ ->
                prefs.edit().putString("name", name.text.toString().trim()).apply()
            }
            .setNegativeButton("বন্ধ", null).show()
    }

    // ---------- ভয়েস ----------
    override fun onInit(s: Int) {
        if (s != TextToSpeech.SUCCESS) return
        tts.setPitch(0.8f)
        tts.setSpeechRate(0.92f)
        var r = tts.setLanguage(Locale("bn", "BD"))
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            r = tts.setLanguage(Locale("bn", "IN"))
        }
        ttsReady = !(r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED)
    }

    private fun say(text: String) {
        if (ttsReady) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "u1")
    }

    private fun micTap() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            listen()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
        }
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, res: IntArray) {
        super.onRequestPermissionsResult(code, perms, res)
        if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) listen()
        else status.text = "মাইকের অনুমতি ছাড়া শুনতে পারব না"
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            status.text = "এই ফোনে ভয়েস চেনার সুবিধা নেই"
            return
        }
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle?) {
                hud.active = false
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                status.text = "আপনি বললেন: $t"
                say("আপনি বললেন: $t")
            }
            override fun onError(e: Int) {
                hud.active = false
                status.text = "বুঝতে পারিনি, আবার চেপে বলুন (কোড $e)"
            }
            override fun onReadyForSpeech(p: Bundle?) { hud.active = true; status.text = "শুনছি…" }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(p: Bundle?) {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "bn-BD")
        recognizer?.startListening(i)
    }

    override fun onDestroy() {
        recognizer?.destroy()
        tts.shutdown()
        super.onDestroy()
    }
}
