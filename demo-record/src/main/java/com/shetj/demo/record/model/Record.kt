package com.shetj.demo.record.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import me.shetj.base.tools.time.DateUtils

/**
 * 录音
 */
@Entity(tableName = "record")
class Record {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0

    @ColumnInfo
    var recTypeId: Int = 0

    @ColumnInfo(name = "audio_url")
    var audioUrl: String? = null//保存的路径

    @ColumnInfo(name = "audio_name")
    var audioName: String? = null//录音的名称

    @ColumnInfo(name = "audio_length")
    var audioLength: Int = 0//长度

    @ColumnInfo(name = "audio_content")
    var audioContent: String? = null//内容

    @ColumnInfo(name = "otherInfo")
    var otherInfo: String? = null// 预览信息

    @ColumnInfo(name = "create_time")
    var createTime: Long = 0 //保存时间
        set(value) {
            createTimeDate = getDay(value)
            createTimeMoth = getMoth(value)
            audioName = getTimeName(value)
            field = value
        }

    @ColumnInfo(name = "create_time_date")
    var createTimeDate: String? = null  //保存时间(yyyy-MM-dd)

    @ColumnInfo(name = "create_time_moth")
    var createTimeMoth: String? = null  //保存时间(yyyy-MM)


    constructor()

    constructor(
        audio_url: String,
        audioName: String,
        audioLength: Int,
        content: String
    ) {
        this.audioUrl = audio_url
        this.audioName = audioName
        this.audioLength = audioLength
        this.audioContent = content
        this.createTime = System.currentTimeMillis()
    }


    constructor(
        audio_url: String,
        audioLength: Int,
        content: String
    ) {
        this.audioUrl = audio_url
        this.audioLength = audioLength
        this.audioContent = content
        this.createTime = System.currentTimeMillis()
    }
}


fun getMoth(time: Long): String {
    return SimpleDateFormat("yyyy年MM月").format(time)
}


fun getDay(time: Long): String {
    return SimpleDateFormat("yyyy年MM月dd日").format(time)
}

fun getTimeName(time: Long): String {
    return SimpleDateFormat(DateUtils.FORMAT_YMDHM_CN).format(time)
}
