package com.abhayattcc.dictionaryreader.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.*

class BillingManager(context: Context, private val onPremiumStatusChanged: (Boolean) -> Unit) {
    private val billingClient: BillingClient
    private var isPremium = false

    init {
        billingClient = BillingClient.newBuilder(context)
            .setListener { billingResult, purchases ->
                if (billingResult.responseCode == BillingResponseCode.OK && purchases != null) {
                    for (purchase in purchases) {
                        handlePurchase(purchase)
                    }
                }
            }
            .enablePendingPurchases()
            .build()

        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    queryPurchases()
                }
            }

            override fun onBillingServiceDisconnected() {
                // Handle disconnection
            }
        })
    }

    private fun queryPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.INAPP) { _, purchases ->
            isPremium = purchases.any { it.isAcknowledged && it.products.contains("premium_subscription") }
            onPremiumStatusChanged(isPremium)
        }
    }

    fun launchBillingFlow(activity: Activity) {
        val productDetailsParamsList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId("premium_subscription")
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productDetailsParamsList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingResponseCode.OK && productDetailsList.isNotEmpty()) {
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetails(productDetailsList[0])
                    .build()
                billingClient.launchBillingFlow(activity, flowParams)
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged) {
            val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(acknowledgePurchaseParams) { billingResult ->
                if (billingResult.responseCode == BillingResponseCode.OK) {
                    isPremium = true
                    onPremiumStatusChanged(true)
                }
            }
        }
    }
}