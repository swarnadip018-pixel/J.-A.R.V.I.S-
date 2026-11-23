package com.jarvis.assistant

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class Store(private val ctx: Context) {
    private val p = ctx.getSharedPreferences("jarvis", Context.MODE_PRIVATE)

    fun str(k: String): String = p.getString(k, "") ?: ""
    fun put(k: String, v: String) { p.edit().putString(k, v).apply() }
    fun flag(k: String, d: Boolean): Boolean = p.getBoolean(k, d)
    fun putFlag(k: String, v: Boolean) { p.edit().putBoolean(k, v).apply() }
    fun remove(vararg ks: String) {
        val e = p.edit()
        for (k in ks) e.remove(k)
        e.apply()
    }

    fun photoFile() = File(ctx.filesDir, "profile.jpg")
    private fun histFile() = File(ctx.filesDir, "chat_history.jsonl")

    fun saveMsg(role: Int, text: String, ts: Long) {
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

    fun loadMsgs(): List<Triple<Int, String, Long>> {
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

    fun clearHistory() { histFile().delete() }
}

object Ai {
    val MODELS = listOf(
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.5-flash",
        "gemini-3.7-flash",
        "gemini-3.8-flash"
    )

    val SYS = """তুমি J.A.R.V.I.S, ব্যবহারকারীর ব্যক্তিগত ভয়েস সহকারী। শান্ত ও ভদ্রভাবে কথা বলো। উত্তর পরিষ্কার ও সম্পূর্ণ দাও: সাধারণ প্রশ্নে ছোট উত্তর, কিন্তু কেউ ব্যাখ্যা বা বিস্তারিত চাইলে প্রয়োজনমতো বিস্তারিত বলো। উত্তর জোরে পড়ে শোনানো হবে, তাই সহজ ভাষায় লেখো।
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

নিয়ম: কাজ করানোর সময় reply-তে সংক্ষেপে বলো তুমি কী করছ, যেমন "ইউটিউব খুলছি"। কল বা SMS-এর ক্ষেত্রে বলো "ডায়ালারে সাজিয়ে দিচ্ছি, কল বাটন আপনি চাপুন"। বার্তা বা কলের নম্বর না জানলে আন্দাজ করো না, action none দিয়ে জিজ্ঞেস করো। এই তালিকার বাইরের কাজ (Wi-Fi/ব্লুটুথ অন-অফ, অন্য অ্যাপের ভেতরে ট্যাপ, ক্যামেরায় ছবি তোলা, সার্চের ফল পড়ে শোনানো) এখনও পারো না, সৎভাবে বলো। আন্দাজে ভুল তথ্য দিও না।"""

    class Result(
        val code: Int,
        val json: JSONObject?,
        val model: String,
        val think: String,
        val modelList: String,
        val ex: String
    )

    private fun body(sys: String, contents: JSONArray, think: String): JSONObject {
        val gc = JSONObject().put("responseMimeType", "application/json")
        if (think != "none") gc.put("thinkingConfig", JSONObject().put("thinkingLevel", think))
        return JSONObject()
            .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", sys))))
            .put("contents", contents)
            .put("generationConfig", gc)
    }

    private fun call(model: String, key: String, body: JSONObject): Pair<Int, String> {
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

    fun ask(
        key: String,
        savedModel: String,
        savedThink: String,
        sys: String,
        hist: List<Pair<String, String>>,
        userText: String
    ): Result {
        try {
            val contents = JSONArray()
            for (h in hist.takeLast(8)) {
                contents.put(JSONObject()
                    .put("role", h.first)
                    .put("parts", JSONArray().put(JSONObject().put("text", h.second))))
            }
            contents.put(JSONObject()
                .put("role", "user")
                .put("parts", JSONArray().put(JSONObject().put("text", userText))))

            val models = ArrayList<String>()
            if (savedModel.isNotEmpty()) models.add(savedModel)
            for (m in MODELS) if (!models.contains(m)) models.add(m)
            val thinks = ArrayList<String>()
            if (savedThink.isNotEmpty()) thinks.add(savedThink)
            for (t in listOf("minimal", "low", "none")) if (!thinks.contains(t)) thinks.add(t)

            var code = 0
            var text = ""
            var okModel = ""
            var okThink = ""
            outer@ for (m in models) {
                for (t in thinks) {
                    val r = call(m, key, body(sys, contents, t))
                    code = r.first
                    text = r.second
                    if (code in 200..299) {
                        okModel = m
                        okThink = t
                        break@outer
                    }
                    if (code == 404) break
                    if (code == 400 && text.contains("thinking", ignoreCase = true)) continue
                    break@outer
                }
            }

            if (code in 200..299) {
                return try {
                    val out = JSONObject(text).getJSONArray("candidates").getJSONObject(0)
                        .getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text")
                    val clean = out.replace("```json", "").replace("```", "").trim()
                    val j = try {
                        JSONObject(clean)
                    } catch (e: Exception) {
                        JSONObject().put("lang", "bn").put("reply", out).put("action", "none")
                    }
                    Result(code, j, okModel, okThink, "", "")
                } catch (e: Exception) {
                    Result(-2, null, "", "", "", "")
                }
            }
            val list = if (code == 404) listModels(key) else ""
            return Result(code, null, "", "", list, "")
        } catch (e: Exception) {
            return Result(-1, null, "", "", "", e.javaClass.simpleName)
        }
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
}

object FishVoice {
    fun fetch(key: String, refId: String, text: String, out: File): Int {
        var code = 0
        try {
            val body = JSONObject()
                .put("text", text)
                .put("reference_id", refId)
                .put("format", "mp3")
            for (m in listOf("s2-pro", "s2.1-pro")) {
                val conn = URL("https://api.fish.audio/v1/tts").openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.connectTimeout = 15000
                conn.readTimeout = 40000
                conn.setRequestProperty("Authorization", "Bearer " + key)
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("model", m)
                conn.doOutput = true
                conn.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                code = conn.responseCode
                if (code in 200..299) {
                    conn.inputStream.use { ins -> out.outputStream().use { ins.copyTo(it) } }
                    return code
                }
                if (code == 401 || code == 402 || code == 429) return code
            }
        } catch (e: Exception) {
            return -1
        }
        return code
    }
}
