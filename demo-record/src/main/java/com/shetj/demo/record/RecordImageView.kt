package com.shetj.demo.record

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.utils.widget.ImageFilterView
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.RecordState.*


class RecordImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : ImageFilterView(context, attrs) {


    fun updateState(state: RecordState){
        when(state){
            RECORDING ->{
                setImageResource(R.mipmap.icon_record_pause_2)
            }
            PAUSED ->{
                setImageResource(R.mipmap.icon_start_record)
            }
            STOPPED->{
                setImageResource(R.mipmap.icon_start_record)
            }
        }
    }
}