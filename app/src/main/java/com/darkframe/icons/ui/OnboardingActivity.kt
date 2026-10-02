package com.darkframe.icons.ui

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.MainActivity

class OnboardingActivity:AppCompatActivity(){
    private val pages=listOf(
        "Choose your style" to "Icons, wallpapers and widgets designed as one visual system.",
        "Customize your phone" to "Build a clean AMOLED, Carbon, Graphite, Titanium or Glass setup.",
        "Create your own look" to "Preview components and apply only what Android allows."
    )
    private var page=0
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);render()}
    private fun render(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(48,96,48,48);setBackgroundColor(0xFF090B0E.toInt())}
        root.addView(TextView(this).apply{text=pages[page].first;textSize=32f;setTextColor(0xFFF4F4F4.toInt())})
        root.addView(TextView(this).apply{text=pages[page].second;textSize=17f;setPadding(0,24,0,48);setTextColor(0xFFB5B8BE.toInt())})
        root.addView(Button(this).apply{text=if(page==2)"Enter DarkFrame" else "Continue";setOnClickListener{if(page<2){page++;render()}else{getSharedPreferences("darkframe",MODE_PRIVATE).edit().putBoolean("onboarded",true).apply();startActivity(Intent(this@OnboardingActivity,MainActivity::class.java));finish()}}})
        setContentView(root)
    }
}
