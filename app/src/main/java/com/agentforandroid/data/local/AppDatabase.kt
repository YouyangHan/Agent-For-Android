package com.agentforandroid.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.agentforandroid.data.local.dao.ConfigDao
import com.agentforandroid.data.local.dao.MessageDao
import com.agentforandroid.data.local.dao.SessionDao
import com.agentforandroid.data.local.entity.ConfigEntity
import com.agentforandroid.data.local.entity.MessageEntity
import com.agentforandroid.data.local.entity.SessionEntity

@Database(
    entities = [ConfigEntity::class, SessionEntity::class, MessageEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun configDao(): ConfigDao
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "agent_for_android.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
