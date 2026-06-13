package com.aptv.app

import android.app.Application
import com.aptv.app.ads.AdmobManager
import com.aptv.app.data.iptv.IptvRepository
import com.aptv.app.data.purchase.PurchaseManager
import com.aptv.app.data.purchase.PremiumStatusRepository

class AptvApplication : Application() {

    lateinit var purchaseManager: PurchaseManager
        private set

    lateinit var premiumRepository: PremiumStatusRepository
        private set

    lateinit var admobManager: AdmobManager
        private set

    lateinit var iptvRepository: IptvRepository
        private set

    override fun onCreate() {
        super.onCreate()

        purchaseManager = PurchaseManager(this)
        purchaseManager.initialize()

        premiumRepository = PremiumStatusRepository(this)

        admobManager = AdmobManager(this)
        admobManager.initialize()

        iptvRepository = IptvRepository(this, purchaseManager)
    }
}
