package br.edu.ifpe.todo

import android.app.Activity
import android.content.Context

/**
 * Holds the application context and the currently resumed activity.
 * Used by platform code that is initialized outside the Compose tree
 * (notifications, database driver).
 */
object AndroidAppContext {
    lateinit var context: Context
        private set
    var activity: Activity? = null
        private set

    fun attach(appContext: Context) {
        context = appContext
    }

    fun registerActivity(activity: Activity) {
        this.activity = activity
    }

    fun unregisterActivity(activity: Activity) {
        if (this.activity === activity) this.activity = null
    }
}