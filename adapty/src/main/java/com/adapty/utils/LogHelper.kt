package com.adapty.utils

import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class LogHelper {
    companion object {
        fun logVerbose(log: String) {
            Log.i("[Adapty ${com.adapty.BuildConfig.VERSION_NAME} $ADAPTY_SDK_VERSION_INT]", "${getCurrentTime()}  - $log - INFO")
        }

        fun logError(log: String) {
            Log.e("[Adapty ${com.adapty.BuildConfig.VERSION_NAME} $ADAPTY_SDK_VERSION_INT]", "${getCurrentTime()}  - $log - ERROR")
        }

        private fun getCurrentTime() : String {
            val time = Calendar.getInstance().time
            val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ", Locale.getDefault())
            return format.format(time)
        }
    }
}