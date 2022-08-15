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

//fun ByteArray.toHexString(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
fun ByteArray.toHexString(): String {
    val hexChars = "0123456789abcdef".toCharArray()
    val hex = CharArray(2 * this.size)
    this.forEachIndexed { i, byte ->
        val unsigned = 0xff and byte.toInt()
        hex[2 * i] = hexChars[unsigned / 16]
        hex[2 * i + 1] = hexChars[unsigned % 16]
    }

    return hex.joinToString("")
}