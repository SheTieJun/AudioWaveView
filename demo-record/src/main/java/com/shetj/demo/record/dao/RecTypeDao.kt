package com.shetj.demo.record.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.shetj.demo.record.model.RecType


@Dao
interface RecTypeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecType(recType: RecType):Long

    @Delete
    suspend fun delRecType(recType: RecType)

    @Query("SELECT * FROM record_type ")
    fun findAllRecType():Flow<MutableList<RecType>>

    @Update
    suspend fun updateRecType(recType: RecType)
}