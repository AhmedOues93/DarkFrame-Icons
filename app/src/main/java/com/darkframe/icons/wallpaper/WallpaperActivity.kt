package com.darkframe.icons.wallpaper

import android.app.WallpaperManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.darkframe.icons.R
import com.darkframe.icons.data.FavoritesStore

class WallpaperActivity : AppCompatActivity() {
    private val wallpaperId = "amoled_frame"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val favorites = FavoritesStore(this)
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())}
        root.addView(TextView(this).apply{text="Wallpapers";textSize=30f;setTextColor(0xFFF4F4F4.toInt())})
        root.addView(TextView(this).apply{text="AMOLED  •  Carbon  •  Graphite  •  Titanium";textSize=14f;setTextColor(0xFF9EA3AD.toInt());setPadding(0,8,0,28)})
        root.addView(ImageView(this).apply{setImageResource(R.drawable.wallpaper_amoled_frame);adjustViewBounds=true})

        val favoriteButton=Button(this).apply {
            fun refresh(){ text=if(favorites.isFavorite("wallpaper",wallpaperId)) "Remove from favorites" else "Add to favorites" }
            refresh()
            setOnClickListener {
                favorites.setFavorite("wallpaper",wallpaperId,!favorites.isFavorite("wallpaper",wallpaperId))
                refresh()
            }
        }
        root.addView(favoriteButton)
        root.addView(Button(this).apply{text="Apply to home screen";setOnClickListener{applyWallpaper(WallpaperManager.FLAG_SYSTEM)}})
        root.addView(Button(this).apply{text="Apply to lock screen";setOnClickListener{applyWallpaper(WallpaperManager.FLAG_LOCK)}})
        root.addView(Button(this).apply{text="Apply to both";setOnClickListener{applyWallpaper(WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)}})
        setContentView(ScrollView(this).apply{addView(root)})
    }

    private fun applyWallpaper(flags:Int){
        val d=ContextCompat.getDrawable(this,R.drawable.wallpaper_amoled_frame)?:return
        val bitmap=Bitmap.createBitmap(1080,2400,Bitmap.Config.ARGB_8888)
        d.setBounds(0,0,bitmap.width,bitmap.height)
        d.draw(Canvas(bitmap))
        try{
            WallpaperManager.getInstance(this).setBitmap(bitmap,null,true,flags)
            Toast.makeText(this,"Wallpaper applied",Toast.LENGTH_SHORT).show()
        }catch(_:Exception){
            Toast.makeText(this,"Wallpaper could not be applied",Toast.LENGTH_SHORT).show()
        }
    }
}
