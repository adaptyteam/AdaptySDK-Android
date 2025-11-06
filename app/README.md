# Adapty SDK Example App

This is a sample Android application that demonstrates Adapty SDK integration.

## Running the Example

1. Clone the repository:
```bash
git clone https://github.com/adaptyteam/AdaptySDK-Android.git
cd AdaptySDK-Android
```

2. Build and run the example app:
```bash
./gradlew app:assembleDebug
```

## Configuration

To enable test purchases, configure the following:

1. Replace the Adapty API key in `src/main/java/com/adapty/example/App.kt`:
    ```kotlin
    val adaptyConfig = AdaptyConfig.Builder("YOUR_ADAPTY_KEY").build()
    ```
    Change `"YOUR_ADAPTY_KEY"` to your key from adapty the Adapty dashboard.

2. Replace the `applicationId` in `build.gradle`:
    ```gradle
    android {
        defaultConfig {
            applicationId "com.adapty.example"  // Replace with your own package name
        }
    }
    ```

