package com.adapty.example.promopush

/**
 * Setup Firebase Cloud Messaging (https://firebase.google.com/docs/cloud-messaging/android/client),
 * add your google-services.json and uncomment this section
 *
 * Don't forget to declare the service in your AndroidManifest.xml
 */

//import com.adapty.Adapty
//import com.google.firebase.messaging.FirebaseMessagingService
//import com.google.firebase.messaging.RemoteMessage
//
//class YourFirebaseMessagingService : FirebaseMessagingService() {
//
//    private val pushHandler: YourAdaptyPushHandler by lazy {
//        YourAdaptyPushHandler(this)
//    }
//
//    override fun onNewToken(p0: String) {
//        super.onNewToken(p0)
//        Adapty.refreshPushToken(p0)
//    }
//
//    override fun onMessageReceived(p0: RemoteMessage) {
//        super.onMessageReceived(p0)
//        if (!pushHandler.handleNotification(p0.data)) {
//            /*
//            here is your logic for other notifications
//            that haven't been handled by Adapty
//             */
//        }
//    }
//}