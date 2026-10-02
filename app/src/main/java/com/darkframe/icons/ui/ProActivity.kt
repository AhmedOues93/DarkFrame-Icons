package com.darkframe.icons.ui

import android.os.Bundle
import android.widget.Toast
import com.android.billingclient.api.ProductDetails
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.PlayBillingManager
import com.darkframe.icons.billing.ProEntitlementStore
import com.darkframe.icons.ui.common.DarkFrameActivity
import com.darkframe.icons.ui.common.SectionScreen

/**
 * DarkFrame Pro: one lifetime purchase, through Google Play.
 *
 * Entitlement comes only from a PURCHASED Play purchase — there is no debug or local override, so a
 * sideloaded build simply shows Pro as unavailable, which is the correct behaviour rather than a
 * bug. Real validation happens through Play Internal Testing.
 */
class ProActivity : DarkFrameActivity(), PlayBillingManager.Listener {

    private lateinit var billing: PlayBillingManager
    private var product: ProductDetails? = null
    private var restoreRequested = false

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

        if (isPro) {
            screen.row(getString(R.string.pro_title), getString(R.string.pro_active))
        } else {
            screen.primaryButton(
                text = if (price != null) getString(R.string.pro_buy, price)
                else getString(R.string.pro_buy_unavailable),
                enabled = price != null,
            ) { billing.launchPurchase(this) }

            screen.secondaryButton(getString(R.string.pro_restore)) {
                restoreRequested = true
                billing.restorePurchases()
            }
        }

        screen.row(
            title = getString(R.string.home_collections),
            subtitle = PRO_CONTENT,
        )
    }

    override fun onBillingReady(product: ProductDetails?) {
        runOnUiThread {
            this.product = product
            render()
        }
    }

    override fun onEntitlementChanged(entitlement: Entitlement) {
        runOnUiThread {
            if (restoreRequested) {
                restoreRequested = false
                val message = if (entitlement == Entitlement.PRO) R.string.pro_restored
                else R.string.pro_none_found
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            }
            render()
        }
    }

    override fun onBillingMessage(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        billing.end()
        super.onDestroy()
    }

    private companion object {
        const val PRO_CONTENT = "Frost · Titanium · Glass, and every Pro wallpaper"
    }
}
