package com.shetj.demo.record.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "record_type")
class RecType {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id:Long = 0

    @ColumnInfo
    var background: String? = null

    @ColumnInfo
    var typeName: String? = null

    @ColumnInfo
    var createTime: String? = null

    @ColumnInfo
    var updateTime: String? = null

    @ColumnInfo
    var isTop: Boolean = false


    companion object{
        fun mock(block: RecType.() ->Unit): RecType {
            return RecType().apply(block)
        }
    }
}
