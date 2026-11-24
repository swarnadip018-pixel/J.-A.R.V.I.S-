package com.jarvis.assistant

import android.app.Activity
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView

class SettingsActions(
    val key: () -> Unit,
    val engine: () -> Unit,
    val voiceApi: () -> Unit,
    val gemini: () -> Unit,
    val access: () -> Unit,
    val lang: () -> Unit,
    val icon: () -> Unit
)

fun voiceEngine(store: Store): String {
    val e = store.str("engine")
    if (e.isNotEmpty()) return e
    val fish = store.str("fish_key").isNotEmpty() && store.str("fish_id").isNotEmpty() && store.flag("fish_on", true)
    return if (fish) "fish" else "phone"
}

class SettingsPage(
    private val act: Activity,
    private val store: Store,
    private val a: SettingsActions
) {
    val root = FrameLayout(act)
    private val list = LinearLayout(act)

    val isOpen: Boolean get() = root.visibility == View.VISIBLE

    init {
        root.setBackgroundColor(Ui.BG)
        root.isClickable = true
        root.visibility = View.GONE
        list.orientation = LinearLayout.VERTICAL
        list.setPadding(Ui.dp(act, 20), Ui.dp(act, 100), Ui.dp(act, 20), Ui.dp(act, 24))
        val sv = ScrollView(act)
        sv.addView(list)
        root.addView(sv, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        Bars.back(act, root) { close() }
        Bars.title(act, root, L.t("settings").uppercase(), Gravity.TOP or Gravity.END)
        refresh()
    }

    fun refresh() {
        list.removeAllViews()
        val eng = when (voiceEngine(store)) {
            "gemini" -> L.t("eng_n_gemini")
            "fish" -> L.t("eng_n_fish")
            else -> L.t("eng_n_phone")
        }
        list.addView(Rows.row(act, IconView.KEY, L.t("s_key"), L.t("s_key_sub")) { a.key() })
        list.addView(Rows.row(act, IconView.WAVE, L.t("s_engine"), eng) { a.engine() })
        list.addView(Rows.row(act, IconView.IDCARD, L.t("s_voice_api"), L.t("s_voice_api_sub")) { a.voiceApi() })
        list.addView(Rows.row(act, IconView.SPARK, L.t("s_gem"), L.t("s_gem_sub")) { a.gemini() })
        list.addView(Rows.divider(act))
        list.addView(Rows.row(act, IconView.ACCESS, L.t("s_access"), L.t("s_access_sub")) { a.access() })
        list.addView(Rows.row(act, IconView.GLOBE, L.t("s_lang"), if (L.bn) "বাংলা" else "English") { a.lang() })
        list.addView(Rows.row(act, IconView.IMAGE, L.t("s_icon"), L.t("s_icon_sub")) { a.icon() })
    }

    fun open() {
        refresh()
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.translationX = w
        root.visibility = View.VISIBLE
        root.animate().translationX(0f).setDuration(240).start()
    }

    fun close() {
        val w = act.resources.displayMetrics.widthPixels.toFloat()
        root.animate().translationX(w).setDuration(220).withEndAction {
            root.visibility = View.GONE
        }.start()
    }
}
