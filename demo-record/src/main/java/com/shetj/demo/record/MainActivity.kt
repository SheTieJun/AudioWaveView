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
            mViewBinding.recordStateMsg.text = ""
        }
    }

    private val recorder by lazy { RecorderKit(context = this, callBack = callback) }

    private val playCallback = object : SimPlayerListener() {
        override fun onStart(duration: Int) {
            super.onStart(duration)
            mViewBinding.waveview.startPlayAnim()
        }

        override fun onResume() {
            super.onResume()
            mViewBinding.waveview.startPlayAnim()

        }

        override fun onPause() {
            super.onPause()
            mViewBinding.waveview.pausePlayAnim()
        }
    }

    override fun initView() {
        super.initView()
        Tim.setLogAuto(true)
        //??????????????????
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
            audioPlayer.playOrPause(recorder.getSaveUrl(), playCallback)
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
                mViewModel.recordOverWriter.postValue(position != duration)
            }

            override suspend fun onCutAudio(startTime: Long, endTime: Long): Boolean {
                if (startTime == endTime) return true
                if (startTime == 0L && endTime == mViewBinding.waveview.getDuration()) {
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
                mViewBinding.startCutTime.text = "?????????" + startPosition.covertToTimets()
                mViewBinding.endCutTime.text = "?????????" + endPosition.covertToTimets()
            }

            override fun onEditModelChange(isEditModel: Boolean) {
                mViewBinding.ivRecordState.isEnabled = !isEditModel
                mViewBinding.cancelCut.isVisible = isEditModel
                mViewBinding.cutAudio.isVisible = isEditModel
                mViewBinding.startCut.isVisible = !isEditModel
            }

            override fun onCutFinish() {
                super.onCutFinish()
                val duration = mViewBinding.waveview.getDuration()
                recorder.setTime(duration)
                mViewModel.timeLiveData.postValue(duration)
            }

            override fun onUpdateScale(scale: Float) {
                super.onUpdateScale(scale)
                mViewBinding.viewScale.text = "???????????????$scale"
            }
        })

        mViewBinding.ivRecordHistory.setOnClickListener {
            if (!recorder.hasRecord()) {
                mViewModel.newRecordCount.postValue(0)
                start<RecordHistoryActivity>()
            } else {
                recorder.complete()
            }
        }


        mViewBinding.ivRecordState.setOnClickListener {
            startRequestPermission("startRequestPermission", Manifest.permission.RECORD_AUDIO) {
                if (it) {
                    launch {
                        if (mViewModel.recordOverWriter.isTrue()) {
                            mViewBinding.waveview.startOverwrite()
                        }
                        recorder.startOrPause()
                    }
                }
            }
        }

        mViewModel.timeLiveData.observe(this) {
            mViewBinding.tvRecordTime.text = Util.formatSeconds4(it)
        }

        mViewModel.newRecordCount.observe(this) {
            mViewBinding.ivRecordHistory.updateCount(it)
        }

        mViewModel.recordOverWriter.observe(this) {
            if (recorder.canOverWriter()) {
                if (it) {
                    mViewBinding.recordStateMsg.text = "????????????"
                } else {
                    mViewBinding.recordStateMsg.text = "????????????"
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
        //????????????????????????????????????
        //1. ???????????????
        val leftAudio = recorder.getAudioFileName("left")
        val leftCutCommand =
            buildCutCommand(recorder.getSaveUrl(), leftAudio, 0.0, startTime.toDouble())
        leftCutCommand.toJson().logI("record")
        val leftFFmpegState = FFmpegKit.runCommand(leftCutCommand)
        if (leftFFmpegState != OnFinish) {
            FileUtils.deleteFile(leftAudio)
            return false
        }

        //2. ????????????
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

        //3.??????
        val mergeAudio = recorder.getAudioFileName("merge")
        val mergeCommand = buildMergeCommand(leftAudio, rightAudio, output = mergeAudio)
        mergeCommand.toJson().logI("record")
        val mergeState = FFmpegKit.runCommand(mergeCommand)
        //4.???????????????????????????
        FileUtils.deleteFile(leftAudio)
        FileUtils.deleteFile(rightAudio)
        if (mergeState == OnFinish) {
            //?????????????????????????????????????????????????????????????????????
            FileUtils.deleteFile(recorder.getSaveUrl())
            recorder.updateSaveFile(mergeAudio)
            return true
        }
        return false
    }


    override fun onBackPressed() {
        super.onBackPressed()
        recorder.pause()
    }

    override fun initData() {
        super.initData()
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
     * ??????UI??????
     */
    fun updateUIState(state: RecordState) {
        mViewBinding.ivRecordHistory.updateState(state)
        mViewBinding.ivRecordState.updateState(state)
        val canPlay = state != RecordState.RECORDING && recorder.hasRecord()
        if (canPlay) {
            mViewBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.black))
            //??????????????????
            mViewModel.difDuration =
                getRecordDuration() - Util.getAudioLength(recorder.getSaveUrl())
        } else {
            mViewBinding.ivRecordPlay.imageTintList =
                ColorStateList.valueOf(ContextCompat.getColor(this, me.shetj.base.R.color.blackHintText))
        }
        mViewBinding.ivRecordPlay.isEnabled = canPlay
        mViewBinding.playTime.isVisible = canPlay
        mViewBinding.startCut.isVisible = state == RecordState.PAUSED
        when (state) {
            RecordState.RECORDING -> {
                mViewBinding.recordStateMsg.text = "????????????"
                needToStart = true
                mViewBinding.waveview.setEnableScroll(false)
            }
            RecordState.PAUSED -> {
                mViewBinding.waveview.setEnableScroll(true)
                mViewBinding.recordStateMsg.text = "????????????"
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