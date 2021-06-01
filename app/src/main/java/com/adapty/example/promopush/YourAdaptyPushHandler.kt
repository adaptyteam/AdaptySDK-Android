package com.adapty.example.promopush

import android.content.Context
import com.adapty.example.R
import com.adapty.push.AdaptyPushHandler

class YourAdaptyPushHandler(context: Context) : AdaptyPushHandler(context) {

    override val clickAction = "YOUR_NOTIFICATION_CLICK_ACTION"

    override val smallIconResId = R.drawable.ic_notification_small
}