package com.chatwing.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chatwing.db.dao.ContactDao
import com.chatwing.db.dao.MemoryDao
import com.chatwing.db.entity.ContactEntity
import com.chatwing.db.entity.MemoryEntity

/**
 * ChatWing 本地数据库
 * 使用 SQLite + Room，数据完全隔离在本地
 * 每个联系人的记忆独立存储，互不干扰
 */
@Database(
    entities = [ContactEntity::class, MemoryEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun contactDao(): ContactDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        private const val DB_NAME = "chatwing.db"

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
