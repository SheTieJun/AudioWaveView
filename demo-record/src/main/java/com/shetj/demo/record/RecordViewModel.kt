package com.shetj.demo.record

import androidx.annotation.WorkerThread
import androidx.lifecycle.MutableLiveData
import com.shetj.demo.record.dao.AppDatabase
import com.shetj.demo.record.dao.RecordDao
import com.shetj.demo.record.model.Record
import com.shetj.demo.record.utils.Util
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.shetj.base.BaseKit
import me.shetj.base.ktx.launch
import me.shetj.base.mvvm.viewbind.BaseViewModel
import me.shetj.base.tools.file.FileUtils

class RecordViewModel : BaseViewModel(){
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
        val currentLevel = (it - 45).coerceAtLeast(0).coerceAtMost(50)
        if (currentLevel > 15){
            return (currentLevel/4f).coerceAtMost(10f)
        }
        return currentLevel/6f
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
