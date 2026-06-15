package com.aptv.app.data.purchase

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aptv.app.data.purchase.model.PurchaseState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "premium_status")

class PremiumStatusRepository(
    private val context: Context
) {
    companion object {
        val KEY_IS_PREMIUM = booleanPreferencesKey("is_premium")
        val KEY_IS_ADFREE = booleanPreferencesKey("is_ad_free")
        val KEY_PURCHASE_TIME = longPreferencesKey("purchase_time_ms")
        val KEY_ORDER_ID = stringPreferencesKey("order_id")
        val KEY_PURCHASE_TOKEN = stringPreferencesKey("purchase_token")
        val KEY_PRODUCT_ID = stringPreferencesKey("product_id")
    }

    val cachedState: Flow<PurchaseState> = context.dataStore.data.map { prefs ->
        PurchaseState(
            isPremium = prefs[KEY_IS_PREMIUM] ?: false,
            isAdFree = prefs[KEY_IS_ADFREE] ?: false,
            purchaseTimeMs = prefs[KEY_PURCHASE_TIME] ?: 0L,
            orderId = prefs[KEY_ORDER_ID],
            purchaseToken = prefs[KEY_PURCHASE_TOKEN],
            productId = prefs[KEY_PRODUCT_ID] ?: PurchaseState.PRODUCT_LIFETIME,
            isAcknowledged = prefs[KEY_ORDER_ID] != null
        )
    }

    suspend fun cachePurchaseState(state: PurchaseState) {
        context.dataStore.edit { prefs ->
            prefs[KEY_IS_PREMIUM] = state.isPremium
            prefs[KEY_IS_ADFREE] = state.isAdFree
            if (state.purchaseTimeMs > 0) prefs[KEY_PURCHASE_TIME] = state.purchaseTimeMs
            state.orderId?.let { prefs[KEY_ORDER_ID] = it }
            state.purchaseToken?.let { prefs[KEY_PURCHASE_TOKEN] = it }
            prefs[KEY_PRODUCT_ID] = state.productId
        }
    }

    suspend fun clearCache() {
        context.dataStore.edit { it.clear() }
    }
}
