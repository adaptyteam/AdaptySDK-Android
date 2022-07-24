package com.adapty.push

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Bitmap
import android.media.RingtoneManager
import android.os.Build
import kotlin.random.Random

public abstract class AdaptyPushHandler(protected val context: Context) {

    private companion object {
        private const val DEFAULT_CHANNEL_ID = "Offers"
    }

    private val notificationChannelCreator: NotificationChannelCreator by lazy {
        NotificationChannelCreator(context)
    }

    public abstract val clickAction: String

    public abstract val smallIconResId: Int

    public open val largeIcon: Bitmap? = null

    public open val channelId: String = DEFAULT_CHANNEL_ID

    public open val customizedNotificationBuilder: Notification.Builder? = null

    public fun handleNotification(data: Map<String, String>): Boolean {
        if (data["source"] == "adapty") {
            notificationChannelCreator.createNotificationChannelIfDontExist(channelId)

            val nm =
                context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(Random.nextInt(10000), buildNotification(data))
            return true
        }
        return false
    }

    private fun getNotificationBuilder(context: Context, channelId: String) =
        if (Build.VERSION.SDK_INT >= 26) {
            Notification.Builder(context, channelId)
        } else {
            Notification.Builder(context)
        }

    private fun buildNotification(data: Map<String, String>): Notification {
        val builder = customizedNotificationBuilder ?: getNotificationBuilder(context, channelId)
            .setProperty(largeIcon != null) {
                setLargeIcon(largeIcon)
            }
            .setAutoCancel(true)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        return builder
            .setContentIntent(createPendingIntent(data, clickAction))
            .setContentTitle(data["title"])
            .setContentText(data["body"])
            .setSmallIcon(smallIconResId)
            .build()
    }

    private fun createPendingIntent(data: Map<String, String>, action: String): PendingIntent {
        val intent = Intent(action)
            .putExtra("source", data["source"])
            .putExtra("profile_id", data["profile_id"])
            .putExtra("promo_delivery_id", data["promo_delivery_id"])
        val flags = if (Build.VERSION.SDK_INT >= 23) {
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_ONE_SHOT
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            flags
        )
    }

    private fun Notification.Builder.setProperty(
        needToSet: Boolean,
        func: Notification.Builder.() -> Notification.Builder
    ) = if (needToSet) this.func() else this
}