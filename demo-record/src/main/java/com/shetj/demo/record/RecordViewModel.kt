package com.shetj.demo.record

import android.os.Build.VERSION_CODES.S
import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.shetj.demo.record.dao.AppDatabase
import com.shetj.demo.record.dao.RecordDao
import com.shetj.demo.record.model.Record
import com.shetj.demo.record.utils.Util
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.withContext
import me.shetj.base.BaseKit
import me.shetj.base.ktx.launch
import me.shetj.base.mvvm.BaseViewModel
import me.shetj.base.tools.app.ArmsUtils
import me.shetj.base.tools.file.FileUtils
import org.koin.java.KoinJavaComponent

class RecordViewModel :BaseViewModel(){
    val newRecordCount: MutableLiveData<Int> = MutableLiveData<Int>(0)


    val recordOverWriter = MutableLiveData<Boolean>(false)

    var difDuration = 0L

    private val pointList: MutableList<Long> = mutableListOf()

    fun saveRecordToDB(file: String) {
        newRecordCount.postValue((newRecordCount.value ?: 0) + 1)
        launch {
            val record = Record(
                file, Util.getAudioLength(file),
                ""
            )
            record.saveOrUpdate()
            pointList.clear()
        }
    }

    fun addKeyPoint() {
        timeLiveData.value?.let { pointList.add(it) }
    }

    val timeLiveData = MutableLiveData<Long>()


    fun getVoiceLevel(it: Int): Float {
        val currentLevel = when {
            it <= 0 -> 0.8f
            it < 20 -> (0.5f) + (it - 20) / 10f
            it in 20..30 -> (0.8f) + (it - 20) / 10f
            it in 40..55 -> (1.0f) + (it - 40) / 15f
            it in 55..58 -> (1.2f) + (it - 55) / 3f
            it in 58..60 -> (2.2f) + (it - 58) / 2f
            it in 60..62 -> (3.3f) + (it - 60) / 2f
            it in 62..65 -> (4.6f) + (it - 62) / 3f
            it in 65..68 -> (5.9f) + (it - 65) / 3f
            it in 68..70 -> (7.1f) + (it - 68) / 3f
            it in 70..75 -> (8.5f) + (it - 70) / 5f
            it in 75..80 -> (11f) + (it - 75) / 5f
            else -> 13.5f
        }
        return currentLevel
    }
}

val recordDao: RecordDao by lazy {
    AppDatabase.getInstance(BaseKit.app).recordDao()
}

@WorkerThread
suspend fun Record.saveOrUpdate() = withContext(Dispatchers.IO) {
    kotlin.runCatching {
        recordDao.insert(this@saveOrUpdate)
    }
}

suspend fun Record.save() {
    withContext(Dispatchers.IO) {
        recordDao.insertRecord(this@save)
    }

}

/**
 * 更新录音
 */
suspend fun Record.updateRecord() {
    withContext(Dispatchers.IO) {
        recordDao.insertRecord(this@updateRecord)
    }
}


suspend fun List<Record>.delete(){
    withContext(Dispatchers.IO) {
        recordDao.deleteList(this@delete)
    }
}

/**
 * 删除
 */
suspend fun Record.delRecord() {
    withContext(Dispatchers.IO) {
        recordDao.deleteRecord(this@delRecord)
        FileUtils.deleteFile(File(this@delRecord.audioUrl!!))
    }
}

suspend fun clear() {
    withContext(Dispatchers.IO) {
        recordDao.deleteAll()
    }
}


fun getAllRecordList() = recordDao.getAllRecordInfo()

suspend fun searchRecord(key: String) = withContext(Dispatchers.IO) {
    recordDao.search(key)
}
