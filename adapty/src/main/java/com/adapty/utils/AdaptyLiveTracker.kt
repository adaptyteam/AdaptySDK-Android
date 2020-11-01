package com.adapty.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.os.Handler

internal class AdaptyLiveTracker(private val kinesisManager: KinesisManager) :
    Application.ActivityLifecycleCallbacks {

    private val handler = Handler()
    private val TRACKING_INTERVAL = (60 * 1000).toLong()
    private val LIVE_EVENT_NAME = "live"

    private var activityReferences = 0
    private var isActivityChangingConfigurations = false
    private var readyToTrack = false

    fun start() {
        readyToTrack = true
        if (activityReferences > 0) {
            trackLive()
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit

    override fun onActivityStarted(activity: Activity) {
        if (++activityReferences == 1 && !isActivityChangingConfigurations && readyToTrack) {
            trackLive()
        }
    }

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) {
        isActivityChangingConfigurations = activity.isChangingConfigurations
        if (--activityReferences == 0 && !isActivityChangingConfigurations) {
            handler.removeCallbacksAndMessages(null)
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) = Unit

    private fun trackLive() {
        handler.removeCallbacksAndMessages(null)
        kinesisManager.trackEvent(LIVE_EVENT_NAME)
        handler.postDelayed({
            trackLive()
        }, TRACKING_INTERVAL)
    }
}