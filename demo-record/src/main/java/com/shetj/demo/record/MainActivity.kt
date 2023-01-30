package com.shetj.demo.record

import android.Manifest
import android.content.res.ColorStateList
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.shetj.demo.record.databinding.ActivityMainBinding
import com.shetj.demo.record.utils.AudioPlayer
import com.shetj.demo.record.utils.PlaySate
import com.shetj.demo.record.utils.PlaySate.Pause
import com.shetj.demo.record.utils.PlaySate.Playing
import com.shetj.demo.record.utils.PlaySate.Stop
import com.shetj.demo.record.utils.SimPlayerListener
import com.shetj.demo.record.utils.Util
import com.shetj.waveview.AudioWaveView.OnChangeListener
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.logI
import me.shetj.base.ktx.start
import me.shetj.base.ktx.startRequestPermission
import me.shetj.base.ktx.toJson
import me.shetj.base.ktx.withIO
import me.shetj.base.mvvm.BaseBindingActivity
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.app.Tim
import me.shetj.ffmpeg.FFmpegKit
import me.shetj.ffmpeg.FFmpegState.OnFinish
import me.shetj.ffmpeg.buildCutCommand
import me.shetj.ffmpeg.buildMergeCommand
import me.shetj.recorder.core.FileUtils
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.RecordState.PAUSED
import me.shetj.recorder.core.SimRecordListener

class MainActivity : BaseBindingActivity<ActivityMainBinding, RecordViewModel>() {

