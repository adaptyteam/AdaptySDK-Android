package com.adapty.example

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.adapty.Adapty
import com.adapty.models.AdaptyAttributionSource
import com.adapty.models.AdaptyConfig
import com.adapty.utils.AdaptyLogLevel
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)

        Adapty.logLevel = if (BuildConfig.DEBUG) AdaptyLogLevel.VERBOSE else AdaptyLogLevel.NONE

        val adaptyConfig = AdaptyConfig.Builder("YOUR_ADAPTY_KEY").build()

        Adapty.activate(this, adaptyConfig)

        val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
                Adapty.updateAttribution(conversionData, AdaptyAttributionSource.APPSFLYER, AppsFlyerLib.getInstance().getAppsFlyerUID(this@App)) { error ->
                    //
                }
            }

            override fun onConversionDataFail(errorMessage: String) {

            }

            override fun onAppOpenAttribution(conversionData: Map<String, String>) {
                Adapty.updateAttribution(conversionData, AdaptyAttributionSource.APPSFLYER, AppsFlyerLib.getInstance().getAppsFlyerUID(this@App)) { error ->
                    //
                }
            }

            override fun onAttributionFailure(errorMessage: String) {

            }
        }
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_KEY", conversionListener, applicationContext)
        AppsFlyerLib.getInstance().startTracking(this)

        val config = AdjustConfig(this, "YOUR_ADJUST_APP_TOKEN", "YOUR_ADJUST_ENVIRONMENT")
        config.setOnAttributionChangedListener { attribution ->
            attribution?.let { attribution ->
                Adapty.updateAttribution(attribution, AdaptyAttributionSource.ADJUST) { error ->
                    //
                }
            }
        }
        Adjust.onCreate(config)
    }
}