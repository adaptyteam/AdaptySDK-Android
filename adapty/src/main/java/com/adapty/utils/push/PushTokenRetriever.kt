package com.adapty.utils.push

internal class PushTokenRetriever {

    internal fun getTokenOrNull(): String? = try {
        val firebaseInstance =
            Class.forName("com.google.firebase.iid.FirebaseInstanceId").getMethod("getInstance")
                .invoke(null)
        firebaseInstance.javaClass.getMethod("getToken").invoke(firebaseInstance) as? String
    } catch (e: Exception) {
        null
    }
}