package com.adapty.internal.data.cache

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class PushTokenRetriever {

    @JvmSynthetic
    fun getTokenOrNull(): String? = try {
        val firebaseInstance =
            Class.forName("com.google.firebase.iid.FirebaseInstanceId").getMethod("getInstance")
                .invoke(null)
        firebaseInstance.javaClass.getMethod("getToken").invoke(firebaseInstance) as? String
    } catch (e: Exception) {
        null
    }
}