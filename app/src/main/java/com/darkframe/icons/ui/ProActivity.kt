package com.darkframe.icons.ui

import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.billingclient.api.ProductDetails
import com.darkframe.icons.R
import com.darkframe.icons.billing.Entitlement
import com.darkframe.icons.billing.PlayBillingManager

class ProActivity : AppCompatActivity(), PlayBillingManager.Listener {
    private lateinit var billing: PlayBillingManager
    private lateinit var status: TextView
    private lateinit var buy: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "DarkFrame Pro"

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(48, 72, 48, 48)
            setBackgroundColor(getColor(R.color.df_background))
        }
        root.addView(TextView(this).apply {
            text = "DarkFrame Pro"
            textSize = 30f
            setTextColor(getColor(R.color.df_text_primary))
        })
        root.addView(TextView(this).apply {
            text = "Unlock Frost, Titanium and Glass. One purchase, restored through Google Play."
            textSize = 15f
            setTextColor(getColor(R.color.df_text_secondary))
            setPadding(0, 16, 0, 32)
        })
        status = TextView(this).apply {
            text = "Connecting to Google Play..."
            textSize = 14f
            setTextColor(getColor(R.color.df_text_secondary))
            setPadding(0, 0, 0, 24)
        }
        root.addView(status)
        buy = Button(this).apply {
            text = "Loading Pro..."
            isEnabled = false
            setOnClickListener { billing.launchPurchase(this@ProActivity) }
        }
        root.addView(buy)
        root.addView(Button(this).apply {
            text = "Restore purchase"
            setOnClickListener { billing.restorePurchases() }
        })
        setContentView(root)

        billing = PlayBillingManager(this, this)
        billing.start()
    }

    override fun onBillingReady(product: ProductDetails?) {
        runOnUiThread {
            if (product == null) {
                status.text = "Pro product is not configured in Google Play for this build."
                buy.text = "Pro unavailable"
                buy.isEnabled = false
            } else {
                val price = product.oneTimePurchaseOfferDetails?.formattedPrice ?: "Google Play"
                status.text = "Secure purchase handled by Google Play."
                buy.text = "Unlock Pro · $price"
                buy.isEnabled = true
            }
        }
    }

    override fun onEntitlementChanged(entitlement: Entitlement) {
        runOnUiThread {
            if (entitlement == Entitlement.PRO) {
                status.text = "DarkFrame Pro is active on this Google Play account."
                buy.text = "Pro active"
                buy.isEnabled = false
            }
        }
    }

    override fun onBillingMessage(message: String) {
        runOnUiThread { Toast.makeText(this, message, Toast.LENGTH_LONG).show() }
    }

    override fun onDestroy() {
        billing.end()
        super.onDestroy()
    }
}
