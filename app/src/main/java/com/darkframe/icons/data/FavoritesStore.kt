package com.darkframe.icons.data

import android.content.Context

class FavoritesStore(context:Context) {
    private val prefs=context.getSharedPreferences("darkframe_favorites",Context.MODE_PRIVATE)
    fun isFavorite(type:String,id:String)=prefs.getBoolean("$type:$id",false)
    fun setFavorite(type:String,id:String,value:Boolean)=prefs.edit().putBoolean("$type:$id",value).apply()
}
