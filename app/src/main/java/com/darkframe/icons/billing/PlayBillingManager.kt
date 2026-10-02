package com.darkframe.icons.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

/**
 * Real Google Play Billing entitlement for the one-time DarkFrame Pro product.
 * No debug/fake Pro path: entitlement is granted only for a PURCHASED Play purchase.
 */
class PlayBillingManager(
    context: Context,
    private val listener: Listener,
) : PurchasesUpdatedListener {

    interface Listener {
        fun onBillingReady(product: ProductDetails?)
        fun onEntitlementChanged(entitlement: Entitlement)
        fun onBillingMessage(message: String)
    }

    companion object {
        const val PRO_PRODUCT_ID = "darkframe_pro_lifetime"
    }

    private val appContext = context.applicationContext
    private var productDetails: ProductDetails? = null
    private var reconnecting = false

    private val client = BillingClient.newBuilder(appContext)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    fun start() {
        if (client.isReady) {
            refresh()
            return
        }
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                reconnecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    refresh()
                } else {
                    listener.onBillingMessage(result.debugMessage.ifBlank { "Google Play Billing unavailable." })
                }
            }

            override fun onBillingServiceDisconnected() {
                if (!reconnecting) {
                    reconnecting = true
                    listener.onBillingMessage("Google Play connection lost. Retry when you open Pro again.")
                }
            }
        })
    }

    private fun refresh() {
        queryProduct()
        restorePurchases()
    }

    private fun queryProduct() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PRO_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        client.queryProductDetailsAsync(params) { result, products ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = products.firstOrNull()
                listener.onBillingReady(productDetails)
            } else {
                listener.onBillingMessage(result.debugMessage.ifBlank { "Could not load Pro from Google Play." })
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            listener.onBillingMessage("Pro is not available from Google Play yet.")
            start()
            return
        }
        val item = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val result = client.launchBillingFlow(
            activity,
            BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(item)).build(),
        )
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            listener.onBillingMessage(result.debugMessage.ifBlank { "Could not open Google Play checkout." })
        }
    }

    fun restorePurchases() {
        if (!client.isReady) {
            start()
            return
        }
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                listener.onBillingMessage(result.debugMessage.ifBlank { "Could not restore purchases." })
                return@queryPurchasesAsync
            }
            applyPurchases(purchases)
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> applyPurchases(purchases.orEmpty())
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            else -> listener.onBillingMessage(result.debugMessage.ifBlank { "Purchase was not completed." })
        }
    }

    private fun applyPurchases(purchases: List<Purchase>) {
        val pro = purchases.firstOrNull {
            PRO_PRODUCT_ID in it.products && it.purchaseState == Purchase.PurchaseState.PURCHASED
        }
        if (pro == null) {
            ProEntitlementStore(appContext).setPro(false)
            listener.onEntitlementChanged(Entitlement.FREE)
            return
        }

        ProEntitlementStore(appContext).setPro(true)
        listener.onEntitlementChanged(Entitlement.PRO)

        if (!pro.isAcknowledged) {
            client.acknowledgePurchase(
                AcknowledgePurchaseParams.newBuilder().setPurchaseToken(pro.purchaseToken).build()
            ) { ack ->
                if (ack.responseCode != BillingClient.BillingResponseCode.OK) {
                    listener.onBillingMessage("Pro is active, but Google Play acknowledgement will be retried.")
                }
            }
        }
    }

    fun end() {
        if (client.isReady) client.endConnection()
    }
}

class ProEntitlementStore(context: Context) : EntitlementProvider {
    private val prefs = context.applicationContext.getSharedPreferences("darkframe_billing", Context.MODE_PRIVATE)

    override fun current(): Entitlement =
        if (prefs.getBoolean("pro_verified", false)) Entitlement.PRO else Entitlement.FREE

    internal fun setPro(enabled: Boolean) {
        prefs.edit().putBoolean("pro_verified", enabled).apply()
    }
}
