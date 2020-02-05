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
    Adapty.activate("YOUR_APP_KEY")
}
```

### Get customer profile and subscription status

```Kotlin
Adapty.getProfile() { error, profile ->
    if (profile.paidAccessLevels["level_configured_in_dashboard"]?.isActive == true) {
        /* Grant user access to paid functions of the app */
    }
}
```

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
) { error, profile ->
    /* ... */
}
```

All properties are optional.  
For **`gender`** possible values are: **`m`**, **`f`**, but you can also pass custom string value.

### Make purchase

```Kotlin
Adapty.makePurchase(product) { error, profile, purchaseData ->
    if (profile.paidAccessLevels["level_configured_in_dashboard"]?.isActive == true) {
        /* Grant user access to paid functions of the app */
    }
    
    /* purchaseData is a Dictionary, containing all info about purchase from Play Store */
}
```

### Restore purchases

```Kotlin
Adapty.restorePurchases() { error, profile ->
    /* ... */
}
```

