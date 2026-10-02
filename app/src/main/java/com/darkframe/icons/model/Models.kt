package com.darkframe.icons.model

enum class ContentTier { FREE, PRO }
enum class IconCollection { BLACK, CARBON, GRAPHITE, TITANIUM, GLASS, PURE_AMOLED }
enum class WallpaperCategory { AMOLED, CARBON, GRAPHITE, TITANIUM, GLASS, MINIMAL, ABSTRACT, FOLD }
enum class WidgetKind { DIGITAL_CLOCK, ANALOG_CLOCK, DATE, CALENDAR, BATTERY, INFO }

data class IconItem(val id:String,val label:String,val drawable:Int,val collection:IconCollection=IconCollection.BLACK,val tier:ContentTier=ContentTier.FREE)
data class WallpaperItem(val id:String,val title:String,val category:WallpaperCategory,val drawable:Int,val tier:ContentTier=ContentTier.FREE)
data class StylePack(val id:String,val title:String,val collection:IconCollection,val wallpaperId:String?,val widgetKinds:List<WidgetKind>,val tier:ContentTier)
