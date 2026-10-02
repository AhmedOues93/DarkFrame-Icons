package com.darkframe.icons

import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.data.DarkFrameCatalog
import com.darkframe.icons.ui.ProActivity
import com.darkframe.icons.ui.StylesActivity
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.setup.GuidedSetupActivity
import com.darkframe.icons.wallpaper.WallpaperActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!getSharedPreferences("darkframe", MODE_PRIVATE).getBoolean("onboarded", false)) {
            startActivity(Intent(this, com.darkframe.icons.ui.OnboardingActivity::class.java)); finish(); return
        }
        render()
    }

    override fun onResume() { super.onResume(); if (::root.isInitialized) render() }

    private lateinit var root: LinearLayout

    private fun render() {
        val scroll = ScrollView(this)
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(28), dp(24), dp(40))
            setBackgroundColor(0xFF07080A.toInt())
        }
        fun title(text:String,size:Float=28f,secondary:Boolean=false)=TextView(this).apply{
            this.text=text; textSize=size; setTextColor(if(secondary)0xFF9BA1AB.toInt() else 0xFFF4F4F2.toInt())
        }
        fun action(text:String, click:()->Unit)=TextView(this).apply{
            this.text=text; textSize=16f; gravity=Gravity.CENTER_VERTICAL
            setTextColor(0xFFF4F4F2.toInt()); setBackgroundColor(0xFF171A1F.toInt())
            setPadding(dp(18),0,dp(18),0); minHeight=dp(58)
            setOnClickListener{click()}
            layoutParams=LinearLayout.LayoutParams(-1,dp(58)).apply{topMargin=dp(10)}
        }

        root.addView(title("DARKFRAME",30f))
        root.addView(title("Make One UI yours.",15f,true).apply{setPadding(0,dp(4),0,dp(24))})

        val samsung = packageManager.resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),0)
            ?.activityInfo?.packageName?.contains("sec.android.app.launcher")==true
        root.addView(title(if(samsung)"Samsung One UI detected" else "Icon pack ready",13f,true))

        root.addView(action("CHOOSE YOUR LOOK"){ startActivity(Intent(this, StylesActivity::class.java)) })
        root.addView(action(if(samsung)"APPLY ON SAMSUNG" else "APPLY ICON PACK"){
            startActivity(Intent(this, GuidedSetupActivity::class.java))
        })
        root.addView(action("WALLPAPERS"){ startActivity(Intent(this, WallpaperActivity::class.java)) })
        root.addView(action("PREVIEW ALL APPS"){ startActivity(Intent(this, IconBrowserActivity::class.java)) })

        root.addView(title("Complete looks",22f).apply{setPadding(0,dp(32),0,dp(8))})
        DarkFrameCatalog.styles.forEach { style ->
            root.addView(action(style.title + if(style.tier.name=="PRO") "   PRO" else ""){
                if(style.tier.name=="PRO" && ProEntitlementStore(this).current()!=Entitlement.PRO)
                    startActivity(Intent(this, ProActivity::class.java))
                else startActivity(Intent(this, StylesActivity::class.java))
            })
        }

        if(ProEntitlementStore(this).current()!=Entitlement.PRO)
            root.addView(action("UNLOCK DARKFRAME PRO"){startActivity(Intent(this,ProActivity::class.java))})

        root.addView(title("DarkFrame changes appearance only. It never runs continuously in the background.",12f,true)
            .apply{setPadding(0,dp(28),0,0)})
        scroll.addView(root); setContentView(scroll)
    }

    private fun dp(v:Int)=(v*resources.displayMetrics.density).toInt()
}
