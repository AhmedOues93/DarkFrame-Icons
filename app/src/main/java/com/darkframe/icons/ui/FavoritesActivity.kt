package com.darkframe.icons.ui

import android.os.Bundle
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.data.DarkFrameCatalog
import com.darkframe.icons.data.FavoritesStore

class FavoritesActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store=FavoritesStore(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())}
        root.addView(TextView(this).apply{text="Favorites";textSize=30f;setTextColor(0xFFF4F4F4.toInt())})
        val icons=DarkFrameCatalog.icons.filter{store.isFavorite("icon",it.id)}
        val hasWallpaper=store.isFavorite("wallpaper","amoled_frame")
        if(icons.isEmpty()&&!hasWallpaper){
            root.addView(TextView(this).apply{text="Nothing saved yet";textSize=17f;setTextColor(0xFFC8CBD0.toInt());setPadding(0,24,0,0)})
        }else{
            if(icons.isNotEmpty()) root.addView(TextView(this).apply{text="Icons\n"+icons.joinToString("\n"){it.label};textSize=17f;setTextColor(0xFFC8CBD0.toInt());setPadding(0,24,0,0)})
            if(hasWallpaper) root.addView(TextView(this).apply{text="Wallpapers\nAMOLED Frame";textSize=17f;setTextColor(0xFFC8CBD0.toInt());setPadding(0,28,0,0)})
        }
        setContentView(ScrollView(this).apply{addView(root)})
    }
}
