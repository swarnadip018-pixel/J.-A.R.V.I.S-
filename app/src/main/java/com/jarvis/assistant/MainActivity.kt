package com.jarvis.assistant

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var tts: TextToSpeech
    private lateinit var status: TextView
    private var recognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(48, 120, 48, 48)
        val btn = Button(this)
        btn.text = "🎤 কথা বলুন"
        status = TextView(this)
        status.text = "বোতাম চেপে কথা বলুন"
        status.textSize = 20f
        layout.addView(btn)
        layout.addView(status)
        setContentView(layout)

        tts = TextToSpeech(this, this)
        btn.setOnClickListener {
            if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                listen()
            } else {
                requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), 1)
            }
        }
    }

    override fun onInit(s: Int) {
        if (s == TextToSpeech.SUCCESS) tts.language = Locale("bn", "BD")
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
                val t = r?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: return
                status.text = "আপনি বললেন: $t"
                tts.speak("আপনি বললেন: $t", TextToSpeech.QUEUE_FLUSH, null, "u1")
            }
            override fun onError(e: Int) { status.text = "বুঝতে পারিনি, আবার বলুন (কোড $e)" }
            override fun onReadyForSpeech(p: Bundle?) { status.text = "শুনছি…" }
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
