package com.adapty.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

internal open class DefaultLogger {
    open fun logVerbose(log: String) = Unit
    open fun logError(log: String) = Unit

    protected fun getCurrentTime() : String {
        val time = Calendar.getInstance().time
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault())
        return format.format(time)
    }
}

internal open class ErrorLogger: DefaultLogger() {
    override fun logError(log: String) {
        Log.e("[Adapty ${com.adapty.BuildConfig.VERSION_NAME} ${com.adapty.BuildConfig.VERSION_CODE}]", "${getCurrentTime()}  - $log - ERROR")
    }
}

internal class VerboseLogger: ErrorLogger() {
    override fun logVerbose(log: String) {
        Log.i("[Adapty ${com.adapty.BuildConfig.VERSION_NAME} ${com.adapty.BuildConfig.VERSION_CODE}]", "${getCurrentTime()}  - $log - INFO")
    }
}