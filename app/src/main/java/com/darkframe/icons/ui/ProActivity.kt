package com.darkframe.icons.ui

import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import com.android.billingclient.api.ProductDetails
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.PlayBillingManager
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.engine.domain.IconStyleCatalog
import com.darkframe.icons.engine.wallpaper.WallpaperCatalog
import com.darkframe.icons.model.ContentTier
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen

/**
 * DarkFrame Pro: one lifetime purchase, through Google Play.
 *
 * Entitlement comes only from a PURCHASED Play purchase — there is no debug or local override, so a
 * sideloaded build simply shows Pro as unavailable, which is the correct behaviour rather than a bug.
 * Real validation happens through Play Internal Testing.
 *
 * What the screen says it unlocks is counted from the catalogs rather than written out, so a Pro
 * collection or wallpaper added later cannot leave the sales copy describing a different product.
 */
class ProActivity : DarkFrameActivity(), PlayBillingManager.Listener {

    private lateinit var billing: PlayBillingManager
    private var product: ProductDetails? = null
    private var restoreRequested = false
    private var pending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        render()
        billing = PlayBillingManager(this, this)
        billing.start()
    }

    private fun render() {
        val isPro = ProEntitlementStore(this).current() == Entitlement.PRO
        val price = product?.oneTimePurchaseOfferDetails?.formattedPrice
        val screen = SectionScreen(this)
            .setUp(getString(R.string.pro_title), getString(R.string.pro_body))

        when {
            isPro -> screen.row(getString(R.string.pro_title), getString(R.string.pro_active))

            // A pending purchase is neither owned nor not owned: cash and carrier payments settle
            // over minutes or days. Offering the buy button again here is how a user ends up paying
            // twice, so it is replaced by the state they are actually in.
            pending -> screen.row(
                getString(R.string.pro_pending_title),
                getString(R.string.pro_pending_body),
            )

            else -> {
                screen.primaryButton(
                    text = if (price != null) getString(R.string.pro_buy, price)
                    else getString(R.string.pro_buy_unavailable),
                    enabled = price != null,
                ) { billing.launchPurchase(this) }
                if (price == null) screen.caption(getString(R.string.pro_price_pending))
            }
        }

        // Always offered, including when already Pro: the usual reason to tap it is a new device,
        // where the app does not yet know what this Google account owns.
        screen.secondaryButton(getString(R.string.pro_restore)) {
            restoreRequested = true
            billing.restorePurchases()
        }

        screen.header(getString(R.string.pro_includes))
        screen.row(
            title = getString(R.string.home_collections),
            subtitle = proCollections(),
        )
        screen.row(
            title = getString(R.string.pro_wallpapers),
            subtitle = resources.getQuantityString(
                R.plurals.pro_wallpaper_count,
                proWallpaperCount(),
                proWallpaperCount(),
            ),
        )
        screen.caption(getString(R.string.pro_free_note))
    }

    private fun proCollections(): String = IconStyleCatalog.all
        .filter { it.tier == ContentTier.PRO }
        .joinToString(" · ") { it.displayName }

    private fun proWallpaperCount(): Int = WallpaperCatalog.all.count { it.tier == ContentTier.PRO }

    override fun onBillingReady(product: ProductDetails?) {
        runOnUiThread {
            this.product = product
            render()
        }
    }

    override fun onEntitlementChanged(entitlement: Entitlement) {
        runOnUiThread {
            if (entitlement == Entitlement.PRO) pending = false
            if (restoreRequested) {
                restoreRequested = false
                // Only reported for an answered query: the manager keeps the cached entitlement when
                // Play could not be reached, and "no previous purchase found" would be a lie then.
                toast(
                    if (entitlement == Entitlement.PRO) R.string.pro_restored
                    else R.string.pro_none_found,
                )
            }
            render()
        }
    }

    override fun onPurchasePending() {
        runOnUiThread {
            pending = true
            render()
        }
    }

    override fun onBillingMessage(message: Int) {
        runOnUiThread {
            // A failed query means the restore produced no answer, so the "nothing found" message
            // must not also fire — otherwise an offline tap reports both at once.
            restoreRequested = false
            toast(message)
        }
    }

    private fun toast(@StringRes message: Int) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        billing.end()
        super.onDestroy()
    }
}
