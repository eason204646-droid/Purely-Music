package com.music.PurelyPlayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

// 🚩 1. 必须在 entities 中加入 PlaylistEntity::class
// 🚩 2. version 必须升为 2，因为表结构变了
@Database(entities = [SongEntity::class, PlaylistEntity::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    // 🚩 3. 必须显式指定返回值类型为 : PlaylistDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "am_player_db"
                )
                    // 🚩 4. 增加此配置：如果版本升级失败，直接重建数据库（防止因缺少迁移逻辑而崩溃）
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}