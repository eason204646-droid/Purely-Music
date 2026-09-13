package com.music.purelymusic

import com.music.purelymusic.data.PlaylistEntity
import com.music.purelymusic.data.PlaylistSongCrossRef
import com.music.purelymusic.data.PlaylistWithSongs
import com.music.purelymusic.data.toPlaylist
import com.music.purelymusic.utils.LanguageDetector
import com.music.purelymusic.utils.LrcParser
import com.music.purelymusic.utils.LyricTranslationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreBehaviorTest {
    @Test
    fun lrcParserSupportsMultipleTimestampsAndFractionPrecision() {
        val parsed = LrcParser.parse("[00:01.2][00:02.34]你好")

        assertEquals(listOf(1_200L, 2_340L), parsed.map { it.time })
        assertEquals(listOf("你好", "你好"), parsed.map { it.content })
    }

    @Test
    fun continuousLrcParserKeepsTextBetweenTimestamps() {
        val parsed = LrcParser.parseContinuous("[00:01.000]First[00:02.500]Second")

        assertEquals(2, parsed.size)
        assertEquals("First", parsed[0].content)
        assertEquals(2_500L, parsed[1].time)
    }

    @Test
    fun playlistRelationPreservesExplicitOrder() {
        val aggregate = PlaylistWithSongs(
            playlist = PlaylistEntity("p1", "通勤", null, null, 1L, 2L),
            songRefs = listOf(
                PlaylistSongCrossRef("p1", 20L, 1),
                PlaylistSongCrossRef("p1", 10L, 0)
            )
        )

        assertEquals(listOf(10L, 20L), aggregate.toPlaylist().songIds)
    }

    @Test
    fun languageDetectorUsesMeaningfulChineseRatio() {
        assertTrue(LanguageDetector.isLyricsChinese(listOf("你好", "世界", "hello")))
        assertFalse(LanguageDetector.isLyricsChinese(listOf("你好", "hello", "world", "again")))
    }

    @Test
    fun translatedLyricsMatchNearbyTimestampsWithoutFakingMissingLines() {
        val originals = LrcParser.parse("[00:01.000]One\n[00:02.000]Two")
        val translated = LyricTranslationParser.parse("[00:01.120]一", originals)

        assertEquals(listOf("一", null), translated)
    }

    @Test
    fun translatedLyricsDecodeEntitiesAndPlainLines() {
        val originals = LrcParser.parse("[00:01.000]One\n[00:02.000]Two")
        val translated = LyricTranslationParser.parse("第一行&amp；\n第二行", originals)

        assertEquals(listOf("第一行&", "第二行"), translated)
    }
}
