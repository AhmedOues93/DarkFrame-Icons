package com.darkframe.icons.wallpaper

import android.app.WallpaperManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.R

class WallpaperActivity:AppCompatActivity(){
    override fun onCreate(savedInstanceState:Bundle?){
        super.onCreate(savedInstanceState)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())}
        root.addView(TextView(this).apply{text="Wallpapers";textSize=30f;setTextColor(0xFFF4F4F4.toInt())})
        root.addView(TextView(this).apply{text="AMOLED  •  Carbon  •  Graphite  •  Titanium";textSize=14f;setTextColor(0xFF9EA3AD.toInt());setPadding(0,8,0,28)})
        val preview=ImageView(this).apply{setImageResource(R.drawable.wallpaper_amoled_frame);adjustViewBounds=true}
        root.addView(preview)
        root.addView(Button(this).apply{text="Apply AMOLED Frame";setOnClickListener{
            try{resources.openRawResource(R.drawable.wallpaper_amoled_frame).use{WallpaperManager.getInstance(this@WallpaperActivity).setStream(it)}}catch(_:Exception){Toast.makeText(this@WallpaperActivity,"Wallpaper could not be applied",Toast.LENGTH_SHORT).show()}
        }})
        setContentView(ScrollView(this).apply{addView(root)})
    }
}
