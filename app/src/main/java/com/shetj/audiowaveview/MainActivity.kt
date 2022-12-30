package com.shetj.audiowaveview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.lifecycleScope
import com.shetj.audiowaveview.databinding.ActivityMainBinding
import com.shetj.waveview.AudioWaveView.OnChangeListener
import com.shetj.waveview.covertToTimets
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
            binding.audioWaveView.cutSelect()
        }

        binding.closeEdit.setOnClickListener {
            binding.audioWaveView.closeEdit()
        }


        binding.audioWaveView.setListener(object : OnChangeListener {
            override fun onUpdateCurrentPosition(position: Long) {
                binding.time1.text = "中线："+position.covertToTimets()
            }

            override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {
                binding.time2.text = "开始："+startPosition.covertToTimets()
                binding.time3.text = "结束："+endPosition.covertToTimets()
            }

            override fun onCutAudio(startTime: Long, endTime: Long): Boolean {
                return true
            }
        })

        binding.play.setOnClickListener {
            binding.audioWaveView.startPlayAnim()
        }

        binding.pause.setOnClickListener {
            binding.audioWaveView.pausePlayAnim()
        }
    }
}