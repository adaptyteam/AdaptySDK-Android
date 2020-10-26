# Attribution tracker integration

Adapty has support for most popular attribution systems which allows you to track how much revenue was driven by each source and ad network.
To integrate with attribution system, just pass attribution you receive to Adapty method.

```Kotlin
Adapty.updateAttribution(
    attribution: Map,
    source: AttributionType,
    networkUserId: String?
)
```

**`attribution`** is **`Map`**, **`JSONObject`** or **`AdjustAttribution`**.
For **`source`** possible values are: **`AttributionType.ADJUST`**, **`AttributionType.APPSFLYER`** and **`AttributionType.BRANCH`**.

### Adjust integration

To integrate with [Adjust](https://www.adjust.com/), just pass attribution you receive from OnAttributionChangedListener of Adjust Android SDK.

```Kotlin
val config = AdjustConfig(context, adjustAppToken, environment)
config.setOnAttributionChangedListener { attribution ->
    attribution?.let { attribution ->
        Adapty.updateAttribution(attribution, AttributionType.ADJUST)
    }
}
Adjust.onCreate(config)
```

### AppsFlyer integration

To integrate with [AppsFlyer](https://www.appsflyer.com/), just pass attribution you receive from AppsFlyerConversionListener of AppsFlyer Android SDK.

```Kotlin
val conversionListener: AppsFlyerConversionListener = object : AppsFlyerConversionListener {
            override fun onConversionDataSuccess(conversionData: Map<String, Any>) {
                // It's important to include the network user ID
                Adapty.updateAttribution(
                    conversionData,
                    AttributionType.APPSFLYER,
                    AppsFlyerLib.getInstance().getAppsFlyerUID(context)
                )
            }

            override fun onConversionDataFail(errorMessage: String) { }

            override fun onAppOpenAttribution(conversionData: Map<String, String>) {
                // It's important to include the network user ID
                Adapty.updateAttribution(
                    conversionData,
                    AttributionType.APPSFLYER,
                    AppsFlyerLib.getInstance().getAppsFlyerUID(context)
                )
            }

            override fun onAttributionFailure(errorMessage: String) { }
        }
AppsFlyerLib.getInstance().init(API_KEY_APPSFLYER, conversionListener, context)
```

### Branch integration

[Branch](https://branch.io/) integration example.

To connect Branch user and Adapty user, make sure you provide your customerUserId as Branch Identity id.
If you prefer to not use customerUserId in Branch, user networkUserId param in attribution method to specify the Branch user ID to attach to.

```Kotlin
// login and update attribution
Branch.getAutoInstance(this)
    .setIdentity(YOUR_USER_ID) { referringParams, error ->
        referringParams?.let { params ->
            Adapty.updateAttribution(params, AttributionType.BRANCH)
        }
    }

// logout
Branch.getAutoInstance(context).logout()
```