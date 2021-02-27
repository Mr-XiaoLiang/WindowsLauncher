package com.lollipop.windowslauncher

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.lollipop.windowslauncher.theme.LColor
import com.lollipop.windowslauncher.utils.LSettings

/**
 * @author lollipop
 * @date 1/30/21 21:00
 */
class LApplication: Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private const val NIGHT_UNKNOWN = 0
    }

    private var topActivity: Activity? = null

    private var lastNightMode = NIGHT_UNKNOWN

    override fun onCreate() {
        super.onCreate()
        lastNightMode = LSettings.getNightMode(this)
        LSettings.notifyColorInfo(this)
        registerActivityLifecycleCallbacks(this)
    }

    private fun checkNightMode() {
        val nightMode = LSettings.getNightMode(this)
        if (lastNightMode != nightMode) {
            lastNightMode = nightMode
            LSettings.notifyColorInfo(this)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        if (topActivity == null) {
            checkNightMode()
        }
        topActivity = activity
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
        if (topActivity == activity) {
            topActivity = null
        }
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

}