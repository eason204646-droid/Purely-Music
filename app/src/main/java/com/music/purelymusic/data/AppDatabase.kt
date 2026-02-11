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
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本1迁移到版本2的数据库结构变更
                // 如果没有结构变更，这里可以留空
            }
        }

        // 数据库迁移策略：版本 2 -> 3
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本2迁移到版本3
                // 可能添加了新字段或表
            }
        }

        // 数据库迁移策略：版本 3 -> 4
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本3迁移到版本4
                // 添加 lastPlayedTime 字段到 songs 表（如果之前没有）
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN lastPlayedTime INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
            }
        }

        // 数据库迁移策略：版本 4 -> 5
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本4迁移到版本5
                // 添加 playCount 字段到 songs 表，用于记录播放次数
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN playCount INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
                // 添加 createdTime 字段到 songs 表，用于记录歌曲添加时间
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN createdTime INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
            }
        }

        // 数据库迁移策略：版本 5 -> 6
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本5迁移到版本6
                // 添加 isFavorite 字段到 songs 表，用于标记是否收藏
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN isFavorite INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
                // 添加 duration 字段到 songs 表，用于记录歌曲时长（毫秒）
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN duration INTEGER NOT NULL DEFAULT 0")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
                // 添加 album 字段到 songs 表，用于记录专辑名称
                try {
                    database.execSQL("ALTER TABLE songs ADD COLUMN album TEXT")
                } catch (e: Exception) {
                    // 字段可能已存在，忽略错误
                }
            }
        }

        // 数据库迁移策略：版本 6 -> 7
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本6迁移到版本7
                // 首先检查 playlists 表是否存在，如果不存在则创建
                val cursor = database.query("SELECT name FROM sqlite_master WHERE type='table' AND name='playlists'")
                val tableExists = cursor.count > 0
                cursor.close()

                if (!tableExists) {
                    // 创建 playlists 表（版本7新增）
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
                    // 如果表已存在，则添加新字段
                    try {
                        database.execSQL("ALTER TABLE playlists ADD COLUMN description TEXT")
                    } catch (e: Exception) {
                        // 字段可能已存在，忽略错误
                    }
                    // 为 playlists 表添加 createdAt 字段，用于记录创建时间
                    try {
                        database.execSQL("ALTER TABLE playlists ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                    } catch (e: Exception) {
                        // 字段可能已存在，忽略错误
                    }
                    // 为 playlists 表添加 updatedAt 字段，用于记录更新时间
                    try {
                        database.execSQL("ALTER TABLE playlists ADD COLUMN updatedAt INTEGER NOT NULL DEFAULT 0")
                    } catch (e: Exception) {
                        // 字段可能已存在，忽略错误
                    }
                }

                // 创建索引以提升查询性能
                try {
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_lastPlayedTime ON songs(lastPlayedTime)")
                } catch (e: Exception) {
                    // 索引可能已存在，忽略错误
                }
                try {
                    database.execSQL("CREATE INDEX IF NOT EXISTS index_songs_isFavorite ON songs(isFavorite)")
                } catch (e: Exception) {
                    // 索引可能已存在，忽略错误
                }
            }
        }

        // 数据库迁移策略：版本 7 -> 8 (1.4.3 -> 1.5)
        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本7迁移到版本8（1.5版本）
                // 本次更新主要功能改进：播放列表功能增强、UI 优化
                // 没有数据库结构变更，空迁移即可
                // 所有用户数据都会完整保留
            }
        }

        // 数据库迁移策略：版本 8 -> 9 (添加专辑功能)
        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // 从版本8迁移到版本9（添加专辑功能）
                // 创建 albums 表
                database.execSQL("CREATE TABLE albums (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, coverUri TEXT, createdAt INTEGER NOT NULL DEFAULT 0)")
            }
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