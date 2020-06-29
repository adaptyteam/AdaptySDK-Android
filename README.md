# Adapty Android SDK

![Adapty: CRM for mobile apps with subscriptions](/adapty.png)

## Requirements

- Android 5.0+

## Installation

### Gradle

Add it in your root build.gradle at the end of repositories:

```Kotlin
allprojects {
    repositories {
        ...
        maven { url 'https://jitpack.io' }
    }
}
```

Add dependency:

```Kotlin
dependencies {
    implementation 'com.github.adaptyteam:AdaptySDK-Android:0.2.7'
}
```

## Usage

### Configure your app

Add the following to your `Application` class:

```Kotlin
override fun onCreate() {
    super.onCreate()
    Adapty.activate(applicationContext, "PUBLIC_SDK_KEY", customerUserId: "YOUR_USER_ID")
}
```
If your app doesn't have user IDs, you can use **`.activate("PUBLIC_SDK_KEY")`** or pass null for the **`customerUserId`**. 
Anyway, you can update **`customerUserId`** later within **`.identify()`** request.

### Convert anonymous user to identifiable user

If you don't have an customerUserId on instantiation, you can set it later at any time with the .identify() method. The most common cases are after registration, when a user switches from being an anonymous user (with a undefined customerUserId) to an authenticated user with some ID.

```Kotlin
Adapty.identify("YOUR_USER_ID") { error ->
    if (error == null) {
        // successful identify
    }
}
```

### Observer mode
In some cases, if you have already built a functioning subscription system, it may not be possible or feasible to use the Adapty SDK to make purchases. However, you can still use the SDK to get access to the data.

To do so, at any purchase or restore in your application, you need to call the .syncPurchases() method to record the action in Adapty

```Kotlin
Adapty.syncPurchases()
```

### Update customer profile

```Kotlin
Adapty.updateProfile(
    email: "user@adapty.io",
    phoneNumber: "+1-###-###-####",
    facebookUserId: "###############",
    mixpanelUserId: "###############",
    appmetricaProfileId: "###",
    appmetricaDeviceId: "###",
    amplitudeUserId: "###############",
    firstName: "John",
    lastName: "Doe",
    gender: "m",
    birthday: Date
) { error ->
    /* ... */
}
```

All properties are optional.  
For **`gender`** possible values are: **`m`**, **`f`**, but you can also pass custom string value.

### Get purchase containers (paywalls)

```Kotlin
Adapty.getPurchaseContainers(activity) { containers, products, state, error ->
    // if error is empty, containers should contain info about your paywalls, products contains info about all your products
}
```
For state possible values are: cached, synced. First means that data was taken from local cache, second means that data was updated from remote server.

### Make purchase

```Kotlin
Adapty.makePurchase(activity, purchaseType, productId) { purchaseData, response, error ->
    if (error == null) {
     // successful purchase
    }
}
```
**`purchaseType`** and **`productID`** are required and can't be empty. **`purchaseType`** can be one of: **`SUBS`** and **`INAPP`**. Adapty handles subscription offers signing for you as well.

### Restore purchases

```Kotlin
Adapty.restorePurchases(activity) { response, error ->
    /* ... */
}
```

### Validate purchase

```Kotlin
Adapty.validatePurchase(productId, purchaseToken) { response, error -> {
   /* ... */
}
```

**`productId`** and **`purchaseToken`** are required and can't be empty.

### Get user purchases info

```Kotlin
Adapty.getPurchaserInfo { purchaserInfo, error ->
    // purchaserInfo object contains all of the purchase and subscription data available about the user
}
```

The **`purchaserInfo`** object gives you access to the following information about a user:

| Name  | Description |
| -------- | ------------- |
| promotionalOfferEligibility | Boolean indicating whether the promotional offer is available for the customer. |
| introductoryOfferEligibility | Boolean indicating whether the introductory offer is available for the customer. |
| paidAccessLevels | Dictionary where the keys are paid access level identifiers configured by developer in Adapty dashboard. Values are PaidAccessLevelsInfoModel objects. Can be null if the customer has no access levels. |
| subscriptions | Dictionary where the keys are vendor product ids. Values are SubscriptionsInfoModel objects. Can be null if the customer has no subscriptions. |
| nonSubscriptions | Dictionary where the keys are vendor product ids. Values are array[] of NonSubscriptionsInfoModel objects. Can be null if the customer has no purchases. |

**`paidAccessLevels`** stores info about current users access level.

