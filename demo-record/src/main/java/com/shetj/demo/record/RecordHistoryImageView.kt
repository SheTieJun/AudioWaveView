package com.shetj.demo.record

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterView
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.RecordState.*

@SuppressLint("UnsafeOptInUsageError")
class RecordHistoryImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ImageFilterView(context, attrs) {

    private var count = 0

    private val lazyDrawable = lazy {
        BadgeDrawable.create(context).apply {
            backgroundColor = Color.RED
            badgeGravity = BadgeDrawable.TOP_END
            maxCharacterCount = 3
            horizontalOffset = 25
            verticalOffset = 25
        }
    }

    private val badgeDrawable by lazyDrawable

    fun updateState(state: RecordState) {
        badgeDrawable.isVisible = state == STOPPED && count > 0
        when (state) {
            RECORDING -> {
                setImageResource(R.drawable.icon_record_save)
            }
            PAUSED -> {
                setImageResource(R.drawable.icon_record_save)
            }
            STOPPED -> {
                BadgeUtils.attachBadgeDrawable(badgeDrawable, this)
                setImageResource(R.drawable.icon_record_history)
            }
        }
    }

    /**
     * 更新未读
     */
    fun updateCount(it: Int) {
        count = it
        badgeDrawable.isVisible = it > 0
        badgeDrawable.number = it
    }
}