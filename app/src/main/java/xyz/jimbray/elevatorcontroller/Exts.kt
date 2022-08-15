package xyz.jimbray.elevatorcontroller

import android.util.Log


/**
 * String TAG 日志扩展
 */
fun String.log_d(tag: String = "jimbray") {
    if (BuildConfig.DEBUG) {
        Log.d(tag, this)
    }
}
