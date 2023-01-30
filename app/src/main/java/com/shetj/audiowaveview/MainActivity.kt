package com.shetj.audiowaveview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.shetj.audiowaveview.databinding.ActivityMainBinding
import com.shetj.waveview.AudioWaveView.OnChangeListener
import com.shetj.waveview.FrameArray
import com.shetj.waveview.covertToTimets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.showToast

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var duration = 0L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        binding.startAdd.setOnClickListener {
            lifecycleScope.launch {
                repeat(25) {
                    delay(40)
                    duration += 40
                    binding.audioWaveView.addFrame((1..9).random().toFloat(), duration)
                }
            }
        }

        binding.end.setOnClickListener {
            binding.audioWaveView.scrollToEnd()
        }

        binding.openEdit.setOnClickListener {
//            binding.audioWaveView.startEditModel(duration - 2000, duration - 1000)
            binding.audioWaveView.startEditModel()
        }

        binding.cutEdit.setOnClickListener {
            launch {
                binding.audioWaveView.cutSelect()
            }
        }

        binding.closeEdit.setOnClickListener {
            binding.audioWaveView.closeEditModel()
        }


        binding.audioWaveView.setListener(object : OnChangeListener {
            override fun onUpdateCurrentPosition(position: Long,duration:Long) {
                binding.time1.text = "中线：" + position.covertToTimets()
            }

            override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {
                binding.time2.text = "开始：" + startPosition.covertToTimets()
                binding.time3.text = "结束：" + endPosition.covertToTimets()
            }

            override suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
                return true
            }

            override fun onUpdateScale(scale: Float) {
                binding.viewScale.text = "缩放级别：$scale"
            }

            override fun onEditModelChange(isEditModel: Boolean) {
                //编辑模式变更通知，进行UI变化
            }
        })

        binding.play.setOnClickListener {
            binding.audioWaveView.startPlayAnim()
        }

        binding.pause.setOnClickListener {
            binding.audioWaveView.pausePlayAnim()
        }
        binding.replace.setOnClickListener {
            if (!binding.audioWaveView.isEditModel()) {
                "请先开始编辑模式，将会替换选中的部分".showToast()
                return@setOnClickListener
            }
            val newFrameArray = FrameArray()
            var newDuration = 0L
            launch {
                repeat(25) {
                    newDuration += 40
                    newFrameArray.add((1..9).random().toFloat())
                }
            }
            val cutStartTime = binding.audioWaveView.getCutStartTime()
            val cutEndTime = binding.audioWaveView.getCutEndTime()
            binding.audioWaveView.replaceFrames(cutStartTime, cutEndTime,newFrameArray,newDuration,true)
        }
        binding.clean.setOnClickListener {
            binding.audioWaveView.clearFrame()
        }
        binding.overwrite.setOnClickListener {
            launch {
                binding.audioWaveView.startOverwrite()
                duration = binding.audioWaveView.getDuration()
                binding.startAdd.performClick()
            }
        }
    }
}