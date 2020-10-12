package com.adapty.example

import androidx.multidex.MultiDex
import androidx.multidex.MultiDexApplication
import com.adapty.Adapty
import com.adapty.api.AttributionType
import com.adapty.utils.LogLevel
import com.appsflyer.AppsFlyerConversionListener
import com.appsflyer.AppsFlyerLib


class App : MultiDexApplication() {

    override fun onCreate() {
        super.onCreate()
        MultiDex.install(this)

        Adapty.setLogLevel(if (BuildConfig.DEBUG) LogLevel.VERBOSE else LogLevel.NONE)
        Adapty.activate(
            this,
            "YOUR_ADAPTY_KEY"
        )

        val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
                //Adapty.updateAttribution(conversionData, AttributionType.APPSFLYER)
            }

            override fun onConversionDataFail(errorMessage: String) {

            }

            override fun onAppOpenAttribution(conversionData: Map<String, String>) {
                //Adapty.updateAttribution(conversionData, AttributionType.APPSFLYER)
            }

            override fun onAttributionFailure(errorMessage: String) {

            }
        }
        AppsFlyerLib.getInstance().init("YOUR_APPSFLYER_KEY", conversionListener, applicationContext)
        AppsFlyerLib.getInstance().startTracking(this)
    }
}