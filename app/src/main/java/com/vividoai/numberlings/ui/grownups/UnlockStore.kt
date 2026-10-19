package com.vividoai.numberlings.ui.grownups

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.QueryProductDetailsParams
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

enum class UnlockEffect { STARTED, ALREADY_OWNED, NOT_ON_THIS_DEVICE }

/**
 * One non-consumable unlock: all six meadows, forever. No subscriptions, no
 * consumables, no nudges inside the child area. If Google Play isn't reachable,
 * or the family skips this via Community Unlock, everything else stays free.
 */
object UnlockStore {
    /** Real contents; family sees this unlock only inside the gated Grown-ups zone. */
    const val PRODUCT_ID = "numberlings_meadows_all"

    /**
     * ANSI: the wiring activates when the product id exists on Play Console.
     * Until then the Community Unlock hosts families; nothing nags in play.
     * Set true at launch (README, STORE_READY.md).
     */
    const val billingConfigured = false

    private var client: BillingClient? = null

    private val purchasesListener = PurchasesUpdatedListener { result, purchases ->
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            purchases?.forEach { purchase ->
                if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                    client?.acknowledgePurchase(
                        AcknowledgePurchaseParams.newBuilder()
                            .setPurchaseToken(purchase.purchaseToken)
                            .build()
                    ) { }
                }
            }
        }
        // Cancellations are quiet on purpose; the child's gameplay never depends on it.
    }

    private fun ensure(context: Context): BillingClient {
        val existing = client
        if (existing != null) return existing
        val built = BillingClient.newBuilder(context.applicationContext)
            .setListener(purchasesListener)
            .enablePendingPurchases(
                PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
            )
            .build()
        client = built
        return built
    }

    suspend fun purchase(context: Context, activity: Activity, productId: String = PRODUCT_ID): UnlockEffect {
        if (!billingConfigured) return UnlockEffect.NOT_ON_THIS_DEVICE
        val play = ensure(context)
        val ok = connect(play)
        if (!ok) return UnlockEffect.NOT_ON_THIS_DEVICE
        return buyProduct(play, activity, productId)
    }

    private suspend fun connect(play: BillingClient): Boolean = suspendCoroutine { continuation ->
        play.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                continuation.resume(result.responseCode == BillingClient.BillingResponseCode.OK)
            }

            override fun onBillingServiceDisconnected() {
                // Soft stop: the grown-up can tap Community Unlock instead.
            }
        })
    }

    private suspend fun buyProduct(
        play: BillingClient,
        activity: Activity,
        productId: String
    ): UnlockEffect = suspendCoroutine { continuation ->
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build()
                )
            )
            .build()

        play.queryProductDetailsAsync(params) { result: BillingResult,
            details: List<ProductDetails> ->
            val product = details.firstOrNull()
            if (result.responseCode != BillingClient.BillingResponseCode.OK || product == null) {
                continuation.resume(UnlockEffect.NOT_ON_THIS_DEVICE)
                return@queryProductDetailsAsync
            }
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(
                    listOf(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                            .setProductDetails(product)
                            .build()
                    )
                )
                .build()
            val launchResult = play.launchBillingFlow(activity, flowParams)
            continuation.resume(
                if (launchResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    UnlockEffect.STARTED
                } else {
                    UnlockEffect.NOT_ON_THIS_DEVICE
                }
            )
        }
    }
}