    private var needToStart: Boolean = false
    private val audioPlayer by lazy { AudioPlayer() }
    private val callback: SimRecordListener = object : SimRecordListener() {

        override fun onStart() {

            updateUIState(RecordState.RECORDING)
        }

        override fun onResume() {
            updateUIState(RecordState.RECORDING)
        }

        override fun onRecording(time: Long, volume: Int) {
            launch {
                val currentLevel = mViewModel.getVoiceLevel(volume)
                mViewBinding.waveview.addFrame(currentLevel, time)
                mViewModel.timeLiveData.value = (time)
            }
        }

        override fun onPause() {
            updateUIState(RecordState.PAUSED)
        }

        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
            updateUIState(RecordState.STOPPED)
            mViewModel.saveRecordToDB(file)
            mViewBinding.waveview.clearFrame()
            mViewModel.timeLiveData.postValue(0)
            mViewBinding.playTime.text = Util.formatSeconds4(0)
        }
    }

    private val recordTool by lazy { RecorderKit(context = this, callBack = callback) }

    private val playCallback = object : SimPlayerListener() {
        override fun onStart(duration: Int) {
            super.onStart(duration)
            mViewModel.difDuration = getRecordDuration() - duration
            mViewBinding.waveview.startPlayAnim()
        }

        override fun onResume() {
            super.onResume()
            mViewBinding.waveview.startPlayAnim()

        }

        override fun onStop() {
            super.onStop()
            mViewBinding.waveview.pausePlayAnim()
        }

        override fun onCompletion() {
            super.onCompletion()
            mViewBinding.playTime.text = Util.formatSeconds4(getRecordDuration())
            mViewBinding.waveview.pausePlayAnim()
        }

        override fun onProgress(current: Int, duration: Int) {
            super.onProgress(current, duration)
            mViewBinding.playTime.text =
                Util.formatSeconds4(current.toLong() + (current / duration.toFloat() * mViewModel.difDuration).toInt())
        }

        override fun onPause() {
            super.onPause()
            mViewBinding.waveview.pausePlayAnim()
        }
    }

    override fun initView() {
        super.initView()
        Tim.setLogAuto(true)
        //添加点击效果
        ArmsUtils.addScaleTouchEffect(
            mViewBinding.ivRecordPlay,
            mViewBinding.ivRecordState,
            mViewBinding.ivRecordHistory
        )
        mViewBinding.ivRecordPlay.setOnClickListener {
            if (needToStart) {
                mViewBinding.waveview.scrollToStart()
                needToStart = false
            }
            audioPlayer.playOrPause(recordTool.getSaveUrl(), playCallback)
        }

        mViewBinding.cancelCut.setOnClickListener {
            mViewBinding.waveview.closeEditModel()
        }

        mViewBinding.startCut.setOnClickListener {
            mViewBinding.waveview.startEditModel()
        }

        mViewBinding.cutAudio.setOnClickListener {
            launch {
                mViewBinding.waveview.cutSelect()
            }
        }

        mViewBinding.waveview.setListener(object : OnChangeListener {
            override fun onUpdateCurrentPosition(position: Long, duration: Long) {
                audioPlayer.setSeekToPlay(position.toInt())
                mViewBinding.playTime.text = Util.formatSeconds4(position)
                mViewModel.timeLiveData.postValue(duration)
            }

            override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {

            }

            override suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
                if (startTime == endTime) return true
                return withIO {
                    return@withIO cutAudio(startTime, endTime)
                }
            }

            override fun onUpdateScale(scale: Float) {

            }

            override fun onEditModelChange(isEditModel: Boolean) {
                mViewBinding.ivRecordState.isEnabled = !isEditModel
                mViewBinding.cancelCut.isVisible = isEditModel
                mViewBinding.cutAudio.isVisible = isEditModel
                mViewBinding.startCut.isVisible = !isEditModel
            }
        })

        mViewBinding.ivRecordHistory.setOnClickListener {
            if (!recordTool.hasRecord()) {
                mViewModel.newRecordCount.postValue(0)
                start<RecordHistoryActivity>()
            } else {
                recordTool.complete()
            }
        }


        mViewBinding.ivRecordState.setOnClickListener {
            startRequestPermission("startRequestPermission", Manifest.permission.RECORD_AUDIO) {
                if (it) {
                    recordTool.startOrPause()
                }
            }
        }

        mViewModel.timeLiveData.observe(this) {
            mViewBinding.tvRecordTime.text = Util.formatSeconds4(it)
        }

        mViewModel.newRecordCount.observe(this) {
            mViewBinding.ivRecordHistory.updateCount(it)
        }


        initPlayAudio()
    }

    private suspend fun cutAudio(startTime: Long, endTime: Long): Boolean {
        //剪掉左边，剪掉右边，合并
        //1. 获取左边的
        val leftAudio = recordTool.getAudioFileName("left")
        val leftCutCommand =
            buildCutCommand(recordTool.getSaveUrl(), leftAudio, 0.0, startTime.toDouble())
        leftCutCommand.toJson().logI("record")
        val leftFFmpegState = FFmpegKit.runCommand(leftCutCommand)
        if (leftFFmpegState != OnFinish) {
            FileUtils.deleteFile(leftAudio)
            return false
        }

        //2. 获取右边
        val rightAudio = recordTool.getAudioFileName("right")
        val rightCutCommand = buildCutCommand(
            recordTool.getSaveUrl(),
            rightAudio,
            endTime.toDouble(),
            getRecordDuration().toDouble()
        )
        rightCutCommand.toJson().logI("record")
        val rightFfmpegState = FFmpegKit.runCommand(rightCutCommand)
        if (rightFfmpegState != OnFinish) {
            FileUtils.deleteFile(leftAudio)
            FileUtils.deleteFile(rightAudio)
            return false
        }

        //3.合并
        val mergeAudio = recordTool.getAudioFileName("merge")
        val mergeCommand = buildMergeCommand(leftAudio, rightAudio, output = mergeAudio)
        mergeCommand.toJson().logI("record")
        val mergeState = FFmpegKit.runCommand(mergeCommand)

//        FileUtils.deleteFile(leftAudio)
//        FileUtils.deleteFile(rightAudio)
        if (mergeState == OnFinish) {
            recordTool.updateSaveFile(mergeAudio)
            val time = mViewBinding.waveview.getDuration()
            recordTool.setTime(time)
            mViewModel.timeLiveData.postValue(time)
            return true
        }
        return false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        recordTool.pause()
    }

    override fun initData() {
        super.initData()
    }

    override fun onDestroy() {
        super.onDestroy()
        recordTool.pause()
        audioPlayer.stopPlay()
    }

    override fun onStop() {
        super.onStop()
        audioPlayer.pause()
    }

    private fun initPlayAudio() {
        launch {
            audioPlayer.playState.collect {
                updateUIState(it)
            }
        }
    }

    private fun getRecordDuration() = mViewModel.timeLiveData.value ?: 0

    /**
     * 更新UI状态
     */
    fun updateUIState(state: RecordState) {
        mViewBinding.ivRecordHistory.updateState(state)
        mViewBinding.ivRecordState.updateState(state)
        val canPlay = state != RecordState.RECORDING && recordTool.hasRecord()
        if (canPlay) {
            mViewBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
            //计算时间差值
            mViewModel.difDuration =
                getRecordDuration() - Util.getAudioLength(recordTool.getSaveUrl())
        } else {
            mViewBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, me.shetj.base.R.color.blackHintText))
        }
        mViewBinding.ivRecordPlay.isEnabled = canPlay
        mViewBinding.playTime.isVisible = canPlay
        mViewBinding.startCut.isVisible = state == RecordState.PAUSED
        when (state) {
            RecordState.RECORDING -> {
                needToStart = true
                mViewBinding.waveview.setEnableScroll(false)
            }
            RecordState.PAUSED -> {
                mViewBinding.waveview.setEnableScroll(true)
            }
            RecordState.STOPPED -> {

            }
        }
    }


    private fun updateUIState(state: PlaySate) {
        mViewBinding.ivRecordState.isEnabled = state !is Playing
        when (state) {
            Pause -> {
                mViewBinding.ivRecordPlay.setImageResource(R.drawable.icon_play_audio)
                mViewBinding.waveview.setEnableScroll(true)
            }
            Playing -> {
                mViewBinding.ivRecordPlay.setImageResource(R.drawable.icon_puase_audio)
                mViewBinding.waveview.setEnableScroll(false)
            }
            Stop -> {
                mViewBinding.ivRecordPlay.setImageResource(R.drawable.icon_play_audio)
                mViewBinding.waveview.setEnableScroll(true)
            }
        }
    }
}