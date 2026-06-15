package com.aptv.app.data.purchase

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
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.queryProductDetailsAsync
import com.aptv.app.data.purchase.model.PremiumFeature
import com.aptv.app.data.purchase.model.PurchaseState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Google Play Billing 管理器
 */
class PurchaseManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) : PurchasesUpdatedListener, BillingClientStateListener {

    private var billingClient: BillingClient? = null

    private val _purchaseState = MutableStateFlow(PurchaseState())
    val purchaseState: StateFlow<PurchaseState> = _purchaseState.asStateFlow()

    private val _lifetimeProductDetails = MutableStateFlow<ProductDetails?>(null)
    val lifetimeProductDetails: StateFlow<ProductDetails?> = _lifetimeProductDetails.asStateFlow()

    private val _adFreeProductDetails = MutableStateFlow<ProductDetails?>(null)
    val adFreeProductDetails: StateFlow<ProductDetails?> = _adFreeProductDetails.asStateFlow()

    private val _isBillingReady = MutableStateFlow(false)
    val isBillingReady: StateFlow<Boolean> = _isBillingReady.asStateFlow()

    val isAdFree: StateFlow<Boolean> = callbackFlow {
        _purchaseState.collect { state ->
            send(state.isAdFree)
        }
        awaitClose()
    }.stateIn(coroutineScope, SharingStarted.Eagerly, false)

    fun initialize() {
        billingClient = BillingClient.newBuilder(context)
            .setListener(this)
            .enablePendingPurchases()
            .build()
        billingClient?.startConnection(this)
    }

    override fun onBillingSetupFinished(billingResult: BillingResult) {
        if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
            _isBillingReady.value = true
            coroutineScope.launch {
                queryExistingPurchases()
                queryProductDetails()
            }
        }
    }

    override fun onBillingServiceDisconnected() {
        _isBillingReady.value = false
        try {
            billingClient?.startConnection(this)
        } catch (_: Exception) {}
    }

    private suspend fun queryProductDetails() {
        val productList = listOf(
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PurchaseState.PRODUCT_LIFETIME)
                .setProductType(BillingClient.ProductType.INAPP)
                .build(),
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(PurchaseState.PRODUCT_AD_FREE)
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        )

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient?.queryProductDetailsAsync(params) { _, productDetailsList ->
            productDetailsList.forEach { details ->
                when (details.productId) {
                    PurchaseState.PRODUCT_LIFETIME -> _lifetimeProductDetails.value = details
                    PurchaseState.PRODUCT_AD_FREE -> _adFreeProductDetails.value = details
                }
            }
        }
    }

    private fun queryExistingPurchases() {
        billingClient?.queryPurchasesAsync(
            com.android.billingclient.api.QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            handlePurchaseList(purchases)
        }
    }

    fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
        onResult: (Boolean, String?) -> Unit
    ) {
        if (!_isBillingReady.value) {
            onResult(false, "Billing service not ready")
            return
        }

        val targetDetails = when (productId) {
            PurchaseState.PRODUCT_LIFETIME -> _lifetimeProductDetails.value
            PurchaseState.PRODUCT_AD_FREE -> _adFreeProductDetails.value
            else -> null
        }

        if (targetDetails == null) {
            onResult(false, "Product not available")
            return
        }

        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(targetDetails)
                .build()
        )

        val billingFlowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()

        val billingResult = billingClient?.launchBillingFlow(activity, billingFlowParams)
        if (billingResult?.responseCode != BillingClient.BillingResponseCode.OK) {
            onResult(false, "Failed to launch purchase flow")
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (!purchases.isNullOrEmpty()) {
                    handlePurchaseList(purchases)
                }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {}
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                _purchaseState.value = _purchaseState.value.copy(isPremium = true)
            }
        }
    }

    private fun handlePurchaseList(purchases: List<Purchase>) {
        var hasLifetime = false
        var hasAdFree = false
        var latestPurchase: Purchase? = null

        for (purchase in purchases) {
            if (purchase.purchaseState == Purchase.PurchaseState.PURCHASED) {
                latestPurchase = purchase
                when {
                    purchase.products.contains(PurchaseState.PRODUCT_LIFETIME) -> hasLifetime = true
                    purchase.products.contains(PurchaseState.PRODUCT_AD_FREE) -> hasAdFree = true
                }

                if (!purchase.isAcknowledged) {
                    acknowledgePurchase(purchase)
                }
            }
        }

        _purchaseState.value = PurchaseState(
            isPremium = hasLifetime,
            isAdFree = hasLifetime || hasAdFree,
            purchaseTimeMs = latestPurchase?.purchaseTime ?: 0L,
            orderId = latestPurchase?.orderId,
            purchaseToken = latestPurchase?.purchaseToken,
            productId = latestPurchase?.products?.firstOrNull() ?: "",
            isAcknowledged = latestPurchase?.isAcknowledged ?: false
        )
    }

    private fun acknowledgePurchase(purchase: Purchase) {
        val acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()

        billingClient?.acknowledgePurchase(acknowledgePurchaseParams) { result ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                _purchaseState.value = _purchaseState.value.copy(isAcknowledged = true)
            }
        }
    }

    fun hasFeature(feature: PremiumFeature): Boolean {
        val state = _purchaseState.value
        return when (feature) {
            PremiumFeature.AD_FREE -> state.isAdFree
            PremiumFeature.UNLIMITED_PLAYLISTS,
            PremiumFeature.REAL_TIME_SUBTITLE,
            PremiumFeature.SUBTITLE_TRANSLATION,
            PremiumFeature.MULTI_SCREEN,
            PremiumFeature.CLOUD_SYNC -> state.isPremium
        }
    }

    fun restorePurchases(onResult: (Boolean) -> Unit = {}) {
        billingClient?.queryPurchasesAsync(
            com.android.billingclient.api.QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
        ) { _, purchases ->
            handlePurchaseList(purchases)
            onResult(_purchaseState.value.isPremium || _purchaseState.value.isAdFree)
        }
    }

    fun destroy() {
        billingClient?.endConnection()
    }
}
