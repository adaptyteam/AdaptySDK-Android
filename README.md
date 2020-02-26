# Adapty Android SDK

![Adapty: CRM for mobile apps with subscriptions](/adapty.png)

Adapty helps you track business metrics, and lets you run ad campaigns targeted at churned users faster, written in Kotlin. https://adapty.io/

## Requirements

- Android 5.0+

## Installation

### Gradle

```
dependencies {
    implementation 'com.adapty:1.0.0’
}
```

## Usage

### Configure your app

Add the following to `Application` class:

```Kotlin
override fun onCreate() {
    super.onCreate()
    Adapty.activate(сontext, "YOUR_APP_KEY", customerUserId: "YOUR_USER_ID")
}
```
If your app doesn't have user IDs, you can use **`.activate("YOUR_APP_KEY")`** or pass null for the **`customerUserId`**. Anyway, you can update **`customerUserId`** later within user update request.

### Update customer profile

```Kotlin
Adapty.updateProfile(
    customerUserId: "<id-in-your-system>",
    email: "user@adapty.io",
    advertisingId: "###############",
    phoneNumber: "+1-###-###-####",
    facebookUserId: "###############",
    mixpanelUserId: "###############",
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

### Make purchase

```Kotlin
Adapty.makePurchase(activity, purchaseType, productId) { purchaseData, response, error ->
    if (error == null) {
     // successful purchase
    }
}
```

### Restore purchases

```Kotlin
Adapty.restorePurchases(activity, purchasesType) { error ->
    /* ... */
}
```

### Validate your receipt

```Kotlin
Adapty.validateReceipt(productId, purchaseToken) { response, error -> {
   /* ... */
}
```

**`productId`** and **`purchaseToken`** are required and can't be empty.

