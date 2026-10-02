package com.darkframe.icons

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.data.DarkFrameCatalog
import com.darkframe.icons.engine.DarkFrameEngine
import com.darkframe.icons.ui.OnboardingActivity
import com.darkframe.icons.ui.browser.IconBrowserActivity
import com.darkframe.icons.ui.SearchActivity
import com.darkframe.icons.ui.StylesActivity
import com.darkframe.icons.wallpaper.WallpaperActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!getSharedPreferences("darkframe", MODE_PRIVATE).getBoolean("onboarded", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java)); finish(); return
        }
        val scroll=ScrollView(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,56);setBackgroundColor(0xFF090B0E.toInt())}
        root.addView(TextView(this).apply{text="DARKFRAME";textSize=30f;setTextColor(0xFFF4F4F4.toInt())})
        root.addView(TextView(this).apply{text="Complete dark customization";textSize=15f;setTextColor(0xFF9EA3AD.toInt());setPadding(0,8,0,28)})

        // The dynamic engine is the headline feature, so it leads the screen.
        root.addView(TextView(this).apply{text=getString(R.string.home_engine_heading);textSize=22f;setTextColor(0xFFF4F4F4.toInt())})
        root.addView(TextView(this).apply{text=getString(R.string.home_engine_body);textSize=14f;setTextColor(0xFF9EA3AD.toInt());setPadding(0,8,0,16)})
        root.addView(Button(this).apply{text=getString(R.string.browser_open_browser);setOnClickListener{startActivity(Intent(this@MainActivity,IconBrowserActivity::class.java))}})
        root.addView(Button(this).apply{
            text=getString(R.string.home_rebuild_cache)
            setOnClickListener{
                val engine=DarkFrameEngine.get(applicationContext)
                Thread{engine.resolver.invalidateAll()}.start()
                Toast.makeText(this@MainActivity,R.string.home_rebuild_done,Toast.LENGTH_SHORT).show()
            }
        })

        root.addView(Button(this).apply{text=getString(R.string.search_curated_title);setOnClickListener{startActivity(Intent(this@MainActivity,SearchActivity::class.java))}})
        root.addView(Button(this).apply{text=getString(R.string.styles_title);setOnClickListener{startActivity(Intent(this@MainActivity,StylesActivity::class.java))}})
        root.addView(Button(this).apply{text="Wallpapers";setOnClickListener{startActivity(Intent(this@MainActivity,WallpaperActivity::class.java))}})
        root.addView(TextView(this).apply{text="Featured styles";textSize=22f;setTextColor(0xFFF4F4F4.toInt());setPadding(0,32,0,12)})
        DarkFrameCatalog.styles.forEach{style->root.addView(TextView(this).apply{text=style.title+(if(style.tier.name=="PRO")"  PRO" else "");textSize=18f;setTextColor(0xFFE7E7E7.toInt());setPadding(4,18,4,18)})}
        root.addView(TextView(this).apply{text="Curated DarkFrame artwork";textSize=22f;setTextColor(0xFFF4F4F4.toInt());setPadding(0,34,0,14)})
        val grid=GridLayout(this).apply{columnCount=4}
        DarkFrameCatalog.icons.forEach{item->
            val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;gravity=Gravity.CENTER;setPadding(6,10,6,16)}
            box.addView(ImageView(this).apply{setImageResource(item.drawable);layoutParams=LinearLayout.LayoutParams(116,116)})
            box.addView(TextView(this).apply{text=item.label;textSize=10f;gravity=Gravity.CENTER;setTextColor(0xFFD8D8D8.toInt())})
            grid.addView(box,GridLayout.LayoutParams().apply{width=0;columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f)})
        }
        root.addView(grid)
        root.addView(Button(this).apply{text="Request missing icon";setOnClickListener{startActivity(Intent(Intent.ACTION_SENDTO,Uri.parse("mailto:darkframe.icons@gmail.com?subject=DarkFrame%20icon%20request")))}})
        scroll.addView(root);setContentView(scroll)
    }
}
