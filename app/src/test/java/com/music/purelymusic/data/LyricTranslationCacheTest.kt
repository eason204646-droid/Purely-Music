package com.music.purelymusic.data

import com.music.purelymusic.model.LrcLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class LyricTranslationCacheTest {
    @get:Rule
    val temporary = TemporaryFolder()

    private val lyrics = listOf(
        LrcLine(1000, "Hello"),
        LrcLine(1000, "Goodbye", isContinuation = true),
        LrcLine(2000, "")
    )

    @Test
    fun completeTranslationIsRestoredWithLineOrderAndBlankLines() {
        val directory = temporary.newFolder()
        val translated = listOf(lyrics[0].copy(translation = "你好"), lyrics[1].copy(translation = "再见"), lyrics[2])
        LyricTranslationCache(directory).write(translated)

        val reopened = LyricTranslationCache(directory)
        assertEquals(translated, reopened.read(lyrics))
        assertNull(reopened.read(listOf(lyrics[0].copy(content = "Good morning"), lyrics[1], lyrics[2])))
        assertNull(reopened.read(listOf(lyrics[0], lyrics[1].copy(time = 1500), lyrics[2])))
    }

    @Test
    fun incompleteOrDamagedTranslationIsACacheMiss() {
        val directory = temporary.newFolder()
        val cache = LyricTranslationCache(directory)
        cache.write(listOf(lyrics[0].copy(translation = "你好"), lyrics[1], lyrics[2]))
        assertNull(cache.read(lyrics))

        cache.write(listOf(lyrics[0].copy(translation = "你好"), lyrics[1].copy(translation = "再见"), lyrics[2]))
        directory.listFiles()!!.single().writeText("[\"你好\",null,null]")
        assertNull(cache.read(lyrics))
        directory.listFiles()!!.single().writeText("broken json")
        assertNull(cache.read(lyrics))
    }
}
