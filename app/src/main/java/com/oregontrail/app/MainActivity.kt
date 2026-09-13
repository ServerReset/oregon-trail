package com.oregontrail.app

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.oregontrail.engine.EngineInfo

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val tv = TextView(this)
        tv.text = "${EngineInfo.TITLE} v${EngineInfo.VERSION}"
        tv.setBackgroundColor(0xFF0B1A0B.toInt())
        tv.setTextColor(0xFF7CFF7C.toInt())
        setContentView(tv)
    }
}
