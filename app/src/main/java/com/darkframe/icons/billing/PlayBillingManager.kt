package com.darkframe.icons.billing

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.StringRes
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.darkframe.icons.R

/**
 * Google Play Billing for the one-time DarkFrame Pro product.
 *
 * There is no debug path, no override and no local flag that grants Pro: entitlement comes from a
 * `PURCHASED` Play purchase and from the value that purchase last wrote, and nothing else in the
 * codebase can set it.
 *
 * What the entitlement should *be* for a given Play answer lives in [EntitlementPolicy] rather than
 * here, because that is the part a billing bug costs someone money over and the only part that can be
 * tested without a Play connection. This class does the Play-shaped work: connecting, retrying,
 * translating response codes into something a person can read, and acknowledging.
 */
class PlayBillingManager(
    context: Context,
    private val listener: Listener,
) : PurchasesUpdatedListener {

    interface Listener {
        fun onBillingReady(product: ProductDetails?)
        fun onEntitlementChanged(entitlement: Entitlement)

        /** A purchase is being settled. Neither granted nor refused yet. */
        fun onPurchasePending()
        fun onBillingMessage(@StringRes message: Int)
    }

    companion object {
        const val PRO_PRODUCT_ID = "darkframe_pro_lifetime"

        /** Reconnect backoff, capped. Play's own guidance is to back off rather than hammer. */
        private const val FIRST_RETRY_MS = 1_000L
        private const val MAX_RETRY_MS = 30_000L
    }

    private val appContext = context.applicationContext
    private val store = ProEntitlementStore(appContext)
    private val handler = Handler(Looper.getMainLooper())

    private var productDetails: ProductDetails? = null
    private var retryDelayMs = FIRST_RETRY_MS
    private var closed = false

    private val client = BillingClient.newBuilder(appContext)
        .setListener(this)
        // Pending purchases are a real state for a one-time product paid in cash or on a carrier
        // bill. Enabling them is what makes PurchaseStatus.PENDING reachable rather than silently
        // dropped by the library.
        .enablePendingPurchases()
        .build()

    fun start() {
        if (closed) return
        if (client.isReady) {
            refresh()
            return
        }
        connect()
    }

    private fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    retryDelayMs = FIRST_RETRY_MS
                    refresh()
                } else {
                    report(problemFor(result.responseCode))
                }
            }

            /**
             * Play disconnects routinely — an app update to Play itself does it. Reconnecting with a
             * capped backoff is what keeps the Pro screen working after one, instead of telling the
             * user to come back later and leaving them to it.
             */
            override fun onBillingServiceDisconnected() {
                if (closed) return
                val delay = retryDelayMs
                retryDelayMs = (retryDelayMs * 2).coerceAtMost(MAX_RETRY_MS)
                handler.postDelayed({ if (!closed) connect() }, delay)
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
                // The price being unavailable does not change what the user owns, so this reports a
                // problem and leaves the entitlement entirely alone.
                listener.onBillingReady(null)
                report(problemFor(result.responseCode))
            }
        }
    }

    fun launchPurchase(activity: Activity) {
        val details = productDetails
        if (details == null) {
            listener.onBillingMessage(R.string.pro_not_ready)
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
            report(problemFor(result.responseCode))
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
            val completed = result.responseCode == BillingClient.BillingResponseCode.OK
            apply(
                completed = completed,
                purchases = purchases,
                problem = if (completed) null else problemFor(result.responseCode),
            )
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        val completed = result.responseCode == BillingClient.BillingResponseCode.OK
        // A result that is not OK says nothing about what the user owns — notably ALREADY_OWNED,
        // which arrives with no purchase list and must not be read as "owns nothing". So a failed
        // flow re-queries rather than deciding from the callback.
        if (!completed) {
            report(problemFor(result.responseCode))
            if (result.responseCode == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
                restorePurchases()
            }
            return
        }
        apply(completed = true, purchases = purchases.orEmpty(), problem = null)
    }

    /**
     * One place where a Play answer becomes an entitlement, so every path obeys the same rules.
     */
    private fun apply(completed: Boolean, purchases: List<Purchase>, problem: BillingProblem?) {
        val ours = purchases.filter { PRO_PRODUCT_ID in it.products }
        val owned = ours.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED }
        val pending = ours.firstOrNull { it.purchaseState == Purchase.PurchaseState.PENDING }

        val decision = EntitlementPolicy.decide(
            queryCompleted = completed,
            status = when {
                owned != null -> PurchaseStatus.OWNED
                pending != null -> PurchaseStatus.PENDING
                else -> PurchaseStatus.NONE
            },
            acknowledged = owned?.isAcknowledged ?: false,
            cached = store.current(),
            problem = problem,
        )

        store.setPro(decision.entitlement == Entitlement.PRO)
        listener.onEntitlementChanged(decision.entitlement)
        if (decision.pending) listener.onPurchasePending()
        if (EntitlementPolicy.isWorthReporting(decision.problem)) report(decision.problem)

        if (decision.acknowledge && owned != null) acknowledge(owned)
    }

    /**
     * Acknowledgement is not housekeeping.
     *
     * Play refunds an unacknowledged purchase after three days and revokes the entitlement with it,
     * so a failure here is retried on every later query by the fact that `isAcknowledged` stays
     * false — which is why the retry needs no state of its own.
     */
    private fun acknowledge(purchase: Purchase) {
        client.acknowledgePurchase(
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build(),
        ) { result ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                listener.onBillingMessage(R.string.pro_ack_retry)
            }
        }
    }

    fun end() {
        closed = true
        handler.removeCallbacksAndMessages(null)
        if (client.isReady) client.endConnection()
    }

    private fun report(problem: BillingProblem?) {
        if (!EntitlementPolicy.isWorthReporting(problem)) return
        listener.onBillingMessage(messageFor(problem ?: BillingProblem.UNKNOWN))
    }

    /**
     * Play's response codes, as the handful of situations a user can act on.
     *
     * `debugMessage` is not used anywhere user-facing: it is written for developers, is not
     * localised, and routinely says things like "Server error, please try again" in the middle of a
     * purchase screen.
     */
    private fun problemFor(code: Int): BillingProblem = when (code) {
        BillingClient.BillingResponseCode.USER_CANCELED -> BillingProblem.CANCELLED
        BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> BillingProblem.ALREADY_OWNED
        BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
        BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE,
        BillingClient.BillingResponseCode.NETWORK_ERROR,
        -> BillingProblem.NETWORK

        BillingClient.BillingResponseCode.BILLING_UNAVAILABLE,
        BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED,
        -> BillingProblem.UNAVAILABLE

        BillingClient.BillingResponseCode.ITEM_UNAVAILABLE -> BillingProblem.ITEM_UNAVAILABLE
        else -> BillingProblem.UNKNOWN
    }

    @StringRes
    private fun messageFor(problem: BillingProblem): Int = when (problem) {
        BillingProblem.NETWORK -> R.string.pro_problem_network
        BillingProblem.UNAVAILABLE -> R.string.pro_problem_unavailable
        BillingProblem.ITEM_UNAVAILABLE -> R.string.pro_problem_item
        BillingProblem.CANCELLED, BillingProblem.ALREADY_OWNED, BillingProblem.UNKNOWN ->
            R.string.pro_problem_unknown
    }
}

/**
 * Where the entitlement is remembered between launches.
 *
 * Deliberately a local cache of a Play answer rather than a source of truth: it is written only by
 * [PlayBillingManager], and [EntitlementPolicy] decides when a Play answer is allowed to change it.
 * That is what makes Pro work on a plane and still honour a refund.
 */
class ProEntitlementStore(context: Context) : EntitlementProvider {
    private val prefs = context.applicationContext
        .getSharedPreferences("darkframe_billing", Context.MODE_PRIVATE)

    override fun current(): Entitlement =
        if (prefs.getBoolean(KEY_PRO, false)) Entitlement.PRO else Entitlement.FREE

    internal fun setPro(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_PRO, enabled).apply()
    }

    private companion object {
        const val KEY_PRO = "pro_verified"
    }
}
