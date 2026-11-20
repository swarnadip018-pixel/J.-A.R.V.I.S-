package com.jarvis.assistant

import android.app.Activity
import android.os.Bundle
import android.widget.TextView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val t = TextView(this)
        t.text = "হ্যালো! আমি জার্ভিস।"
        t.textSize = 24f
        t.setPadding(48, 120, 48, 48)
        setContentView(t)
    }
}
