package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.LeaseInspectionEntity

@Database(
    entities = [LeaseInspectionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class LeaseDatabase : RoomDatabase() {
    abstract fun leaseDao(): LeaseDao

    companion object {
        @Volatile
        private var INSTANCE: LeaseDatabase? = null

        fun getInstance(context: Context): LeaseDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LeaseDatabase::class.java,
                    "leasecheck_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
