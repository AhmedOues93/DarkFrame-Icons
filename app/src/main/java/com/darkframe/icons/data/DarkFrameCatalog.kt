package com.darkframe.icons.data

import com.darkframe.icons.R
import com.darkframe.icons.model.*

object DarkFrameCatalog {
    val icons = listOf(
        IconItem("instagram","Instagram",R.drawable.df_instagram),
        IconItem("youtube","YouTube",R.drawable.df_youtube),
        IconItem("chatgpt","ChatGPT",R.drawable.df_chatgpt),
        IconItem("spotify","Spotify",R.drawable.df_spotify),
        IconItem("whatsapp","WhatsApp",R.drawable.df_whatsapp),
        IconItem("tiktok","TikTok",R.drawable.df_tiktok),
        IconItem("gmail","Gmail",R.drawable.df_gmail),
        IconItem("chrome","Chrome",R.drawable.df_chrome),
        IconItem("maps","Maps",R.drawable.df_maps),
        IconItem("camera","Camera",R.drawable.df_camera),
        IconItem("phone","Phone",R.drawable.df_phone),
        IconItem("messages","Messages",R.drawable.df_messages)
    )
    val styles = listOf(
        StylePack("black","DarkFrame Black",IconCollection.BLACK,null,listOf(WidgetKind.DIGITAL_CLOCK,WidgetKind.BATTERY),ContentTier.FREE),
        StylePack("carbon","DarkFrame Carbon",IconCollection.CARBON,null,listOf(WidgetKind.DIGITAL_CLOCK,WidgetKind.BATTERY),ContentTier.PRO),
        StylePack("graphite","DarkFrame Graphite",IconCollection.GRAPHITE,null,listOf(WidgetKind.DATE,WidgetKind.CALENDAR),ContentTier.PRO),
        StylePack("titanium","DarkFrame Titanium",IconCollection.TITANIUM,null,listOf(WidgetKind.ANALOG_CLOCK,WidgetKind.INFO),ContentTier.PRO),
        StylePack("glass","DarkFrame Glass",IconCollection.GLASS,null,listOf(WidgetKind.DIGITAL_CLOCK,WidgetKind.DATE),ContentTier.PRO),
        StylePack("amoled","DarkFrame Pure AMOLED",IconCollection.PURE_AMOLED,null,listOf(WidgetKind.DIGITAL_CLOCK,WidgetKind.BATTERY),ContentTier.FREE)
    )
    fun search(query:String):List<IconItem> {
        val q=query.trim()
        if(q.isEmpty()) return icons
        return icons.filter { it.label.contains(q,true) || it.id.contains(q,true) }
    }
}
