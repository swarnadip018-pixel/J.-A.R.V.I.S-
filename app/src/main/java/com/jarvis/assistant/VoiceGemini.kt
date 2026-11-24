package com.jarvis.assistant

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.nio.ByteBuffer
import java.nio.ByteOrder

object GeminiVoice {
    private val MODELS = listOf("gemini-3.1-flash-tts-preview", "gemini-2.5-flash-preview-tts")

    private fun writeWav(f: File, pcm: ByteArray, rate: Int) {
        val h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray())
        h.putInt(36 + pcm.size)
        h.put("WAVE".toByteArray())
        h.put("fmt ".toByteArray())
        h.putInt(16)
        h.putShort(1.toShort())
        h.putShort(1.toShort())
        h.putInt(rate)
        h.putInt(rate * 2)
        h.putShort(2.toShort())
        h.putShort(16.toShort())
        h.put("data".toByteArray())
        h.putInt(pcm.size)
        f.outputStream().use {
            it.write(h.array())
            it.write(pcm)
        }
    }

    // সফল হলে HTTP কোড (200..299) ফেরত দেয় এবং WAV ফাইল লেখে
    fun fetch(key: String, voice: String, style: String, text: String, out: File): Int {
        var code = 0
        try {
            val prompt = "Say in a " + style + " voice: " + text
            val body = JSONObject()
                .put("contents", JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt)))))
                .put("generationConfig", JSONObject()
                    .put("responseModalities", JSONArray().put("AUDIO"))
                    .put("speechConfig", JSONObject()
                        .put("voiceConfig", JSONObject()
                            .put("prebuiltVoiceConfig", JSONObject().put("voiceName", voice)))))
            for (m in MODELS) {
                val url = URL("https://generativelanguage.googleapis.com/v1beta/models/$m:generateContent")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("x-goog-api-key", key)
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                code = conn.responseCode
                if (code in 200..299) {
                    val txt = conn.inputStream.bufferedReader().use { it.readText() }
                    val b64 = JSONObject(txt).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0)
                        .getJSONObject("inlineData").getString("data")
                    writeWav(out, Base64.decode(b64, Base64.DEFAULT), 24000)
                    return code
                }
                if (code != 404) return code
            }
        } catch (e: Exception) {
            return -1
        }
        return code
    }
}
