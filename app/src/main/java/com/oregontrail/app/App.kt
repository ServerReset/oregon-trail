package com.oregontrail.app

import android.app.Application
import com.google.android.material.color.DynamicColors

/** Applies Material You dynamic colours to activities when the device supports it. */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        try {
            DynamicColors.applyToActivitiesIfAvailable(this)
        } catch (_: Throwable) {
            // Dynamic colour is optional; ignore failures.
        }
    }
}
