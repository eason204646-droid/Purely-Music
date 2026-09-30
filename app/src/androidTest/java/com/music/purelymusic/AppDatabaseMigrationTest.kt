// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.purelymusic.data.AlbumEntity
import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.PlaylistEntity
import com.music.purelymusic.data.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val databaseName = "upgrade-from-2-7-test.db"
    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        context.deleteDatabase(databaseName)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(databaseName)
    }

    @Test
    fun emptyVersion27DatabaseOpensAndAcceptsNewData() = runBlocking {
        createVersion9Database()
        val migrated = openCurrentDatabase()
        assertEquals(emptyList<SongEntity>(), migrated.songDao().getAllSongs())
        migrated.songDao().insertSong(SongEntity(title = "新歌曲", artist = "歌手", coverUri = null, musicUri = null))
        migrated.albumDao().insertAlbum(AlbumEntity("new-album", "新专辑", "歌手", null))
        migrated.playlistDao().insertPlaylistEntity(PlaylistEntity("new-playlist", "新歌单", null))
        assertEquals(1, migrated.songDao().getAllSongs().size)
        assertEquals("新专辑", migrated.albumDao().getAlbumById("new-album")?.name)
        assertEquals("新歌单", migrated.playlistDao().getAllPlaylists().single().playlist.name)
    }

    @Test
    fun version27UpgradePreservesLibraryAndPlaylistOrder() = runBlocking {
        createVersion9Database(populated = true)
        assertPreservedData(openCurrentDatabase())
        // The stored Room identity and schema must also work on subsequent launches.
        database?.close()
        assertPreservedData(openCurrentDatabase())
    }

    @Test
    fun previouslyUpgradedVersion27DatabasePreservesData() = runBlocking {
        createVersion9Database(populated = true, previouslyUpgraded = true)
        assertPreservedData(openCurrentDatabase())
    }

    @Test
    fun invalidLegacyPlaylistJsonDoesNotBlockUpgrade() = runBlocking {
        createVersion9Database(populated = true)
        SQLiteDatabase.openDatabase(context.getDatabasePath(databaseName).path, null, SQLiteDatabase.OPEN_READWRITE).use { legacy ->
            listOf("null", "", "not-json", "{}", "[null,\"bad-id\",2,1]").forEachIndexed { index, json ->
                legacy.execSQL(
                    "INSERT INTO playlists (id, name, songIdsJson, createdAt, updatedAt) VALUES (?, ?, ?, 0, 0)",
                    arrayOf("invalid-$index", "异常歌单 $index", json)
                )
            }
        }
        val migrated = openCurrentDatabase()
        val playlists = migrated.playlistDao().getAllPlaylists().associateBy { it.playlist.id }
        assertEquals(6, playlists.size)
        (0..3).forEach { index -> assertEquals(emptyList<Long>(), playlists.getValue("invalid-$index").songRefs.map { it.songId }) }
        assertEquals(listOf(2L, 1L), playlists.getValue("invalid-4").songRefs.sortedBy { it.position }.map { it.songId })
        assertPreservedData(migrated)
    }

    private fun openCurrentDatabase(): AppDatabase = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
        .addMigrations(AppDatabase.MIGRATION_9_10)
        .allowMainThreadQueries()
        .build()
        .also {
            database = it
            // Force migration and Room validation; build() alone does not open the database.
            assertEquals(10, it.openHelper.writableDatabase.version)
            it.openHelper.writableDatabase.query("PRAGMA foreign_key_check").use { cursor -> assertFalse(cursor.moveToFirst()) }
        }

    private suspend fun assertPreservedData(migrated: AppDatabase) {
        val songs = migrated.songDao().getAllSongs().associateBy { it.id }
        assertEquals(3, songs.size)
        assertEquals(
            SongEntity(1L, "第一首", "歌手", "/covers/1.jpg", "/music/1.flac", "/lyrics/1.lrc", 1234L, 7, 100L, 1, 90000L, "旧专辑", "album-1"),
            songs[1L]
        )
        assertNull(songs.getValue(2L).albumId)
        assertNull(songs.getValue(9007199254740993L).albumId)
        assertEquals(AlbumEntity("album-1", "旧专辑", "歌手", "/covers/album.jpg", 200L), migrated.albumDao().getAlbumById("album-1"))
        val playlist = migrated.playlistDao().getAllPlaylists().single { it.playlist.id == "playlist-1" }
        assertEquals(PlaylistEntity("playlist-1", "中文歌单", "/covers/playlist.jpg", "保留描述", 300L, 400L), playlist.playlist)
        assertEquals(listOf(2L, 1L, 9007199254740993L), playlist.songRefs.sortedBy { it.position }.map { it.songId })
        migrated.openHelper.writableDatabase.query("SELECT seq FROM sqlite_sequence WHERE name = 'songs'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(9007199254740994L, cursor.getLong(0))
        }
    }

    /** DDL from the 2.7 entities. Earlier migrations also created defaults and two indices. */
    private fun createVersion9Database(populated: Boolean = false, previouslyUpgraded: Boolean = false) {
        val file = context.getDatabasePath(databaseName)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { legacy ->
            val nullableDefault = if (previouslyUpgraded) "" else " DEFAULT null"
            val timestampDefault = if (previouslyUpgraded) " DEFAULT 0" else ""
            legacy.execSQL(
                """
                CREATE TABLE songs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    title TEXT NOT NULL, artist TEXT NOT NULL, coverUri TEXT, musicUri TEXT,
                    lrcPath TEXT DEFAULT null,
                    lastPlayedTime INTEGER NOT NULL DEFAULT 0, playCount INTEGER NOT NULL DEFAULT 0,
                    createdTime INTEGER NOT NULL DEFAULT 0, isFavorite INTEGER NOT NULL DEFAULT 0,
                    duration INTEGER NOT NULL DEFAULT 0, album TEXT$nullableDefault
                )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                CREATE TABLE playlists (
                    id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, coverUri TEXT,
                    songIdsJson TEXT NOT NULL, description TEXT,
                    createdAt INTEGER NOT NULL$timestampDefault, updatedAt INTEGER NOT NULL$timestampDefault
                )
                """.trimIndent()
            )
            legacy.execSQL(
                "CREATE TABLE albums (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, artist TEXT NOT NULL, coverUri TEXT, createdAt INTEGER NOT NULL$timestampDefault)"
            )
            if (previouslyUpgraded) {
                legacy.execSQL("CREATE INDEX index_songs_lastPlayedTime ON songs(lastPlayedTime)")
                legacy.execSQL("CREATE INDEX index_songs_isFavorite ON songs(isFavorite)")
            }
            if (populated) {
                legacy.execSQL("INSERT INTO albums VALUES ('album-1', '旧专辑', '歌手', '/covers/album.jpg', 200)")
                legacy.execSQL("INSERT INTO songs VALUES (1, '第一首', '歌手', '/covers/1.jpg', '/music/1.flac', '/lyrics/1.lrc', 1234, 7, 100, 1, 90000, '旧专辑')")
                legacy.execSQL("INSERT INTO songs VALUES (2, '第二首', '歌手', NULL, '/music/2.flac', NULL, 0, 0, 101, 0, 80000, NULL)")
                legacy.execSQL("INSERT INTO songs VALUES (9007199254740993, '大编号歌曲', '歌手', NULL, '/music/3.flac', NULL, 0, 0, 102, 0, 70000, '未关联专辑')")
                legacy.execSQL("INSERT INTO playlists VALUES ('playlist-1', '中文歌单', '/covers/playlist.jpg', '[2,999,1,2,9007199254740993]', '保留描述', 300, 400)")
                // AUTOINCREMENT must not reuse a previously allocated ID after table rebuilding.
                legacy.execSQL("UPDATE sqlite_sequence SET seq = 9007199254740994 WHERE name = 'songs'")
            }
            legacy.version = 9
        }
    }
}
