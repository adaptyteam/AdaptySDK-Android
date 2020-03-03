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
    implementation 'com.github.adaptyteam:AdaptySDK-Android:0.1.2'
}
```

## Usage

### Configure your app

Add the following to `Application` class:

```Kotlin
override fun onCreate() {
    super.onCreate()
    Adapty.activate(сontext, "PUBLIC_SDK_KEY", customerUserId: "PUBLIC_SDK_KEY")
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

### Update customer profile

```Kotlin
Adapty.updateProfile(
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

### Logout user

Makes your user anonymous.

```Kotlin
Adapty.logout { error ->
    if (error == null) {
        // successful logout
    }
}
```

**`productId`** and **`purchaseToken`** are required and can't be empty.

