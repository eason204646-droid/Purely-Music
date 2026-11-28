package com.music.purelymusic.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryArtistTest {
    @Test
    fun findsCreditedAndLinkedAlbumsWithoutConfusingLegacySameNameAlbums() {
        val albums = listOf(
            Album(id = "credited", name = "Live", artist = "artist", coverUri = null),
            Album(id = "other", name = "Live", artist = "Another Artist", coverUri = null),
            Album(id = "compilation", name = "Mix", artist = "Various Artists", coverUri = null),
            Album(id = "legacy", name = "Duets", artist = "Various Artists", coverUri = null)
        )
        val songs = listOf(
            Song(id = 1, title = "First track", artist = " Artist ", musicUri = null, album = "Live"),
            Song(id = 2, title = "Second track", artist = "Artist", musicUri = null, albumId = "compilation"),
            Song(id = 3, title = "Third track", artist = "ARTIST", musicUri = null, album = "Duets"),
            Song(id = 4, title = "Single", artist = "Artist", musicUri = null)
        )
        val grouped = songs.groupBy { it.artist.artistKey() }

        assertEquals(setOf("artist"), grouped.keys)
        val relatedAlbums = findArtistAlbums("artist", grouped.getValue("artist"), albums)
        assertEquals(listOf("credited", "compilation", "legacy"), relatedAlbums.map { it.id })
    }
}
