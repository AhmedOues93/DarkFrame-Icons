package com.darkframe.icons.model

/** Free or paid. The one axis DarkFrame gates content on. */
enum class ContentTier { FREE, PRO }

/** The six icon collections. Each has exactly one [com.darkframe.icons.engine.domain.IconStyle]. */
enum class IconCollection { NOIR, COLOR_POP, FROST, TITANIUM, GLASS, PURE_AMOLED }

/**
 * Wallpaper groupings shown as filter chips. Only those with content are ever displayed.
 *
 * FROST replaces an earlier LIGHT, which no wallpaper ever used: a category with nothing in it is a
 * claim the product cannot keep, and naming it FROST ties it to the collection it pairs with.
 */
enum class WallpaperCategory { AMOLED, CARBON, GRAPHITE, TITANIUM, GLASS, FROST, MINIMAL, ABSTRACT, FOLD }

/** The widget shapes a look can recommend. */
enum class WidgetKind { DIGITAL_CLOCK, ANALOG_CLOCK, DATE, CALENDAR, BATTERY, INFO }
