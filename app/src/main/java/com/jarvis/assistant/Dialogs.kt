package com.jarvis.assistant

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
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

    private fun input(act: Activity, hint: String, value: String): EditText {
        val e = EditText(act)
        e.hint = hint
        e.setText(value)
        e.setSingleLine(true)
        e.setTextColor(Color.WHITE)
        e.setHintTextColor(Color.GRAY)
        return e
    }

    fun key(act: Activity, store: Store) {
        val b = box(act)
        val key = input(act, L.t("key_hint"), "")
        b.addView(info(act, L.t("key_info")))
        b.addView(key)
        val has = store.str("key").isNotEmpty()
        dark(act).setTitle(if (has) L.t("key_title_has") else L.t("key_title")).setView(b)
            .setPositiveButton(L.t("dlg_save")) { _, _ ->
                val k = key.text.toString().trim()
                if (k.isNotEmpty()) {
                    store.put("key", k)
                    store.remove("model", "think")
                    toast(act, L.t("key_saved"))
                }
            }
            .setNegativeButton(L.t("dlg_close"), null).show()
    }

    fun fish(act: Activity, store: Store) {
        val b = box(act)
        val k = input(act, L.t("fish_key_hint"), "")
        val id = input(act, L.t("fish_id_hint"), store.str("fish_id"))
        b.addView(info(act, L.t("fish_info")))
        b.addView(k)
        b.addView(id)
        dark(act).setTitle(L.t("fish_title")).setView(b)
            .setPositiveButton(L.t("dlg_save")) { _, _ ->
                val kk = k.text.toString().trim()
                val ii = id.text.toString().trim()
                if (kk.isNotEmpty()) store.put("fish_key", kk)
                if (ii.isNotEmpty()) store.put("fish_id", ii)
                toast(act, L.t("fish_saved"))
            }
            .setNegativeButton(L.t("dlg_close"), null).show()
    }

    fun engine(act: Activity, store: Store, onChanged: () -> Unit) {
        val ids = arrayOf("phone", "gemini", "fish")
        val names = arrayOf(L.t("eng_phone"), L.t("eng_gemini"), L.t("eng_fish"))
        val short = arrayOf(L.t("eng_n_phone"), L.t("eng_n_gemini"), L.t("eng_n_fish"))
        val cur = ids.indexOf(voiceEngine(store))
        dark(act).setTitle(L.t("eng_title"))
            .setSingleChoiceItems(names, cur) { d, i ->
                store.put("engine", ids[i])
                d.dismiss()
                onChanged()
                toast(act, L.f("eng_set", short[i]))
            }
            .setNegativeButton(L.t("dlg_close"), null).show()
    }

    fun geminiVoice(act: Activity, store: Store) {
        val voices = listOf(
            "Charon" to "Informative", "Orus" to "Firm", "Algenib" to "Gravelly",
            "Iapetus" to "Clear", "Kore" to "Firm", "Puck" to "Upbeat",
            "Fenrir" to "Excitable", "Aoede" to "Breezy", "Sulafat" to "Warm", "Achird" to "Friendly"
        )
        val cur = store.str("gem_voice").ifEmpty { "Charon" }
        val b = box(act)
        b.addView(info(act, L.t("gem_info")))
        val rg = RadioGroup(act)
        rg.orientation = RadioGroup.VERTICAL
        for (v in voices) {
            val rb = RadioButton(act)
            rb.id = View.generateViewId()
            rb.tag = v.first
            rb.text = v.first + " — " + v.second
            rb.setTextColor(Color.WHITE)
            rg.addView(rb)
            if (v.first == cur) rb.isChecked = true
        }
        b.addView(rg)
        val label = info(act, L.t("gem_style"))
        label.setPadding(0, Ui.dp(act, 14), 0, 0)
        b.addView(label)
        val style = input(act, "", store.str("gem_style").ifEmpty { "calm, deep and composed" })
        b.addView(style)
        val sv = ScrollView(act)
        sv.addView(b)
        dark(act).setTitle(L.t("gem_title")).setView(sv)
            .setPositiveButton(L.t("dlg_save")) { _, _ ->
                val sel = rg.findViewById<RadioButton>(rg.checkedRadioButtonId)
                val name = (sel?.tag as? String) ?: "Charon"
                store.put("gem_voice", name)
                val st = style.text.toString().trim()
                store.put("gem_style", if (st.isEmpty()) "calm, deep and composed" else st)
                toast(act, L.t("gem_saved"))
            }
            .setNegativeButton(L.t("dlg_close"), null).show()
    }

    fun listenLang(act: Activity, store: Store) {
        val names = arrayOf("বাংলা", "English", "हिन्दी")
        val codes = arrayOf("bn-BD", "en-US", "hi-IN")
        dark(act).setTitle(L.t("ll_title"))
            .setItems(names) { _, i ->
                store.put("inlang", codes[i])
                toast(act, L.f("ll_set", names[i]))
            }.show()
    }

    fun appLang(act: Activity, store: Store, onChanged: () -> Unit) {
        val names = arrayOf("English", "বাংলা")
        val codes = arrayOf("en", "bn")
        dark(act).setTitle(L.t("al_title"))
            .setItems(names) { _, i ->
                store.put("ui_lang", codes[i])
                L.bn = codes[i] == "bn"
                onChanged()
            }.show()
    }

    fun iconInfo(act: Activity, onPick: () -> Unit) {
        dark(act).setTitle(L.t("ic_title")).setMessage(L.t("ic_msg"))
            .setPositiveButton(L.t("ic_pick")) { _, _ -> onPick() }
            .setNegativeButton(L.t("ic_cancel"), null).show()
    }
}
