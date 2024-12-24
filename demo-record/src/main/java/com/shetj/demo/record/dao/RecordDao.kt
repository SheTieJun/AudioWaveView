package com.shetj.demo.record.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy.Companion.REPLACE
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.shetj.demo.record.model.Record


@Dao
interface RecordDao {

    @Insert(onConflict = REPLACE)
    suspend fun insertRecord(record: Record): Long


    @Insert(onConflict = REPLACE)
    suspend fun insert(record: Record)

    @Delete()
    suspend fun deleteRecord(record: Record)


    @Query("SELECT * FROM record order by id DESC")
    fun getAllRecord(): Flow<MutableList<Record>>


    @Query("select * from record order by id DESC limit 1 ")
    fun getLastRecord(): Flow<Record>


    @Query("DELETE FROM record")
    suspend fun deleteAll()


    @Query("SELECT * FROM record order by id DESC")
    fun getAllRecordInfo(): Flow<List<Record>>


    @Query("SELECT * FROM record where audio_name like '%' || :key || '%' ")
    suspend fun search(key: String): List<Record>

    @Delete
    fun deleteList(collectionDataList: List<Record>)
}