package com.darkframe.icons.ui
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.darkframe.icons.data.DarkFrameCatalog
import com.darkframe.icons.data.FavoritesStore
class FavoritesActivity:AppCompatActivity(){override fun onCreate(b:Bundle?){super.onCreate(b);val store=FavoritesStore(this);val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(40,56,40,40);setBackgroundColor(0xFF090B0E.toInt())};root.addView(TextView(this).apply{text="Favorites";textSize=30f;setTextColor(0xFFF4F4F4.toInt())});val fav=DarkFrameCatalog.icons.filter{store.isFavorite("icon",it.id)};root.addView(TextView(this).apply{text=if(fav.isEmpty())"No favorite icons yet" else fav.joinToString("\n"){it.label};textSize=17f;setTextColor(0xFFC8CBD0.toInt());setPadding(0,24,0,0)});setContentView(root)}}