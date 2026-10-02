package com.darkframe.icons

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private val icons = listOf("Instagram","YouTube","ChatGPT","Spotify","WhatsApp","TikTok","Gmail","Chrome","Maps","Camera","Phone","Messages")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val scroll = ScrollView(this)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; setPadding(48,64,48,64); setBackgroundColor(0xFF090B0E.toInt())
        }
        root.addView(TextView(this).apply { text="DARKFRAME"; textSize=32f; setTextColor(0xFFF4F4F4.toInt()) })
        root.addView(TextView(this).apply { text="Classic Outline"; textSize=16f; setTextColor(0xFF9EA3AD.toInt()); setPadding(0,8,0,32) })
        val grid=GridLayout(this).apply { columnCount=4; alignmentMode=GridLayout.ALIGN_BOUNDS }
        icons.forEachIndexed { i,name ->
            val box=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; gravity=Gravity.CENTER; setPadding(8,12,8,18) }
            box.addView(ImageView(this).apply { setImageResource(iconRes(i)); layoutParams=LinearLayout.LayoutParams(120,120) })
            box.addView(TextView(this).apply { text=name; textSize=11f; gravity=Gravity.CENTER; setTextColor(0xFFD8D8D8.toInt()) })
            grid.addView(box, GridLayout.LayoutParams().apply { width=0; columnSpec=GridLayout.spec(GridLayout.UNDEFINED,1f) })
        }
        root.addView(grid)
        root.addView(Button(this).apply { text="Request an icon"; setOnClickListener {
            startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:darkframe.icons@gmail.com?subject=DarkFrame%20icon%20request")))
        }})
        scroll.addView(root); setContentView(scroll)
    }
    private fun iconRes(i:Int)= listOf(R.drawable.df_instagram,R.drawable.df_youtube,R.drawable.df_chatgpt,R.drawable.df_spotify,R.drawable.df_whatsapp,R.drawable.df_tiktok,R.drawable.df_gmail,R.drawable.df_chrome,R.drawable.df_maps,R.drawable.df_camera,R.drawable.df_phone,R.drawable.df_messages)[i]
}
