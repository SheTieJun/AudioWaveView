package com.shetj.demo.record

import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.text.TextUtils
import kotlinx.coroutines.delay
import me.shetj.base.BaseKit.isDebug
import me.shetj.base.tools.file.EnvironmentStorage
import me.shetj.player.PlayerListener
import me.shetj.recorder.core.BaseRecorder
import me.shetj.recorder.core.FileUtils
import me.shetj.recorder.core.PermissionListener
import me.shetj.recorder.core.RecordListener
import me.shetj.recorder.core.RecordState
import me.shetj.recorder.core.SimRecordListener
import me.shetj.recorder.core.recorder
import me.shetj.recorder.mixRecorder.buildMix

/**
 * 录音工具类
 */
class RecorderKit(
    private val context: Context?,
    private val callBack: SimRecordListener?
) : RecordListener, PermissionListener {

    companion object {
        fun clearCache(context: Context) {
            FileUtils.deleteFile(
                EnvironmentStorage.getPath(
                    root = context.filesDir.absolutePath,
                    packagePath = "record"
                )
            )
        }
    }

    val maxDuration = 24 * 60 * 60 * 1000L

    private var hasRecord = false

    fun hasRecord(): Boolean {
        return hasRecord
    }

    init {
        initRecorder()
    }

    private var mRecorder: BaseRecorder? = null
    private var saveFile = ""

    suspend fun startOrPause(file: String = "") {
        if (mRecorder == null) {
            initRecorder()
        }
        when (mRecorder?.state) {
            RecordState.STOPPED -> {
                if (TextUtils.isEmpty(file)) {
                    val mRecordFile =
                        EnvironmentStorage.getPath(
                            root = context!!.filesDir.absolutePath,
                            packagePath = "record"
                        ) + "/" + System.currentTimeMillis() + ".mp3"
                    this.saveFile = mRecordFile
                } else {
                    this.saveFile = file
                }
                mRecorder?.setOutputFile(saveFile, isContinue = true)
                mRecorder?.muteRecord(true) //防止部分手机开头的爆破音，开启静音
                mRecorder?.start()
                delay(40)
                mRecorder?.muteRecord(false)//结束静音
                hasRecord = true
            }
            RecordState.PAUSED -> {
                mRecorder?.resume()
            }
            RecordState.RECORDING -> {
                mRecorder?.pause()
            }
            else -> {}
        }
    }

    fun getAudioFileName(name: String): String {
        return EnvironmentStorage.getPath(
            root = context!!.filesDir.absolutePath,
            packagePath = "record"
        ) + "/$name" + System.currentTimeMillis() + ".mp3"
    }


    fun updateSaveFile(file: String) {
        saveFile = file
        mRecorder?.updateDataEncode(saveFile, isContinue = true)
    }

    fun getSaveUrl() = saveFile

    fun getState() = mRecorder?.state ?: RecordState.STOPPED

    /**
     * VOICE_COMMUNICATION 消除回声和噪声问题
     * MIC 麦克风- 因为有噪音问题
     */
    private fun initRecorder() {
        mRecorder = recorder {
            mMaxTime = maxDuration
            isDebug = true
            samplingRate = 48000
            audioSource = MediaRecorder.AudioSource.MIC
            audioChannel = 1
            mp3BitRate = 128
            mp3Quality = 5
            recordListener = this@RecorderKit
            permissionListener = this@RecorderKit
            enableAudioEffect = true
        }.buildMix(context)
        mRecorder?.setMaxTime(maxDuration, maxDuration - 20 * 1000)
    }

    fun isPause(): Boolean {
        return mRecorder?.state == RecordState.PAUSED
    }

    fun canOverWriter(): Boolean {
        return mRecorder?.state != RecordState.RECORDING && hasRecord
    }

    fun setBackgroundPlayerListener(listener: PlayerListener) {
        mRecorder?.setBackgroundMusicListener(listener)
    }

    fun startOrPauseBGM() {
        if (mRecorder?.isPlayMusic() == true) {
            if (mRecorder?.isPauseMusic() == true) {
                mRecorder?.resumeMusic()
            } else {
                mRecorder?.pauseMusic()
            }
        } else {
            mRecorder?.startPlayMusic()
        }
    }

    fun pause() {
        mRecorder?.pause()
    }

    fun clear() {
        mRecorder?.destroy()
    }

    fun reset() {
        mRecorder?.reset()
    }

    /**
     * 设置开始录制时间
     * @param startTime 已经录制的时间
     */
    fun setTime(startTime: Long) {
        mRecorder?.setCurDuration(startTime)
    }


    /**
     * 录音异常
     */
    private fun resolveError() {
        FileUtils.deleteFile(saveFile)
        if (mRecorder != null && mRecorder!!.isActive) {
            mRecorder!!.complete()
        }
    }

    /**
     * 停止录音
     */
    fun complete() {
        mRecorder?.complete()
        hasRecord = false
    }

    override fun needPermission() {
        callBack?.needPermission()
    }

    override fun onStart() {
        callBack?.onStart()
    }

    override fun onSuccess(isAutoComplete: Boolean, file: String, time: Long) {
        callBack?.onSuccess(isAutoComplete, file, time)
    }

    override fun onResume() {
        callBack?.onResume()
    }

    override fun onReset() {
    }

    override fun onRecording(time: Long, volume: Int) {
        callBack?.onRecording(time, volume)
    }

    override fun onPause() {
        callBack?.onPause()
    }

    override fun onRemind(duration: Long) {
        callBack?.onRemind(duration)
    }

    override fun onMaxChange(time: Long) {
        callBack?.onMaxChange(time)
    }

    override fun onError(e: Exception) {
        resolveError()
        callBack?.onError(e)
    }

    fun setVolume(volume: Float) {
        mRecorder?.setBGMVolume(volume)
    }

    fun setBackGroundUrl(context: Context?, url: Uri) {
        if (context != null) {
            mRecorder!!.setBackgroundMusic(context, url, null)
        }
    }


    fun getDuration(): Long? {
        if (hasRecord()) {
            return mRecorder?.duration
        }
        return 0
    }

    /**
     * Update record config
     *"标准、单声道", "高清、单声道", "标准、双声道", "高清、双声道"
     * @param type
     */
    fun updateRecordConfig(type: Int) {
        if (mRecorder == null) {
            initRecorder()
        }
        when (type) {
            0 -> {
                mRecorder!!.setAudioChannel(1)
                mRecorder!!.setMp3BitRate(96)
            }
            1 -> {
                mRecorder!!.setAudioChannel(1)
                mRecorder!!.setMp3BitRate(164)
            }
            2 -> {
                mRecorder!!.setAudioChannel(2)
                mRecorder!!.setMp3BitRate(96)
            }
            3 -> {
                mRecorder!!.setAudioChannel(2)
                mRecorder!!.setMp3BitRate(96)
            }
            else -> {

            }
        }
    }
}