| Name  | Description |
| -------- | ------------- |
| id | Paid Access Level identifier configured by developer in Adapty dashboard. |
| isActive | Boolean indicating whether the paid access level is active. |
| vendorProductId | Identifier of the product in vendor system (App Store/Google Play etc.) that unlocked this access level. |
| store | The store that unlocked this subscription, can be one of: **app_store**, **play_store** & **adapty**. |
| activatedAt | The ISO 8601 datetime when access level was activated (may be in the future). |
| renewedAt | The ISO 8601 datetime when access level was renewed. |
| expiresAt | The ISO 8601 datetime when access level will expire (may be in the past and may be null for lifetime access). |
| isLifetime | Boolean indicating whether the paid access level is active for lifetime (no expiration date). If set to true you shouldn't use **expires_at**. |
| activeIntroductoryOfferType | The type of active introductory offer. Possible values are: **free_trial**, **pay_as_you_go** & **pay_up_front**. If the value is not null it means that offer was applied during the current subscription period. |
| activePromotionalOfferType | The type of active promotional offer. Possible values are: **free_trial**, **pay_as_you_go** & **pay_up_front**. If the value is not null it means that offer was applied during the current subscription period. |
| willRenew | Boolean indicating whether auto renewable subscription is set to renew. |
| isInGracePeriod | Boolean indicating whether auto renewable subscription is in grace period. |
| unsubscribedAt | The ISO 8601 datetime when auto renewable subscription was cancelled. Subscription can still be active, it just means that auto renewal turned off. Will set to null if the user reactivates subscription. |
| billingIssueDetectedAt | The ISO 8601 datetime when billing issue was detected (vendor was not able to charge the card). Subscription can still be active. Will set to null if the charge was successful. |

**`subscriptions`** stores info about vendor subscription.

| Name  | Description |
| -------- | ------------- |
| isActive | Boolean indicating whether the subscription is active. |
| vendorProductId | Identifier of the product in vendor system (App Store/Google Play etc.). |
| store | Store where the product was purchased. Possible values are: **app_store**, **play_store** & **adapty**. |
| activatedAt | The ISO 8601 datetime when access level was activated (may be in the future). |
| renewedAt | The ISO 8601 datetime when access level was renewed. |
| expiresAt | The ISO 8601 datetime when access level will expire (may be in the past and may be null for lifetime access). |
| startsAt | The ISO 8601 datetime when access level stared. |
| isLifetime | Boolean indicating whether the subscription is active for lifetime (no expiration date). If set to true you shouldn't use **expires_at**. |
| activeIntroductoryOfferType | The type of active introductory offer. Possible values are: **free_trial**, **pay_as_you_go** & **pay_up_front**. If the value is not null it means that offer was applied during the current subscription period. |
| activePromotionalOfferType | The type of active promotional offer. Possible values are: **free_trial**, **pay_as_you_go** & **pay_up_front**. If the value is not null it means that offer was applied during the current subscription period. |
| willRenew | Boolean indicating whether auto renewable subscription is set to renew. |
| isInGracePeriod | Boolean indicating whether auto renewable subscription is in grace period. |
| unsubscribedAt | The ISO 8601 datetime when auto renewable subscription was cancelled. Subscription can still be active, it just means that auto renewal turned off. Will set to null if the user reactivates subscription. |
| billingIssueDetectedAt | The ISO 8601 datetime when billing issue was detected (vendor was not able to charge the card). Subscription can still be active. Will set to null if the charge was successful. |
| isSandbox | Boolean indicating whether the product was purchased in sandbox or production environment. |
| vendorTransactionId | Transaction id in vendor system. |
| vendorOriginalTransactionId | Original transaction id in vendor system. For auto renewable subscription this will be id of the first transaction in the subscription. |

**`nonSubscriptions `** stores info about purchases that are not subscriptions.

| Name  | Description |
| -------- | ------------- |
| purchaseId | Identifier of the purchase in Adapty. You can use it to unsure that you've already processed this purchase (for example tracking one time products). |
| vendorProductId | Identifier of the product in vendor system (App Store/Google Play etc.). |
| store | Store where the product was purchased. Possible values are: **app_store**, **play_store** & **adapty**. |
| purchasedAt | The ISO 8601 datetime when the product was purchased. |
| isOneTime | Boolean indicating whether the product should only be processed once. If true, the purchase will be returned by Adapty API one time only. |
| isSandbox | Boolean indicating whether the product was purchased in sandbox or production environment. |
| vendorTransactionId | Transaction id in vendor system. |
| vendorOriginalTransactionId | Original transaction id in vendor system. For auto renewable subscription this will be id of the first transaction in the subscription. |

### Checking if a user is subscribed 

The subscription status for a user can easily be determined from **`paidAccessLevels`** property of **`purchaserInfo`** object by **`isActive`** property inside.

```Kotlin
Adapty.getPurchaserInfo { purchaserInfo, error ->
    if (purchaserInfo?.paidAccessLevels["level_configured_in_dashboard"]?.isActive) {
    
    }
}
```

### Listening for purchaser info updates
You can respond to any changes in purchaser info by conforming to an optional delegate method, didReceivePurchaserInfo. This will fire whenever we receive a change in purchaser info.
```Kotlin
Adapty.setOnPurchaserInfoUpdatedListener(object : OnPurchaserInfoUpdatedListener {
            override fun didReceiveUpdatedPurchaserInfo(purchaserInfo: PurchaserInfoModel) {
                // handle any changes to purchaserInfo
            }
        })
```

### Logout user

Makes your user anonymous.

```Kotlin
Adapty.logout { error ->
    if (error == null) {
        // successful logout
    }
}
```

