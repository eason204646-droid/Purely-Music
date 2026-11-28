// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.music.purelymusic.ui.utils.extractAlbumTitle
import com.music.purelymusic.ui.utils.fillAlbumTitleOnCover
import com.music.purelymusic.ui.utils.prepareImportedAlbumTitle
import com.music.purelymusic.ui.utils.refineTitleMatte
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumArtworkTest {
    @Test
    fun extractionKeepsLetterColorAndHolesWithoutChangingTheOriginalCover() = runBlocking {
        val cover = Bitmap.createBitmap(240, 160, Bitmap.Config.ARGB_8888)
        val background = Color.rgb(24, 35, 39)
        val lettering = Color.rgb(13, 218, 195)
        val canvas = Canvas(cover)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lettering }
        canvas.drawColor(background)
        canvas.drawRect(35f, 55f, 168f, 109f, paint)
        paint.color = background
        canvas.drawRect(55f, 71f, 149f, 93f, paint)
        paint.color = lettering
        canvas.drawCircle(186f, 83f, 8.5f, paint)
        cover.setPixel(24, 48, lettering) // Isolated noise inside the selection.

        val extraction = checkNotNull(extractAlbumTitle(cover, Rect(15, 42, 211, 122), 0.5f, null))
        val result = extraction.artwork
        assertEquals(162, result.width)
        assertEquals(56, result.height)
        assertEquals(0, Color.alpha(result.getPixel(60, 25)))
        assertEquals(lettering, result.getPixel(6, 6))
        assertTrue((0 until result.width).any { x ->
            (0 until result.height).any { y -> Color.alpha(result.getPixel(x, y)) in 1..254 }
        })

        val cleaned = fillAlbumTitleOnCover(cover, extraction)
        assertEquals(background, cleaned.getPixel(38, 60))
        assertEquals(background, cleaned.getPixel(5, 5))
        assertEquals(lettering, cover.getPixel(38, 60))
    }

    @Test
    fun sampledColorFindsLowContrastLettering() = runBlocking {
        val cover = Bitmap.createBitmap(120, 80, Bitmap.Config.ARGB_8888)
        val paint = Paint().apply { color = Color.rgb(90, 126, 131) }
        val canvas = Canvas(cover)
        canvas.drawColor(Color.rgb(83, 108, 117))
        canvas.drawRect(30f, 32f, 100f, 50f, paint)
        val selection = Rect(20, 25, 110, 56)

        assertNull(extractAlbumTitle(cover, selection, 0.5f, null))
        val sampled = checkNotNull(extractAlbumTitle(cover, selection, 0.5f, Point(52, 40)))
        assertEquals(paint.color, sampled.artwork.getPixel(6, 6))
    }

    @Test
    fun aTightSelectionDoesNotTrainOnTheLetterStrokeJustOutsideIt() = runBlocking {
        val cover = Bitmap.createBitmap(280, 145, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        val paint = Paint().apply { color = Color.rgb(17, 230, 220) }
        canvas.drawColor(Color.rgb(10, 25, 30))
        canvas.drawRect(165f, 51f, 234f, 88f, paint)
        val selection = Rect(22, 38, 249, 83)

        val extracted = checkNotNull(extractAlbumTitle(cover, selection, 0.5f, null)).artwork
        assertTrue((0 until extracted.width).count { x -> extracted.getPixel(x, 9) == paint.color } >= 65)
    }

    @Test
    fun shadedStrokeContinuesAcrossAmbiguousPixelsWithoutFillingItsHole() = runBlocking {
        val width = 100
        val height = 54
        val colors = IntArray(width * height) { Color.rgb(14, 26, 30) }
        val matte = FloatArray(colors.size)
        for (y in 10 until 44) {
            for (x in 11 until 89) {
                if (x in 33..66 && y in 22..31) continue
                val index = y * width + x
                val shadow = x in 46..51
                colors[index] = if (shadow) Color.rgb(15, 99, 117) else Color.rgb(19, 124, 142)
                matte[index] = if (shadow) 0.36f else 1f
            }
        }

        refineTitleMatte(colors, matte, width, height)

        assertTrue(matte[16 * width + 48] > 0.65f)
        assertTrue(matte[27 * width + 48] < 0.05f)
        assertTrue(matte[9 * width + 48] < 0.05f)
    }

    @Test
    fun aDistinctInkRejectsDetachedHighlightsOfUnrelatedColors() = runBlocking {
        val cover = Bitmap.createBitmap(260, 140, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        val paint = Paint().apply { color = Color.rgb(12, 212, 224) }
        canvas.drawColor(Color.rgb(13, 26, 31))
        canvas.drawRect(48f, 57f, 174f, 94f, paint)
        paint.color = Color.WHITE
        canvas.drawRect(29f, 53f, 34f, 85f, paint)
        paint.color = Color.rgb(110, 189, 48)
        canvas.drawRect(186f, 49f, 209f, 55f, paint)
        val region = Rect(19, 41, 232, 103)
        val extracted = checkNotNull(extractAlbumTitle(cover, region, 0.5f, null))
        fun alpha(x: Int, y: Int) = Color.alpha(extracted.matte[(y - region.top) * region.width() + x - region.left])

        assertEquals(255, alpha(70, 76))
        assertEquals(0, alpha(31, 70))
        assertEquals(0, alpha(196, 52))
    }

    @Test
    fun colorFromTheOppositeEdgeDoesNotEraseAStrokeNearTheLowerEdge() = runBlocking {
        val cover = Bitmap.createBitmap(280, 180, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(cover)
        val paint = Paint().apply { color = Color.rgb(22, 203, 213) }
        canvas.drawColor(Color.rgb(12, 24, 28))
        canvas.drawRect(0f, 0f, 280f, 60f, paint)
        canvas.drawRect(70f, 118f, 200f, 139f, paint)
        val region = Rect(20, 58, 260, 145)

        val extracted = checkNotNull(extractAlbumTitle(cover, region, 0.5f, null))
        fun alpha(x: Int, y: Int) = Color.alpha(extracted.matte[(y - region.top) * region.width() + x - region.left])
        assertTrue(alpha(100, 132) > 210)
        assertEquals(0, alpha(100, 58))
        assertEquals(0, alpha(56, 132))
    }

    @Test
    fun importingTrimsTransparentPaddingAndRejectsOpaqueOrEmptyImages() = runBlocking {
        val bitmap = Bitmap.createBitmap(140, 100, Bitmap.Config.ARGB_8888)
        assertNull(prepareImportedAlbumTitle(bitmap))
        val canvas = Canvas(bitmap)
        val paint = Paint().apply { color = Color.RED }
        canvas.drawRect(20f, 40f, 110f, 58f, paint)
        val trimmed = checkNotNull(prepareImportedAlbumTitle(bitmap))
        assertEquals(92, trimmed.width)
        assertEquals(20, trimmed.height)
        assertEquals(Color.RED, trimmed.getPixel(1, 1))
        assertEquals(0, Color.alpha(trimmed.getPixel(0, 0)))

        canvas.drawColor(Color.BLACK)
        assertNull(prepareImportedAlbumTitle(bitmap))
    }
}
