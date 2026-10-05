package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.LeaseInspectionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaseDao {
    @Query("SELECT * FROM lease_inspections ORDER BY analyzedDate DESC")
    fun getAllInspections(): Flow<List<LeaseInspectionEntity>>

    @Query("SELECT * FROM lease_inspections WHERE id = :id LIMIT 1")
    fun getInspectionById(id: Long): Flow<LeaseInspectionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInspection(entity: LeaseInspectionEntity): Long

    @Query("DELETE FROM lease_inspections WHERE id = :id")
    suspend fun deleteInspectionById(id: Long)

    @Query("DELETE FROM lease_inspections")
    suspend fun clearAll()
}
