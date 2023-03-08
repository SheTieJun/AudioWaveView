package com.shetj.demo

import android.graphics.Color
import android.os.Bundle
import android.widget.RadioButton
import android.widget.SeekBar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.shetj.demo.record.R
import com.shetj.demo.record.databinding.ActivityTestWaveViewBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shetj.base.base.AbBindingActivity
import me.shetj.base.ktx.dp2px
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.logI

class TestWaveViewActivity : AbBindingActivity<ActivityTestWaveViewBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }
    private var duration = 0L

    override fun initView() {

        mViewBinding.audioWaveView.setCutSelectPaintColor(Color.parseColor("#4cFF0000"))

        mViewBinding.waveWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (40f.dp2px)
                "waveWidth:$fl".logI()
                mViewBinding.audioWaveView.setWaveWidth(fl)

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mViewBinding.waveCornerRadius.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (40f.dp2px)
                "waveCornerRadius:$fl".logI()
                mViewBinding.audioWaveView.setWaveCornerRadius(fl)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mViewBinding.waveSpace.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (20f.dp2px)
                "waveSpace:$fl".logI()
                mViewBinding.audioWaveView.setWaveSpace(fl)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mViewBinding.waveTimeSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ("waveTimeSize:$progress").logI()
                mViewBinding.audioWaveView.setTimeSize(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mViewBinding.waveScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "waveScale:${(progress*0.1f)}".logI()
                mViewBinding.audioWaveView.setWaveScale(progress*0.1f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mViewBinding.centerLineColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mViewBinding.centerLineColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mViewBinding.centerLineColorRadioGroup.indexOfChild(radioButton)
            mViewBinding.audioWaveView.setCenterLineColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.black)
                1 -> ContextCompat.getColor(this, R.color.purple_700)
                else -> ContextCompat.getColor(this, R.color.teal_700)
            })
        }

        mViewBinding.waveLeftColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mViewBinding.waveLeftColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mViewBinding.waveLeftColorRadioGroup.indexOfChild(radioButton)
            mViewBinding.audioWaveView.setLeftPaintColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.pink)
                1 -> ContextCompat.getColor(this, R.color.yellow)
                else -> ContextCompat.getColor(this, R.color.black)
            })
        }

        mViewBinding.waveRightColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mViewBinding.waveRightColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mViewBinding.waveRightColorRadioGroup.indexOfChild(radioButton)
            mViewBinding.audioWaveView.setRightPaintColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.red)
                1 -> ContextCompat.getColor(this, R.color.blue)
                else -> ContextCompat.getColor(this, R.color.green)
            })
        }

        mViewBinding.centerline.setOnClickListener {
            mViewBinding.audioWaveView.isShowCenterLine = !mViewBinding.audioWaveView.isShowCenterLine
        }
        mViewBinding.topbottomline.setOnClickListener {
            mViewBinding.audioWaveView.isShowTopBottomLine = !mViewBinding.audioWaveView.isShowTopBottomLine
        }

        mViewBinding.startAdd.setOnClickListener {
            lifecycleScope.launch {
                repeat(25) {
                    delay(40)
                    duration += 40
                    mViewBinding.audioWaveView.addFrame((1..9).random().toFloat(), duration)
                }
            }
        }

        mViewBinding.end.setOnClickListener {
            mViewBinding.audioWaveView.scrollToEnd()
        }

        mViewBinding.openEdit.setOnClickListener {
            mViewBinding.audioWaveView.startEditModel()
        }

        mViewBinding.cutEdit.setOnClickListener {
            launch {
                mViewBinding.audioWaveView.cutSelect()
            }
        }

        mViewBinding.closeEdit.setOnClickListener {
            mViewBinding.audioWaveView.closeEditModel()
        }

        mViewBinding.play.setOnClickListener {
            mViewBinding.audioWaveView.startPlayAnim()
        }

        mViewBinding.pause.setOnClickListener {
            mViewBinding.audioWaveView.pausePlayAnim()
        }
        mViewBinding.clean.setOnClickListener {
            mViewBinding.audioWaveView.clearFrame()
        }
    }
}