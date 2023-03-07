package com.shetj.waveview

import android.graphics.Bitmap
import android.graphics.Bitmap.Config
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable


fun Long.covertToTimets(): String {
    val ts = this % 1000
    val seconds = this / 1000
    return (asTwoDigit(seconds.toInt() / 60) + ":"
            + asTwoDigit(seconds.toInt() % 60)) + "." + asTwoDigit(ts.toInt() / 10)
}


internal fun Long.covertToTime(): String {
    this.let { second ->
        val formatTime: String
        val h: Long = second / 3600
        val m: Long = second % 3600 / 60
        val s: Long = second % 3600 % 60
        formatTime = if (h == 0L) {
            asTwoDigit(m) + ":" + asTwoDigit(s)
        } else {
            asTwoDigit(h) + ":" + asTwoDigit(m) + ":" + asTwoDigit(s)
        }
        return formatTime
    }
}

internal fun asTwoDigit(digit: Long): String {
    var value = ""
    if (digit < 10) {
        value = "0"
    }
    value += digit.toString()
    return value
}

internal fun Int.covertToTime(): String {
    this.let { second ->
        val formatTime: String
        val h: Int = second / 3600
        val m: Int = second % 3600 / 60
        val s: Int = second % 3600 % 60
        formatTime = if (h == 0) {
            asTwoDigit(m) + ":" + asTwoDigit(s)
        } else {
            asTwoDigit(h) + ":" + asTwoDigit(m) + ":" + asTwoDigit(s)
        }
        return formatTime
    }
}


internal fun asTwoDigit(digit: Int): String {
    var value = ""
    if (digit < 10) {
        value = "0"
    }
    value += digit.toString()
    return value
}


internal fun Drawable.toBitmap(
    width: Int = intrinsicWidth,
    height: Int = intrinsicHeight,
    config: Config? = null
): Bitmap {
    if (this is BitmapDrawable) {
        if (config == null || bitmap.config == config) {
            // Fast-path to return original. Bitmap.createScaledBitmap will do this check, but it
            // involves allocation and two jumps into native code so we perform the check ourselves.
            if (width == bitmap.width && height == bitmap.height) {
                return bitmap
            }
            return Bitmap.createScaledBitmap(bitmap, width, height, true)
        }
    }

    val (oldLeft, oldTop, oldRight, oldBottom) = bounds

    val bitmap = Bitmap.createBitmap(width, height, config ?: Config.ARGB_8888)
    setBounds(0, 0, width, height)
    draw(Canvas(bitmap))

    setBounds(oldLeft, oldTop, oldRight, oldBottom)
    return bitmap
}


private operator fun Rect.component1(): Int {
    return left
}

private operator fun Rect.component2(): Int {
    return top
}

private operator fun Rect.component3(): Int {
    return right
}

private operator fun Rect.component4(): Int {
    return bottom
}
