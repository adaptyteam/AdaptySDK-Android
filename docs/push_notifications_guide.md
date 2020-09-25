# Promo push notifications

We use FCM to send push notifications from Adapty. If you already use Firebase cloud messaging in your project, you can proceed to [Setup Adapty with Firebase](#setup-adapty-with-firebase)

## Add Firebase to your project

You can add Firebase to your project according to [official documentation](https://firebase.google.com/docs/cloud-messaging/android/client)

## Setup Adapty with Firebase

### Create your `AdaptyPushHandler`

To handle push notifications from Adapty you can inherit from **`AdaptyPushHandler`** like this:
#### Kotlin:
```Kotlin
class YourAdaptyPushHandler(context: Context) : AdaptyPushHandler(context) {

    override val clickAction = "YOUR_NOTIFICATION_CLICK_ACTION"
    
    override val smallIconResId = R.drawable.ic_notification_small_icon
}
```
#### Java:
```Java
public class YourAdaptyPushHandler extends AdaptyPushHandler {

    public YourAdaptyPushHandler(@NotNull Context context) {
        super(context);
    }

    @NotNull
    @Override
    public String getClickAction() {
        return "YOUR_NOTIFICATION_CLICK_ACTION";
    }

    @Override
    public int getSmallIconResId() {
        return R.drawable.ic_notification_small_icon;
    }
}
```

**`clickAction`** refers to [click_action](https://firebase.google.com/docs/cloud-messaging/http-server-ref) parameter from Firebase notification message body. You also need to declare the same action in your *AndroidManifest* in Activity you want to be launched after push is clicked:
```XML
<activity android:name=".YourActivity">
    <!-- your intent-filters-->
    <intent-filter>
        <action android:name="YOUR_NOTIFICATION_CLICK_ACTION" />
        <category android:name="android.intent.category.DEFAULT" />
    </intent-filter>
</activity>
```

Optional properties of **`AdaptyPushHandler`** (or getters in case of Java) you can override:
* **`largeIcon`** is a nullable Bitmap (ignored in case of null), refers to `setLargeIcon` from Notification.Builder
* **`customizedNotificationBuilder`** is a nullable `Notification.Builder` (ignored in case of null). In this case you **don't need** to specify `setSmallIcon`, `setContentIntent`, `setContentTitle` and `setContentText` in your custom builder as it will be overwritten by Adapty SDK. If you override **`customizedNotificationBuilder`** with non-null value, property **`largeIcon`** **is ignored** (so you need to specify it in your custom builder if needed).
* **`channelId`** is the ID of [notification channel](https://developer.android.com/guide/topics/ui/notifiers/notifications.html#ManageChannels) you want promo notifications to be associated with. You can override with the ID of already existing channel (if the channel exists it won't be overwritten). If you pass the ID of unexisting channel, Adapty will create new one where ID and name equal to ID you passed. If you don't override **`channelId`**, Adapty will create its default channel called "Offers".

### Setup your messaging service to work with promo notifications

You only need to do a few things - register new Firebase tokens and delegate handling promo notifications to your `AdaptyPushHandler` - just like in the example below:
```Kotlin
class YourFirebaseMessagingService : FirebaseMessagingService() {

    private val pushHandler: YourAdaptyPushHandler by lazy {
        YourAdaptyPushHandler(this)
    }

    override fun onNewToken(p0: String) {
        super.onNewToken(p0)
        Adapty.refreshPushToken(p0)
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        if (!pushHandler.handleNotification(p0.data)) {
            /*
            here is your logic for other notifications
            that haven't been handled by Adapty
             */
        }
    }
}
```

### Handle promo notifications in your Activity
Clicking on the notification will open the Activity where you specified your *action*. You can handle an Intent to your Activity like this:
#### Kotlin:
```Kotlin
class YourActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //your logic
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent) {
        if (Adapty.handlePromoIntent(intent) { promo, error ->
                //your logic for callback
            }
        ) {
            //your logic for the case user did click on promo notification,
            //for example show loading indicator
        } else {
            //your logic for other cases
        }
    }
}
```
#### Java:
```Java
public class YourActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //your logic
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (Adapty.handlePromoIntent(intent, (promo, error) -> {
            //your logic for callback
            //please note that promo can be null
            return null;
        })) {
            //your logic for the case user did click on promo notification,
            //for example show loading indicator
        } else {
            //your logic for other cases
        }
    }
}
```
