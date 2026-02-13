//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.data

import android.content.Context
import androidx.arch.core.executor.ArchTaskExecutor
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

// 🚩 1. 必须在 entities 中加入 PlaylistEntity::class
@Database(entities = [SongEntity::class, PlaylistEntity::class, AlbumEntity::class], version = 9, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun songDao(): SongDao

    // 🚩 3. 必须显式指定返回值类型为 : PlaylistDao
    abstract fun playlistDao(): PlaylistDao

    // 专辑 DAO
    abstract fun albumDao(): AlbumDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // 数据库迁移策略：版本 1 -> 2
        private val MIGRATION_1_2 = Migration(1, 2) { _: SupportSQLiteDatabase -> }

        // 数据库迁移策略：版本 2 -> 3
        private val MIGRATION_2_3 = Migration(2, 3) { _: SupportSQLiteDatabase -> }

        // 数据库迁移策略：版本 3 -> 4
        private val MIGRATION_3_4 = Migration(3, 4) { database: SupportSQLiteDatabase ->
            database.execSQL("ALTER TABLE songs ADD COLUMN lastPlayedTime INTEGER NOT NULL DEFAULT 0")
        }

        // 数据库迁移策略：版本 4 -> 5
        private val MIGRATION_4_5 = Migration(4, 5) { database: SupportSQLiteDatabase ->
            database.execSQL("ALTER TABLE songs ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE songs ADD COLUMN createdTime INTEGER NOT NULL DEFAULT 0")
        }

        // 数据库迁移策略：版本 5 -> 6
        private val MIGRATION_5_6 = Migration(5, 6) { database: SupportSQLiteDatabase ->
            database.execSQL("ALTER TABLE songs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE songs ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE songs ADD COLUMN album TEXT")
        }

        // 数据库迁移策略：版本 6 -> 7
        private val MIGRATION_6_7 = Migration(6, 7) { database: SupportSQLiteDatabase ->
            val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='playlists'")
            val tableExists = cursor.count > 0
            cursor.close()

            if (!tableExists) {
                database.execSQL("""
                    CREATE TABLE playlists (
                        id TEXT PRIMARY KEY NOT NULL,
                        name TEXT NOT NULL,
                        coverUri TEXT,
                        songIdsJson TEXT NOT NULL,
                        description TEXT,
                        createdAt INTEGER NOT NULL DEFAULT 0,
                        updatedAt INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
            } else {
                database.execSQL("ALTER TABLE playlists ADD COLUMN description TEXT")
                database.execSQL("ALTER TABLE playlists ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE playlists ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
            }

            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_lastPlayedTime ON songs(lastPlayedTime)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isFavorite ON songs(isFavorite)")
        }

        // 数据库迁移策略：版本 7 -> 8
        private val MIGRATION_7_8 = Migration(7, 8) { _: SupportSQLiteDatabase -> }

        // 数据库迁移策略：版本 8 -> 9
        private val MIGRATION_8_9 = Migration(8, 9) { database: SupportSQLiteDatabase ->
            database.execSQL("CREATE TABLE albums (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, coverUri TEXT, createdAt INTEGER NOT NULL DEFAULT 0)")
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "am_player_db"
                )
                    // 🚩 使用 addMigrations 添加迁移策略，确保数据不会丢失
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9)
                    // 🚩 设置 WAL 模式以提高性能并确保数据持久化
                    .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                    // 🚩 允许主线程查询（仅用于调试，生产环境应该移除）
                    //.allowMainThreadQueries()
                    // 🚩 添加查询回调以便调试
                    .setQueryCallback({ sqlQuery, bindArgs ->
                        android.util.Log.d("RoomQuery", "SQL: $sqlQuery, Args: $bindArgs")
                    }, ArchTaskExecutor.getIOThreadExecutor())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}