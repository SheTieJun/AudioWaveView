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
import me.shetj.base.base.AbBaseBindingActivity
import me.shetj.base.ktx.dp2px
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.logI
import me.shetj.base.ktx.setAppearance

class TestWaveViewActivity : AbBaseBindingActivity<ActivityTestWaveViewBinding>() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAppearance(true)
    }

    override fun setUpClicks() {
        mBinding.centerline.setOnClickListener {
            mBinding.audioWaveView.isShowCenterLine = !mBinding.audioWaveView.isShowCenterLine
        }
        mBinding.topbottomline.setOnClickListener {
            mBinding.audioWaveView.isShowTopBottomLine = !mBinding.audioWaveView.isShowTopBottomLine
        }

        mBinding.startAdd.setOnClickListener {
            lifecycleScope.launch {
                repeat(25) {
                    delay(40)
                    duration += 40
                    mBinding.audioWaveView.addFrame((1..9).random().toFloat(), duration)
                }
            }
        }

        mBinding.end.setOnClickListener {
            mBinding.audioWaveView.scrollToEnd()
        }

        mBinding.openEdit.setOnClickListener {
            mBinding.audioWaveView.startEditModel()
        }

        mBinding.cutEdit.setOnClickListener {
            launch {
                mBinding.audioWaveView.cutSelect()
            }
        }

        mBinding.closeEdit.setOnClickListener {
            mBinding.audioWaveView.closeEditModel()
        }

        mBinding.play.setOnClickListener {
            mBinding.audioWaveView.startPlayAnim()
        }

        mBinding.pause.setOnClickListener {
            mBinding.audioWaveView.pausePlayAnim()
        }
        mBinding.clean.setOnClickListener {
            mBinding.audioWaveView.clearFrame()
        }
    }

    private var duration = 0L

    override fun initBaseView() {
        super.initBaseView()
        mBinding.audioWaveView.setCutSelectPaintColor(Color.parseColor("#4cFF0000"))

        mBinding.waveWidth.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (40f.dp2px)
                "waveWidth:$fl".logI()
                mBinding.audioWaveView.setWaveWidth(fl)

            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mBinding.waveCornerRadius.setOnSeekBarChangeListener(object :
            SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (40f.dp2px)
                "waveCornerRadius:$fl".logI()
                mBinding.audioWaveView.setWaveCornerRadius(fl)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mBinding.waveSpace.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val fl = progress / 100F * (20f.dp2px)
                "waveSpace:$fl".logI()
                mBinding.audioWaveView.setWaveSpace(fl)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mBinding.waveTimeSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                ("waveTimeSize:$progress").logI()
                mBinding.audioWaveView.setTimeSize(progress.toFloat())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mBinding.waveScale.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                "waveScale:${(progress*0.1f)}".logI()
                mBinding.audioWaveView.setWaveScale(progress*0.1f)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}

            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        mBinding.centerLineColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mBinding.centerLineColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mBinding.centerLineColorRadioGroup.indexOfChild(radioButton)
            mBinding.audioWaveView.setCenterLineColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.black)
                1 -> ContextCompat.getColor(this, R.color.purple_700)
                else -> ContextCompat.getColor(this, R.color.teal_700)
            })
        }

        mBinding.waveLeftColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mBinding.waveLeftColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mBinding.waveLeftColorRadioGroup.indexOfChild(radioButton)
            mBinding.audioWaveView.setLeftPaintColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.pink)
                1 -> ContextCompat.getColor(this, R.color.yellow)
                else -> ContextCompat.getColor(this, R.color.black)
            })
        }

        mBinding.waveRightColorRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            val radioButton = mBinding.waveRightColorRadioGroup.findViewById(checkedId) as RadioButton
            val index = mBinding.waveRightColorRadioGroup.indexOfChild(radioButton)
            mBinding.audioWaveView.setRightPaintColor(when (index) {
                0 -> ContextCompat.getColor(this, R.color.red)
                1 -> ContextCompat.getColor(this, R.color.blue)
                else -> ContextCompat.getColor(this, R.color.green)
            })
        }


    }
}