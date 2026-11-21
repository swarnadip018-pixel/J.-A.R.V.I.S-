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
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private val BG = Color.parseColor("#050B12")
    private val CYAN = Color.parseColor("#00E5FF")
    private val SOFT = Color.parseColor("#7FDBE8")
    private val MODEL = "gemini-2.5-flash"

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
    private var recognizer: SpeechRecognizer? = null
    private val history = ArrayList<Pair<String, String>>()

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("jarvis", Context.MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = BG
        window.navigationBarColor = BG

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
        hud.setOnClickListener { micTap() }

        status = TextView(this)
        status.text = "মাঝের গোলকে চেপে কথা বলুন"
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
        ring.setColor(Color.parseColor("#0A1A24"))
        profile.background = ring
        val pLp = FrameLayout.LayoutParams(dp(44), dp(44), Gravity.TOP or Gravity.END)
        pLp.topMargin = dp(42)
        pLp.rightMargin = dp(16)
        profile.setOnClickListener { showProfile() }
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
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("J.A.R.V.I.S")
                .setMessage("আপনার ব্যক্তিগত সহকারী\nসংস্করণ 2.0 (AI যুক্ত)")
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
        if (scrim.visibility == View.VISIBLE) closeDrawer() else super.onBackPressed()
    }

    private fun darkDialog() = AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    private fun showProfile() {
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(dp(24), dp(8), dp(24), 0)
        val info = TextView(this)
        info.text = "আপনার নাম লিখে রাখুন। জার্ভিস আপনাকে এই নামে ডাকবে।"
        info.setTextColor(Color.WHITE)
        val name = EditText(this)
        name.setText(prefs().getString("name", ""))
        name.hint = "আপনার নাম"
        name.setTextColor(Color.WHITE)
        name.setHintTextColor(Color.GRAY)
        box.addView(info)
        box.addView(name)
        darkDialog().setTitle("👤 আমার তথ্য").setView(box)
            .setPositiveButton("সেভ") { _, _ ->
                prefs().edit().putString("name", name.text.toString().trim()).apply()
            }
            .setNegativeButton("বন্ধ", null).show()
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

    // ---------- ভয়েস আউটপুট ----------
    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) {
            tts.setPitch(0.8f)
            tts.setSpeechRate(0.92f)
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
        if (res.isNotEmpty() && res[0] == PackageManager.PERMISSION_GRANTED) listen()
        else status.text = "মাইকের অনুমতি ছাড়া শুনতে পারব না"
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
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
                askAi(t)
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
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, prefs().getString("inlang", "bn-BD"))
        recognizer?.startListening(i)
    }

    // ---------- AI ব্রেইন ----------
    private fun askAi(userText: String) {
        val key = prefs().getString("key", "")?.trim().orEmpty()
        if (key.isEmpty()) {
            hud.active = false
            status.text = "আপনি বললেন: $userText\n\nAI চালাতে আগে মেনু → API Key-তে Key বসান।"
            showKeyDialog()
            return
        }
        hud.active = true
        status.text = "আপনি: $userText\n\nভাবছি…"
        val name = prefs().getString("name", "").orEmpty()
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
                        hud.active = false
                        status.text = "আপনি: $userText\n\n$reply"
                        if (!speakIn(lang, reply)) {
                            status.text = status.text.toString() + "\n\n(এই ভাষার ভয়েস ফোনে নেই। Settings → Text-to-speech → Google Speech Services থেকে ডাউনলোড করুন।)"
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    hud.active = false
                    status.text = "ইন্টারনেট বা সংযোগে সমস্যা। নেট চালু আছে কি না দেখুন।\n(${e.javaClass.simpleName})"
                }
            }
        }.start()
    }

    private fun showHttpError(code: Int) {
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
