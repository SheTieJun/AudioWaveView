package com.shetj.waveview

import android.animation.ValueAnimator
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 重置ValueAnimator动画时长 防止部分手机修改动画时长导致动画不执行
 */
internal fun ValueAnimator?.resetDurationScale() {
    try {
        if (this != null ) {
            val setAnimationScale: Method =
                ValueAnimator::class.java.getMethod(
                    "setDurationScale",
                    Float::class.javaPrimitiveType
                )
            setAnimationScale.invoke(this, 1)
        }
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}