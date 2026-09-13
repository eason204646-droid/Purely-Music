package com.music.purelymusic

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.purelymusic.data.AppDatabase
import com.music.purelymusic.data.PlaylistEntity
import com.music.purelymusic.data.PlaylistSongCrossRef
import com.music.purelymusic.data.SongEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {
    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun songDeletionCascadesAndPlaylistOrderIsStable() = runBlocking {
        val first = SongEntity(1L, "第一首", "歌手", null, "/music/1.flac")
        val second = SongEntity(2L, "第二首", "歌手", null, "/music/2.flac")
        database.songDao().insertSong(first)
        database.songDao().insertSong(second)
        database.playlistDao().upsertPlaylist(
            PlaylistEntity("playlist", "测试歌单", null),
            listOf(
                PlaylistSongCrossRef("playlist", 2L, 0),
                PlaylistSongCrossRef("playlist", 1L, 1)
            )
        )

        val initial = database.playlistDao().getAllPlaylists().single().songRefs.sortedBy { it.position }
        assertEquals(listOf(2L, 1L), initial.map { it.songId })

        database.songDao().deleteSong(second)
        val remaining = database.playlistDao().getAllPlaylists().single().songRefs
        assertEquals(listOf(1L), remaining.map { it.songId })
    }
}
