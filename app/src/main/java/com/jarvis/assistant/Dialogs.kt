package com.jarvis.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast

object Dialogs {
    private fun dark(act: Activity) =
        AlertDialog.Builder(act, android.R.style.Theme_DeviceDefault_Dialog_Alert)

    private fun toast(act: Activity, m: String) = Toast.makeText(act, m, Toast.LENGTH_SHORT).show()

    private fun box(act: Activity): LinearLayout {
        val b = LinearLayout(act)
        b.orientation = LinearLayout.VERTICAL
        b.setPadding(Ui.dp(act, 24), Ui.dp(act, 8), Ui.dp(act, 24), 0)
        return b
    }

    private fun info(act: Activity, t: String): TextView {
        val v = TextView(act)
        v.text = t
        v.setTextColor(Color.WHITE)
        return v
    }

    private fun input(act: Activity, hint: String): EditText {
        val e = EditText(act)
        e.hint = hint
        e.setSingleLine(true)
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(Color.GRAY)
        return e
    }

    fun key(act: Activity, store: Store) {
        val b = box(act)
        val key = input(act, "AQ... বা AIza...")
        b.addView(info(act, "aistudio.google.com থেকে নেওয়া Gemini API Key এখানে পেস্ট করুন। এটা শুধু আপনার ফোনে থাকবে।"))
        b.addView(key)
        val has = store.str("key").isNotEmpty()
        dark(act).setTitle(if (has) "🔑 API Key (আগেরটা সেভ আছে)" else "🔑 API Key").setView(b)
            .setPositiveButton("সেভ") { _, _ ->
                val k = key.text.toString().trim()
                if (k.isNotEmpty()) {
                    store.put("key", k)
                    store.remove("model", "think")
                    toast(act, "Key সেভ হয়েছে")
                }
            }
            .setNegativeButton("বন্ধ", null).show()
    }

    fun fish(act: Activity, store: Store) {
        val b = box(act)
        val k = input(act, "Fish Audio API Key")
        val id = input(act, "Reference ID")
        b.addView(info(act, "Fish Audio-র API Key ও ক্লোন ভয়েসের Reference ID বসান। উত্তরের লেখা Fish Audio-র সার্ভারে যাবে। শুধু নিজের বা অনুমতি পাওয়া কণ্ঠ ব্যবহার করুন।"))
        b.addView(k)
        b.addView(id)
        val has = store.str("fish_key").isNotEmpty() && store.str("fish_id").isNotEmpty()
        val on = store.flag("fish_on", true)
        val title = if (has) (if (on) "🔊 ক্লোন ভয়েস (চালু)" else "🔊 ক্লোন ভয়েস (বন্ধ)") else "🔊 ক্লোন ভয়েস"
        val d = dark(act).setTitle(title).setView(b)
            .setPositiveButton("সেভ ও চালু") { _, _ ->
                val kk = k.text.toString().trim()
                val ii = id.text.toString().trim()
                if (kk.isNotEmpty()) store.put("fish_key", kk)
                if (ii.isNotEmpty()) store.put("fish_id", ii)
                store.putFlag("fish_on", true)
                toast(act, "সেভ হয়েছে। এবার জার্ভিসকে কিছু জিজ্ঞেস করুন।")
            }
            .setNegativeButton("বন্ধ", null)
        if (has) {
            d.setNeutralButton(if (on) "ক্লোন বন্ধ করুন" else "ক্লোন চালু করুন") { _, _ ->
                store.putFlag("fish_on", !on)
                toast(act, if (on) "ফোনের সাধারণ ভয়েসে ফিরলাম" else "ক্লোন ভয়েস চালু")
            }
        }
        d.show()
    }

    fun lang(act: Activity, store: Store) {
        val names = arrayOf("বাংলা", "English", "हिन्दी")
        val codes = arrayOf("bn-BD", "en-US", "hi-IN")
        dark(act).setTitle("🌐 আপনি কোন ভাষায় কথা বলবেন?")
            .setItems(names) { _, i ->
                store.put("inlang", codes[i])
                toast(act, "শোনার ভাষা: ${names[i]}")
            }.show()
    }
}
