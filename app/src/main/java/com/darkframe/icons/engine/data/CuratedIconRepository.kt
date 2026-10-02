package com.darkframe.icons.engine.data

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.annotation.DrawableRes
import com.darkframe.icons.R
import com.darkframe.icons.engine.domain.AppIdentity
import com.darkframe.icons.engine.domain.CuratedEntry
import com.darkframe.icons.engine.domain.CuratedIconIndex
import org.xmlpull.v1.XmlPullParser

/**
 * DarkFrame's handmade icon overrides.
 *
 * **These are quality overrides, not a supported-app list.** The engine themes every app the
 * device exposes; an entry here only says "we have drawn a better glyph for this one than its
 * installed icon, prefer ours". Shipping zero curated icons would not reduce coverage by a single
 * app, only polish.
 *
 * Curated artwork is authored as a *glyph on transparency* (`dfg_*`), with no background of its
 * own, so the same drawing works in all six collections: the collection supplies the container and
 * the glyph treatment. That is why curated and generated icons sit side by side in a grid without
 * looking like two different products.
 *
 * Read from [R.xml.df_curated], which is separate from [R.xml.appfilter] on purpose — see
 * `docs/LAUNCHER_SUPPORT.md`. `appfilter.xml` is the launcher-facing icon-pack contract and maps
 * to complete pre-composed tiles; this file is the engine's internal override map.
 */
class CuratedIconRepository(private val context: Context) {

    @Volatile
    private var index: CuratedIconIndex? = null

    private val drawableIdCache = HashMap<String, Int>()

    /** Parsed once, then held for the process lifetime. Resource XML cannot change at runtime. */
    fun index(): CuratedIconIndex {
        index?.let { return it }
        return synchronized(this) {
            index ?: parse().also { index = it }
        }
    }

    fun hasOverrideFor(identity: AppIdentity): Boolean =
        curatedDrawableId(identity) != null

    /**
     * Resource id of the curated glyph for [identity], or null when there is none — which is the
     * normal case and simply means the installed icon is used as the source instead.
     *
     * Returns null, rather than a broken id, when an `appfilter` row names a drawable that is not
     * in the APK.
     */
    @DrawableRes
    fun curatedDrawableId(identity: AppIdentity): Int? {
        val name = index().drawableNameFor(identity) ?: return null
        return drawableIdFor(name)
    }

    @DrawableRes
    private fun drawableIdFor(name: String): Int? = synchronized(drawableIdCache) {
        drawableIdCache.getOrPut(name) {
            // getIdentifier is the only way to go from an appfilter's drawable *name* to a
            // resource id. The alternative — a generated name-to-id map — would have to be
            // regenerated on every artwork change and is a worse trade for a one-off,
            // cached-for-the-process-lifetime lookup.
            @Suppress("DiscouragedApi")
            context.resources.getIdentifier(name, "drawable", context.packageName)
        }.takeIf { it != 0 }
    }

    private fun parse(): CuratedIconIndex {
        val entries = ArrayList<CuratedEntry>()
        var parser: XmlResourceParser? = null
        try {
            parser = context.resources.getXml(R.xml.df_curated)
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    entries += CuratedEntry(
                        drawableName = parser.getAttributeValue(null, "drawable").orEmpty(),
                        component = parser.getAttributeValue(null, "component"),
                        packageName = parser.getAttributeValue(null, "package"),
                    )
                }
                event = parser.next()
            }
        } catch (error: Exception) {
            // A malformed override file must degrade to "no curated artwork", never to a device
            // whose app drawer fails to load.
            return CuratedIconIndex.from(entries)
        } finally {
            parser?.close()
        }
        return CuratedIconIndex.from(entries)
    }
}
