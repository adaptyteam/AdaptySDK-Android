package com.adapty.utils

public fun interface AdaptyLogHandler {
    public fun onLogMessageReceived(level: AdaptyLogLevel, message: String)
}