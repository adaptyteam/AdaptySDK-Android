package com.adapty.internal.utils

import androidx.annotation.RestrictTo

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal interface NetworkLogger {

    fun logRequest(msg: () -> String)

    fun logResponse(msg: () -> String)

    fun logError(msg: () -> String) {
        Logger.logError(msg)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class DefaultNetworkLogger : NetworkLogger {

    override fun logRequest(msg: () -> String) {
        Logger.logRequest(msg)
    }

    override fun logResponse(msg: () -> String) {
        Logger.logResponse(msg)
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal class KinesisNetworkLogger : NetworkLogger {

    override fun logRequest(msg: () -> String) {
        Logger.logAnalytics(msg)
    }

    override fun logResponse(msg: () -> String) {
        Logger.logAnalytics(msg)
    }
}