package com.aptv.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AdmobManager(
    private val context: Context
) {
    companion object {
        // 测试 ID（上线前必须替换为正式 ID）
        const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
        const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        const val INTERSTITIAL_SHOW_INTERVAL = 5
    }

    private var interstitialAd: InterstitialAd? = null
    private var channelSwitchCount = 0

    private val _isBannerLoaded = MutableStateFlow(false)
    val isBannerLoaded: StateFlow<Boolean> = _isBannerLoaded.asStateFlow()

    fun initialize() {
        MobileAds.initialize(context) {
            loadInterstitialAd()
        }
    }

    fun createBannerAd(): AdView {
        val adView = AdView(context).apply {
            adUnitId = BANNER_AD_UNIT_ID
            setAdSize(AdSize.BANNER)

            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    _isBannerLoaded.value = true
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    _isBannerLoaded.value = false
                }
            }

            loadAd(AdRequest.Builder().build())
        }
        return adView
    }

    private fun loadInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                }
            }
        )
    }

    fun tryShowInterstitial(
        activity: Activity,
        force: Boolean = false,
        onDismiss: () -> Unit = {}
    ) {
        if (!force) {
            channelSwitchCount++
            if (channelSwitchCount < INTERSTITIAL_SHOW_INTERVAL) return
            channelSwitchCount = 0
        }

        val ad = interstitialAd
        if (ad != null) {
            ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    interstitialAd = null
                    loadInterstitialAd()
                    onDismiss()
                }

                override fun onAdFailedToShowFullScreenContent(error: com.google.android.gms.ads.AdError) {
                    interstitialAd = null
                    loadInterstitialAd()
                    onDismiss()
                }
            }
            ad.show(activity)
        } else {
            loadInterstitialAd()
            onDismiss()
        }
    }

    fun destroyBannerAd() {
        _isBannerLoaded.value = false
    }
}
