package com.darkframe.icons.ui.setup

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.ui.browser.IconBrowserActivity

class GuidedSetupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL; setPadding(dp(24),dp(32),dp(24),dp(32)); setBackgroundColor(0xFF07080A.toInt())
        }
        fun text(s:String,z:Float,c:Int)=TextView(this).apply{text=s;textSize=z;setTextColor(c);setPadding(0,0,0,dp(14))}
        fun button(s:String,run:()->Unit)=TextView(this).apply{
            text=s;textSize=16f;gravity=Gravity.CENTER;setTextColor(0xFFF4F4F2.toInt());setBackgroundColor(0xFF171A1F.toInt())
            minHeight=dp(58);setOnClickListener{run()};layoutParams=LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(10)}
        }
        root.addView(text("Apply on Samsung One UI",28f,0xFFF4F4F2.toInt()))
        root.addView(text("DarkFrame prepares the full icon style. Samsung controls the final system-wide apply step.",15f,0xFF9BA1AB.toInt()))
        root.addView(text("1  Choose a DarkFrame collection\n2  Open Samsung Theme Park\n3  Create an icon theme and choose DarkFrame as the icon pack\n4  Apply the theme",18f,0xFFF4F4F2.toInt()))
        root.addView(button("OPEN THEME PARK"){
            val launch=packageManager.getLaunchIntentForPackage("com.samsung.android.themedesigner")
                ?: packageManager.getLaunchIntentForPackage("com.samsung.android.goodlock")
            if(launch!=null) startActivity(launch) else {
                try{startActivity(Intent(Settings.ACTION_SETTINGS))}catch(_:Exception){}
                Toast.makeText(this,"Install Theme Park from Galaxy Store, then return to DarkFrame.",Toast.LENGTH_LONG).show()
            }
        })
        root.addView(button("PREVIEW DARKFRAME ICONS"){startActivity(Intent(this,IconBrowserActivity::class.java))})
        root.addView(text("No duplicate shortcuts. DarkFrame does not run a continuous background service.",13f,0xFF9BA1AB.toInt()).apply{setPadding(0,dp(24),0,0)})
        setContentView(ScrollView(this).apply{addView(root)})
    }
    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
