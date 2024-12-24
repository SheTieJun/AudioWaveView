package com.shetj.demo.record

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.ColorStateList
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.shetj.demo.TestWaveViewActivity
import com.shetj.demo.record.databinding.ActivityMainBinding
import com.shetj.demo.record.utils.AudioPlayer
import com.shetj.demo.record.utils.PlaySate
import com.shetj.demo.record.utils.PlaySate.Pause
import com.shetj.demo.record.utils.PlaySate.Playing
import com.shetj.demo.record.utils.PlaySate.Stop
import com.shetj.demo.record.utils.SimPlayerListener
import com.shetj.demo.record.utils.Util
import com.shetj.waveview.AudioWaveView.OnChangeListener
import com.shetj.waveview.covertToTimets
import me.shetj.base.ktx.isTrue
import me.shetj.base.ktx.launch
import me.shetj.base.ktx.launchActivity
import me.shetj.base.ktx.logI
import me.shetj.base.ktx.start
import me.shetj.base.ktx.startRequestPermission
import me.shetj.base.ktx.toJson
import me.shetj.base.ktx.withIO
import me.shetj.base.mvvm.viewbind.BaseBindingActivity
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.app.Tim
import me.shetj.ffmpeg.FFmpegKit
import me.shetj.ffmpeg.FFmpegState.OnFinish
import me.shetj.ffmpeg.buildCutCommand
import me.shetj.ffmpeg.buildMergeCommand
import me.shetj.recorder.core.FileUtils
import me.shetj.recorder.core.RecordState
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
                mBinding.waveview.addFrame(currentLevel, time)
                mViewModel.timeLiveData.value = (time)
            }
        }

        override fun onPause() {
            updateUIState(RecordState.PAUSED)
        }

        override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
            updateUIState(RecordState.STOPPED)
            mViewModel.saveRecordToDB(file)
            mBinding.waveview.clearFrame()
            mViewModel.timeLiveData.postValue(0)
            mBinding.playTime.text = Util.formatSeconds4(0)
            mBinding.recordStateMsg.text = ""
        }
    }

    private val recorder by lazy { RecorderKit(context = this, callBack = callback) }

    private val playCallback = object : SimPlayerListener() {
        override fun onStart(duration: Int) {
            super.onStart(duration)
            mBinding.waveview.startPlayAnim()
        }

        override fun onResume() {
            super.onResume()
            mBinding.waveview.startPlayAnim()

        }

        override fun onPause() {
            super.onPause()
            mBinding.waveview.pausePlayAnim()
        }
    }

    override fun initBaseView() {
        super.initBaseView()
        Tim.setLogAuto(true)
        //添加点击效果
        ArmsUtils.addScaleTouchEffect(
            mBinding.ivRecordPlay,
            mBinding.ivRecordState,
            mBinding.ivRecordHistory
        )
        mBinding.ivRecordPlay.setOnClickListener {
            if (needToStart) {
                mBinding.waveview.scrollToStart()
                needToStart = false
            }
            audioPlayer.playOrPause(recorder.getSaveUrl(), playCallback)
        }

        mBinding.cancelCut.setOnClickListener {
            mBinding.waveview.closeEditModel()
        }

        mBinding.startCut.setOnClickListener {
            mBinding.waveview.startEditModel()
        }

        mBinding.cutAudio.setOnClickListener {
            launch {
                mBinding.waveview.cutSelect()
            }
        }

        mBinding.waveview.setListener(object : OnChangeListener {
            override fun onUpdateCurrentPosition(position: Long, duration: Long) {
                audioPlayer.setSeekToPlay(position.toInt())
                mBinding.playTime.text = Util.formatSeconds4(position)
                mViewModel.timeLiveData.postValue(duration)
                mViewModel.recordOverWriter.postValue(position != duration)
            }

            override suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
                if (startTime == endTime) return true
                if (startTime == 0L && endTime == mBinding.waveview.getDuration()) {
                    recorder.updateSaveFile(recorder.getAudioFileName("merge"))
                    return true
                }
                return withIO {
                    return@withIO cutAudio(startTime, endTime)
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onUpdateCutPosition(startPosition: Long, endPosition: Long) {
                super.onUpdateCutPosition(startPosition, endPosition)
                mBinding.startCutTime.text = "开始：" + startPosition.covertToTimets()
                mBinding.endCutTime.text = "结束：" + endPosition.covertToTimets()
            }

            override fun onEditModelChange(isEditModel: Boolean) {
                mBinding.ivRecordState.isEnabled = !isEditModel
                mBinding.cancelCut.isVisible = isEditModel
                mBinding.cutAudio.isVisible = isEditModel
                mBinding.startCut.isVisible = !isEditModel
            }

            override fun onCutFinish() {
                super.onCutFinish()
                val duration = mBinding.waveview.getDuration()
                recorder.setTime(duration)
                mViewModel.timeLiveData.postValue(duration)
            }

            override fun onUpdateScale(scale: Float) {
                super.onUpdateScale(scale)
                mBinding.viewScale.text = "缩放级别：$scale"
            }
        })

        mBinding.ivRecordHistory.setOnClickListener {
            if (!recorder.hasRecord()) {
                mViewModel.newRecordCount.postValue(0)
                launchActivity<RecordHistoryActivity>{}
            } else {
                recorder.complete()
            }
        }


        mBinding.ivRecordState.setOnClickListener {
            startRequestPermission(  Manifest.permission.RECORD_AUDIO) {
                if (it) {
                    launch {
                        if (mViewModel.recordOverWriter.isTrue()) {
                            mBinding.waveview.startOverwrite()
                        }
                        recorder.startOrPause()
                    }
                }
            }
        }

        mViewModel.timeLiveData.observe(this) {
            mBinding.tvRecordTime.text = Util.formatSeconds4(it)
        }

        mViewModel.newRecordCount.observe(this) {
            mBinding.ivRecordHistory.updateCount(it)
        }

        mViewModel.recordOverWriter.observe(this) {
            if (recorder.canOverWriter()) {
                if (it) {
                    mBinding.recordStateMsg.text = "覆盖录制"
                } else {
                    mBinding.recordStateMsg.text = "继续录制"
                }
            }
        }

        initPlayAudio()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_github_view, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.github ->{
                val url = "https://github.com/SheTieJun/AudioWaveView"
                val i = Intent(Intent.ACTION_VIEW)
                i.data = Uri.parse(url)
                startActivity(i)
            }
            R.id.waveview ->{
                start<TestWaveViewActivity>()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private suspend fun cutAudio(startTime: Long, endTime: Long): Boolean {
        //剪掉左边，剪掉右边，合并
        //1. 获取左边的
        val leftAudio = recorder.getAudioFileName("left")
        val leftCutCommand =
            buildCutCommand(recorder.getSaveUrl(), leftAudio, 0.0, startTime.toDouble())
        leftCutCommand.toJson().logI("record")
        val leftFFmpegState = FFmpegKit.runCommand(leftCutCommand)
        if (leftFFmpegState != OnFinish) {
            FileUtils.deleteFile(leftAudio)
            return false
        }

        //2. 获取右边
        val rightAudio = recorder.getAudioFileName("right")
        val rightCutCommand = buildCutCommand(
            recorder.getSaveUrl(),
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
        val mergeAudio = recorder.getAudioFileName("merge")
        val mergeCommand = buildMergeCommand(leftAudio, rightAudio, output = mergeAudio)
        mergeCommand.toJson().logI("record")
        val mergeState = FFmpegKit.runCommand(mergeCommand)
        //4.删除临时的音频文件
        FileUtils.deleteFile(leftAudio)
        FileUtils.deleteFile(rightAudio)
        if (mergeState == OnFinish) {
            //如果要实现重录取消等功能，就不可以删除这些文件
            FileUtils.deleteFile(recorder.getSaveUrl())
            recorder.updateSaveFile(mergeAudio)
            return true
        }
        return false
    }


    override fun onBack() {
        super.onBack()
        recorder.pause()
    }


    override fun onDestroy() {
        super.onDestroy()
        recorder.pause()
        audioPlayer.stopPlay()
    }

    override fun onStop() {
        super.onStop()
        audioPlayer.pause()
    }

    override fun setUpClicks() {

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
        mBinding.ivRecordHistory.updateState(state)
        mBinding.ivRecordState.updateState(state)
        val canPlay = state != RecordState.RECORDING && recorder.hasRecord()
        if (canPlay) {
            mBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
            //计算时间差值
            mViewModel.difDuration =
                getRecordDuration() - Util.getAudioLength(recorder.getSaveUrl())
        } else {
            mBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, me.shetj.base.R.color.blackHintText))
        }
        mBinding.ivRecordPlay.isEnabled = canPlay
        mBinding.playTime.isVisible = canPlay
        mBinding.startCut.isVisible = state == RecordState.PAUSED
        when (state) {
            RecordState.RECORDING -> {
                mBinding.recordStateMsg.text = "暂停录制"
                needToStart = true
                mBinding.waveview.setEnableScroll(false)
            }
            RecordState.PAUSED -> {
                mBinding.waveview.setEnableScroll(true)
                mBinding.recordStateMsg.text = "继续录制"
            }
            RecordState.STOPPED -> {

            }
        }
    }


    private fun updateUIState(state: PlaySate) {
        mBinding.ivRecordState.isEnabled = state !is Playing
        when (state) {
            Pause -> {
                mBinding.ivRecordPlay.setImageResource(R.drawable.icon_play_audio)
                mBinding.waveview.setEnableScroll(true)
            }
            Playing -> {
                mBinding.ivRecordPlay.setImageResource(R.drawable.icon_puase_audio)
                mBinding.waveview.setEnableScroll(false)
            }
            Stop -> {
                mBinding.ivRecordPlay.setImageResource(R.drawable.icon_play_audio)
                mBinding.waveview.setEnableScroll(true)
            }
        }
    }
}