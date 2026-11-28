// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.model

internal fun String.artistKey(): String = trim().lowercase()

internal fun findArtistAlbums(artistKey: String, songs: List<Song>, albums: List<Album>): List<Album> {
    val linkedIds = songs.mapNotNull { it.albumId }.toSet()
    val legacyNames = songs.filter { it.albumId == null }.mapNotNull { it.album }.toSet()
    val albumCountsByName = albums.groupingBy { it.name }.eachCount()
    return albums.filter { album ->
        album.id in linkedIds ||
            album.artist.artistKey() == artistKey ||
            (album.name in legacyNames && albumCountsByName[album.name] == 1)
    }
}
