package com.darkframe.icons

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(56, 72, 56, 56)
            setBackgroundColor(0xFF090B0E.toInt())
        }
        root.addView(TextView(this).apply {
            text = "DARKFRAME"
            textSize = 30f
            setTextColor(0xFFF5F5F5.toInt())
        })
        root.addView(TextView(this).apply {
            text = "Classic Outline · Preview"
            textSize = 16f
            setTextColor(0xFFB8BDC7.toInt())
            setPadding(0, 18, 0, 0)
        })
        setContentView(root)
    }
}
