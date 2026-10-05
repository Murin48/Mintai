package com.example.data.repository

import com.example.data.local.LeaseDao
import com.example.data.model.LeaseInspectionEntity
import kotlinx.coroutines.flow.Flow

class LeaseRepository(private val leaseDao: LeaseDao) {
    val allInspections: Flow<List<LeaseInspectionEntity>> = leaseDao.getAllInspections()

    fun getInspectionById(id: Long): Flow<LeaseInspectionEntity?> = leaseDao.getInspectionById(id)

    suspend fun saveInspection(entity: LeaseInspectionEntity): Long = leaseDao.insertInspection(entity)

    suspend fun deleteInspection(id: Long) = leaseDao.deleteInspectionById(id)

    suspend fun clearHistory() = leaseDao.clearAll()
}
