package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.util.Locale
import kotlin.math.min

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var store: Store
    private lateinit var phone: PhoneActions
    private lateinit var profile: ProfilePage
    private lateinit var historyPage: HistoryPage
    private lateinit var settings: SettingsPage
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
    private var recognizer: SpeechRecognizer? = null
    private var player: MediaPlayer? = null
    private var voiceGen = 0
    private val ctxHist = ArrayList<Pair<String, String>>()
    private var chatMode = false
    private var silent = 0
    private var pendingAction: JSONObject? = null
    private var pendingPerm: JSONObject? = null
    private var liveUser: ChatUi.Msg? = null
    private val mutedStreams = ArrayList<Int>()
    private val PICK_PHOTO = 77
    private val PICK_ICON = 78

    private fun dp(v: Int) = Ui.dp(this, v)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        L.bn = store.str("ui_lang") == "bn"
        phone = PhoneActions(this) { addSystem(it) }
        window.statusBarColor = Ui.BG
        window.navigationBarColor = Ui.BG
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val root = FrameLayout(this)
        root.setBackgroundColor(Ui.BG)

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
        title.setTextColor(Ui.CYAN)
        title.letterSpacing = 0.25f
        title.typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        title.gravity = Gravity.CENTER

        val sub = TextView(this)
        sub.text = "your personal assistant"
        sub.textSize = 14f
        sub.setTextColor(Ui.SOFT)
        sub.letterSpacing = 0.15f
        sub.gravity = Gravity.CENTER

        hud = HudView(this)
        Ui.quiet(hud)
        val hudLp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        hudLp.setMargins(dp(76), dp(16), dp(76), 0)
        hud.setOnClickListener {
            if (chatMode) {
                stopChat(L.t("stopped"))
            } else {
                chatMode = true
                silent = 0
                micTap()
            }
        }
        hud.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> updateFocus() }

        status = TextView(this)
        status.text = L.t("hint")
        status.textSize = 13f
        status.setTextColor(Ui.SOFT)
        status.setShadowLayer(6f, 0f, 0f, Color.BLACK)
        status.gravity = Gravity.CENTER
        status.maxLines = 3
        status.minHeight = dp(24)
        status.setPadding(dp(24), dp(6), dp(24), dp(6))

        chatBox = LinearLayout(this)
        chatBox.orientation = LinearLayout.VERTICAL
        chatBox.setPadding(dp(16), dp(4), dp(16), dp(16))
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
        menuBtn.setTextColor(Ui.CYAN)
        menuBtn.setPadding(dp(16), dp(8), dp(16), dp(8))
        Ui.quiet(menuBtn)
        val mLp = FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.START)
        mLp.topMargin = dp(36)
        menuBtn.setOnClickListener { openDrawer() }
        root.addView(menuBtn, mLp)

        val avatar = FrameLayout(this)
        val ring = GradientDrawable()
        ring.shape = GradientDrawable.OVAL
        ring.setStroke(dp(2), Ui.CYAN)
        ring.setColor(Ui.PANEL)
        avatar.background = ring
        avatar.clipToOutline = true
        Ui.quiet(avatar)
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
        avatar.setOnClickListener { profile.open() }
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

        profile = ProfilePage(this, store, { pickPhoto() }, { refreshAvatar() })
        root.addView(profile.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        historyPage = HistoryPage(this, store)
        root.addView(historyPage.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        settings = SettingsPage(this, store, SettingsActions(
            key = { Dialogs.key(this, store) },
            engine = { Dialogs.engine(this, store) { settings.refresh() } },
            voiceApi = { Dialogs.fish(this, store) },
            gemini = { Dialogs.geminiVoice(this, store) },
            access = { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            lang = { Dialogs.appLang(this, store) { recreate() } },
            icon = { Dialogs.iconInfo(this) { pickIcon() } }
        ))
        root.addView(settings.root, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

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

    private fun refreshAvatar() {
        val f = store.photoFile()
        if (f.exists()) {
            avatarImg.setImageBitmap(BitmapFactory.decodeFile(f.absolutePath))
            avatarEmoji.visibility = View.GONE
        } else {
            avatarImg.setImageDrawable(null)
            avatarEmoji.visibility = View.VISIBLE
        }
    }

    // ---------- ছবি বাছা (প্রোফাইল ও অ্যাপ আইকন) ----------
    private fun pickPhoto() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, L.t("choose_photo")), PICK_PHOTO)
    }

    private fun pickIcon() {
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        startActivityForResult(Intent.createChooser(i, L.t("choose_photo")), PICK_ICON)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (res != RESULT_OK || data?.data == null) return
        if (req == PICK_PHOTO) profile.setPhoto(data.data!!)
        if (req == PICK_ICON) makeShortcut(data.data!!)
    }

    private fun makeShortcut(uri: Uri) {
        try {
            val sm = getSystemService(ShortcutManager::class.java)
            if (sm == null || !sm.isRequestPinShortcutSupported) {
                addSystem(L.t("ic_unsupported"))
                return
            }
            val bounds = BitmapFactory.Options()
            bounds.inJustDecodeBounds = true
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var s = 1
            while (bounds.outWidth / s > 1024 || bounds.outHeight / s > 1024) s *= 2
            val opts = BitmapFactory.Options()
            opts.inSampleSize = s
            val bmp = contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            if (bmp == null) {
                addSystem(L.t("ic_fail"))
                return
            }
            val side = min(bmp.width, bmp.height)
            val sq = Bitmap.createBitmap(bmp, (bmp.width - side) / 2, (bmp.height - side) / 2, side, side)
            val icon = Bitmap.createScaledBitmap(sq, 432, 432, true)
            val launch = Intent(this, MainActivity::class.java)
            launch.action = Intent.ACTION_MAIN
            val info = ShortcutInfo.Builder(this, "jarvis_" + System.currentTimeMillis())
                .setShortLabel("Jarvis")
                .setIcon(Icon.createWithAdaptiveBitmap(icon))
                .setIntent(launch)
                .build()
            sm.requestPinShortcut(info, null)
            Toast.makeText(this, L.t("ic_ok"), Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            addSystem(L.t("ic_fail"))
        }
    }

    // ---------- মেনু ----------
    private fun buildDrawer() {
        val head = TextView(this)
        head.text = L.t("menu")
        head.textSize = 22f
        head.setTextColor(Ui.CYAN)
        head.setPadding(0, 0, 0, dp(12))
        drawer.addView(head)

        drawerItem(IconView.NEW_CHAT, L.t("new_chat")) {
            ctxHist.clear()
            chatBox.removeAllViews()
            status.text = L.t("new_started")
        }
        drawerItem(IconView.HISTORY, L.t("history")) { historyPage.open() }
        drawerItem(IconView.MIC, L.t("listen_lang")) { Dialogs.listenLang(this, store) }
        drawerItem(IconView.ABOUT, L.t("about")) {
            val m = store.str("model")
            AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("J.A.R.V.I.S")
                .setMessage(L.f("about_msg", if (m.isEmpty()) L.t("about_none") else m))
                .setPositiveButton(L.t("ok"), null).show()
        }
        drawer.addView(Rows.divider(this))
        drawerItem(IconView.GEAR, L.t("settings")) { settings.open() }
    }

    private fun drawerItem(icon: Int, label: String, onTap: () -> Unit) {
        drawer.addView(Rows.row(this, icon, label, null) {
            closeDrawer()
            onTap()
        })
    }

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
            historyPage.isOpen -> historyPage.close()
            profile.isOpen -> profile.close()
            settings.isOpen -> settings.close()
            scrim.visibility == View.VISIBLE -> closeDrawer()
            else -> super.onBackPressed()
        }
    }

    // ---------- চ্যাটের লেখা ----------
    private fun scrollDown() {
        chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }
    }

    private fun addSystem(msg: String) {
        chatBox.addView(ChatUi.makeMsg(this, 2, msg, System.currentTimeMillis(), false).root)
        scrollDown()
    }

    private fun err(msg: String) {
        chatMode = false
        hud.active = false
        status.text = ""
        addSystem(msg)
    }

    private fun showLive(t: String) {
        val m = liveUser
        if (m == null) {
            val n = ChatUi.makeMsg(this, 0, t, System.currentTimeMillis(), false)
            liveUser = n
            chatBox.addView(n.root)
        } else {
            m.tv.text = t
        }
        scrollDown()
    }

    private fun dropLive() {
        val m = liveUser
        if (m != null) chatBox.removeView(m.root)
        liveUser = null
    }

    private fun commitUser(t: String) {
        val ts = System.currentTimeMillis()
        store.saveMsg(0, t, ts)
        val m = liveUser
        if (m != null) {
            m.tv.text = t
        } else {
            chatBox.addView(ChatUi.makeMsg(this, 0, t, ts, false).root)
        }
        liveUser = null
        scrollDown()
    }

    private fun showReply(text: String) {
        val ts = System.currentTimeMillis()
        store.saveMsg(1, text, ts)
        val m = ChatUi.makeMsg(this, 1, "", ts, false)
        chatBox.addView(m.root)
        ChatUi.typeInto(m.tv, text, 30L) { scrollDown() }
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
        unmuteBeep()
        dropLive()
        hud.active = false
        status.text = msg
    }

    override fun onPause() {
        unmuteBeep()
        if (chatMode) stopChat(L.t("stopped"))
        super.onPause()
    }

    private fun muteBeep() {
        if (mutedStreams.isNotEmpty()) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for (s in intArrayOf(AudioManager.STREAM_NOTIFICATION, AudioManager.STREAM_SYSTEM, AudioManager.STREAM_MUSIC)) {
            try {
                am.adjustStreamVolume(s, AudioManager.ADJUST_MUTE, 0)
                mutedStreams.add(s)
            } catch (e: Exception) {
            }
        }
    }

    private fun unmuteBeep() {
        if (mutedStreams.isEmpty()) return
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        for (s in mutedStreams) {
            try {
                am.adjustStreamVolume(s, AudioManager.ADJUST_UNMUTE, 0)
            } catch (e: Exception) {
            }
        }
        mutedStreams.clear()
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

    private fun doAction(o: JSONObject): Boolean {
        val r = phone.run(o)
        if (r == 2) {
            pendingPerm = o
            requestPermissions(arrayOf(Manifest.permission.READ_CONTACTS), 2)
            return false
        }
        return r == 1
    }

    private fun afterSpeech() {
        hud.active = false
        val p = pendingAction
        pendingAction = null
        if (p != null) {
            val left = doAction(p)
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
        when (voiceEngine(store)) {
            "fish" -> {
                val fk = store.str("fish_key").trim()
                val fid = store.str("fish_id").trim()
                if (fk.isNotEmpty() && fid.isNotEmpty()) {
                    speakCloud(lang, text, File(cacheDir, "jarvis_voice.mp3")) { f -> FishVoice.fetch(fk, fid, text, f) }
                    return true
                }
            }
            "gemini" -> {
                val key = store.str("key").trim()
                if (key.isNotEmpty()) {
                    val voice = store.str("gem_voice").ifEmpty { "Charon" }
                    val style = store.str("gem_style").ifEmpty { "calm, deep and composed" }
                    speakCloud(lang, text, File(cacheDir, "jarvis_voice.wav")) { f -> GeminiVoice.fetch(key, voice, style, text, f) }
                    return true
                }
            }
        }
        return speakIn(lang, text)
    }

    private fun speakCloud(lang: String, text: String, file: File, fetch: (File) -> Int) {
        val gen = voiceGen
        Thread {
            val code = fetch(file)
            runOnUiThread {
                if (gen == voiceGen) {
                    if (code in 200..299 && file.length() > 0) playFile(file, lang, text) else voiceFailed(code, lang, text)
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
            mp.setOnErrorListener { _, _, _ -> voiceFailed(-2, lang, text); true }
            mp.prepareAsync()
        } catch (e: Exception) {
            voiceFailed(-2, lang, text)
        }
    }

    private fun voiceFailed(code: Int, lang: String, text: String) {
        val why = if (voiceEngine(store) == "gemini") {
            L.t("gem_fail")
        } else {
            when (code) {
                401 -> L.t("fish_key_bad")
                402 -> L.t("fish_credit")
                404, 422 -> L.t("fish_id")
                429 -> L.t("fish_limit")
                else -> L.t("fish_fail")
            }
        }
        addSystem(L.f("voice_fallback", why, code))
        if (!speakIn(lang, text)) afterSpeech()
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
                phone.run(p)
            } else {
                addSystem(L.t("c_denied"))
            }
            return
        }
        if (granted) {
            listen()
        } else {
            chatMode = false
            status.text = L.t("mic_denied")
        }
    }

    private fun listen() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            chatMode = false
            status.text = L.t("no_recog")
            return
        }
        stopVoice()
        recognizer?.destroy()
        recognizer = SpeechRecognizer.createSpeechRecognizer(this)
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onResults(r: Bundle?) {
                unmuteBeep()
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (t == null) {
                    hud.active = false
                    dropLive()
                    return
                }
                silent = 0
                commitUser(t)
                askAi(t)
            }
            override fun onPartialResults(p: Bundle?) {
                val t = p?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (!t.isNullOrBlank()) showLive(t)
            }
            override fun onError(e: Int) {
                unmuteBeep()
                dropLive()
                hud.active = false
                val quiet = e == SpeechRecognizer.ERROR_NO_MATCH || e == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                if (chatMode && quiet) {
                    silent++
                    if (silent >= 3) {
                        stopChat(L.t("long_silence"))
                    } else {
                        hud.postDelayed({ if (chatMode) listen() }, 300)
                    }
                } else {
                    chatMode = false
                    status.text = L.f("mic_err", e)
                }
            }
            override fun onReadyForSpeech(p: Bundle?) {
                hud.active = true
                status.text = if (chatMode) L.t("listening_stop") else L.t("listening")
                hud.postDelayed({ unmuteBeep() }, 250)
            }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(v: Float) {}
            override fun onBufferReceived(b: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(t: Int, p: Bundle?) {}
        })
        val i = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        val lang = store.str("inlang")
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (lang.isEmpty()) "bn-BD" else lang)
        i.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1100L)
        i.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 900L)
        muteBeep()
        recognizer?.startListening(i)
        hud.postDelayed({ unmuteBeep() }, 1500)
    }

    // ---------- AI ব্রেইন ----------
    private fun askAi(userText: String) {
        val key = store.str("key").trim()
        if (key.isEmpty()) {
            err(L.t("need_key"))
            Dialogs.key(this, store)
            return
        }
        hud.active = true
        status.text = L.t("thinking")
        var name = store.str("fullname")
        if (name.isEmpty()) name = store.str("username")
        val sys = if (name.isEmpty()) Prompt.SYS else Prompt.SYS + "\nThe user's name: " + name
        val hist = ArrayList(ctxHist)
        val savedModel = store.str("model")
        val savedThink = store.str("think")

        Thread {
            val r = Ai.ask(key, savedModel, savedThink, sys, hist, userText)
            runOnUiThread { handleAi(r, userText) }
        }.start()
    }

    private fun handleAi(r: Ai.Result, userText: String) {
        val j = r.json
        if (r.code in 200..299 && j != null) {
            val reply = j.optString("reply", "")
            val lang = j.optString("lang", "bn")
            val action = j.optString("action", "none")
            ctxHist.add(Pair("user", userText))
            ctxHist.add(Pair("model", reply))
            store.put("model", r.model)
            store.put("think", r.think)
            status.text = ""
            if (reply.isNotBlank()) showReply(reply)
            if (action.isNotEmpty() && action != "none") pendingAction = j
            val spoke = reply.isNotBlank() && speakReply(lang, reply)
            if (!spoke) {
                hud.active = false
                if (reply.isNotBlank()) addSystem(L.t("no_voice"))
                chatMode = false
                val pa = pendingAction
                pendingAction = null
                if (pa != null) doAction(pa)
            }
            return
        }
        when (r.code) {
            404 -> err(L.f("err_404", r.modelList))
            400, 403 -> err(L.f("err_key", r.code))
            429 -> err(L.t("err_quota"))
            -1 -> err(L.f("err_net", r.ex))
            -2 -> err(L.t("err_parse"))
            else -> err(L.f("err_server", r.code))
        }
    }

    override fun onDestroy() {
        unmuteBeep()
        recognizer?.destroy()
        try { player?.release() } catch (e: Exception) { }
        tts.shutdown()
        super.onDestroy()
    }
}
