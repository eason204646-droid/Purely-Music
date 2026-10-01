package com.music.purelymusic.utils

import com.music.purelymusic.model.LrcLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricTranslationParserTest {
    private val originals = linkedMapOf(
        10 to LrcLine(45_000, "I miss you more than life"),
        11 to LrcLine(48_000, "And if you can't be next to me"),
        12 to LrcLine(51_000, "Your memory is ecstasy"),
        13 to LrcLine(54_000, "I miss you more than life")
    )
    private val expected = mapOf(
        10 to "我比生命更想念你",
        11 to "如果你不能在我身边",
        12 to "你的记忆是狂喜的",
        13 to "我比生命还要想念你"
    )

    @Test
    fun screenshotResponseKeepsTranslationsOnTheirOwnLines() {
        val response = "[[PMT_0010]]我比生命更想念你" +
            "[/PMT_0011]]如果你不能在我身边" +
            "[PMT_0012]你的记忆是狂喜的" +
            "[[PMT_0013]我比生命还要想念你"

        assertEquals(expected, LyricTranslationParser.parseBatch(response, originals))
    }

    @Test
    fun damagedBracketsWhitespaceAndSlashesStillIdentifyLineBoundaries() {
        val response = "[ / P M T _ 0010 ]] 我比生命更想念你\n" +
            "【PTM＿0011】 如果你不能在我身边\n" +
            "［［ptc－0012］ 你的记忆是狂喜的\n" +
            "PMT_0013 我比生命还要想念你"

        assertEquals(expected, LyricTranslationParser.parseBatch(response, originals))
    }

    @Test
    fun htmlEncodedMarkersAreDecodedBeforeSplitting() {
        val response = "&#91;&#91;PMT_0010&#93;&#93;我比生命更想念你" +
            "&#x5b;PMT_0011&#x5d;如果你不能在我身边" +
            "&amp;#91;PMT_0012&amp;#93;你的记忆是狂喜的" +
            "&#91；PMT_0013&#93；我比生命还要想念你"

        assertEquals(expected, LyricTranslationParser.parseBatch(response, originals))
    }

    @Test
    fun markedResponseMayReturnLinesInADifferentOrder() {
        val response = "[[PMT_0013]]我比生命还要想念你\n" +
            "[[PMT_0011]]如果你不能在我身边\n" +
            "[[PMT_0012]]你的记忆是狂喜的\n" +
            "[[PMT_0010]]我比生命更想念你"

        assertEquals(expected, LyricTranslationParser.parseBatch(response, originals))
    }

    @Test
    fun lostMarkerInvalidatesBatchInsteadOfAttachingTwoTranslationsToOneLine() {
        val response = "[[PMT_0010]]我比生命更想念你 如果你不能在我身边" +
            "[[PMT_0012]]你的记忆是狂喜的[[PMT_0013]]我比生命还要想念你"

        assertTrue(LyricTranslationParser.parseBatch(response, originals).isEmpty())
    }

    @Test
    fun unlabelledPrefixInvalidatesBatchInsteadOfBeingSilentlyDropped() {
        val response = "多出来的一句\n" + expected.entries.joinToString("\n") {
            "[[PMT_${it.key}]]${it.value}"
        }

        assertTrue(LyricTranslationParser.parseBatch(response, originals).isEmpty())
    }

    @Test
    fun duplicateMarkersCannotOverwriteAnotherTranslation() {
        val response = "[[PMT_0010]]第一句[[PMT_0011]]第二句" +
            "[[PMT_0012]]第三句[[PMT_0012]]第四句"

        assertTrue(LyricTranslationParser.parseBatch(response, originals).isEmpty())
        assertFalse(LyricTranslationParser.parseMarked(response).containsKey(12))
    }

    @Test
    fun markerFromAnotherBatchCannotPopulateTheCurrentBatch() {
        val response = "[[PMT_0010]]第一句[[PMT_0011]]第二句" +
            "[[PMT_0012]]第三句[[PMT_0099]]第四句"

        assertTrue(LyricTranslationParser.parseBatch(response, originals).isEmpty())
    }

    @Test
    fun corruptedOrUnknownExtraMarkersInvalidateOtherwiseCompleteBatch() {
        val complete = expected.entries.joinToString("\n") { "[[PMT_${it.key}]]${it.value}" }

        for (marker in listOf("[PMT_bad]", "[[UNKNOWN_9]]", "PMT_99999999999999999999")) {
            assertTrue(LyricTranslationParser.containsTranslationMarker(marker))
            assertTrue(LyricTranslationParser.parseBatch("$complete $marker 错误的额外译文", originals).isEmpty())
        }
    }

    @Test
    fun collapsedOrPartialPlainResponseDoesNotAssignTextByGuessing() {
        for (response in listOf("第一句 第二句 第三句 第四句", "第一句\n第三句\n第四句")) {
            assertTrue(LyricTranslationParser.parseBatch(response, originals).isEmpty())
        }
    }

    @Test
    fun completePlainResponseCanStillUseLineOrder() {
        assertEquals(expected, LyricTranslationParser.parseBatch(expected.values.joinToString("\n"), originals))
    }

    @Test
    fun timestampedResponseOnlyUsesMatchingTimestamps() {
        assertEquals(
            mapOf(11 to "如果你不能在我身边"),
            LyricTranslationParser.parseBatch("[00:48.100]如果你不能在我身边", originals)
        )
    }

    @Test
    fun sanitizerNeverDisplaysDamagedLineMarkers() {
        for (marker in listOf("[/PMT_0011]]", "[PMT_0012]", "[[PMT_0013]", "PMT_0013", "[PMT_bad]")) {
            assertEquals("译文", LyricTranslationParser.sanitizeTranslation("$marker 译文"))
        }
        assertNull(LyricTranslationParser.sanitizeTranslation("[/PMT_0011]]"))
    }

    @Test
    fun ordinaryLyricTextAndInvalidHtmlEntitiesArePreserved() {
        val text = "你的记忆 [如此鲜活] &amp; &#x1F3B5; &#999999999999999999;"

        assertEquals(
            "你的记忆 [如此鲜活] & 🎵 &#999999999999999999;",
            LyricTranslationParser.sanitizeTranslation(text)
        )
        assertFalse(LyricTranslationParser.containsTranslationMarker(text))
    }
}
