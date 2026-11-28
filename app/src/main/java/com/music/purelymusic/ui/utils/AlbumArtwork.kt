// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Point
import android.graphics.Rect
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Precision
import coil.size.Scale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

internal suspend fun loadAlbumBitmap(context: Context, source: Any, size: Int = 1280): Bitmap? {
    return try {
        val request = ImageRequest.Builder(context)
            .data(source)
            .size(size, size)
            .precision(Precision.EXACT)
            .scale(Scale.FIT)
            .allowHardware(false)
            .build()
        val result = context.imageLoader.execute(request) as? SuccessResult
        withContext(Dispatchers.Default) { result?.drawable?.toBitmap() }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (error: Exception) {
        Log.w("AlbumArtwork", "Could not decode album artwork", error)
        null
    }
}

internal suspend fun prepareImportedAlbumTitle(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
    val pixels = bitmap.readPixels()
    if (pixels.none { Color.alpha(it) <= 12 }) return@withContext null
    trimmedArtwork(pixels, bitmap.width, bitmap.height)
}

internal class ExtractedAlbumTitle(
    val artwork: Bitmap,
    internal val region: Rect,
    internal val matte: IntArray
)

/**
 * Fits a local background palette from around the selection, then estimates a soft matte.
 * A sampled lettering color can guide the separation on covers with competing colors.
 */
internal suspend fun extractAlbumTitle(
    cover: Bitmap,
    region: Rect,
    strength: Float,
    foreground: Point?
): ExtractedAlbumTitle? = withContext(Dispatchers.Default) {
    require(Rect(0, 0, cover.width, cover.height).contains(region))
    val width = region.width()
    val height = region.height()
    if (width < 6 || height < 6) return@withContext null
    val pixels = cover.readPixels(region)
    val backgroundColors = IntArray(pixels.size)
    val matte = FloatArray(pixels.size)
    val bands = localBackgroundModels(cover, region, strength)
    val sampledColor = foreground?.takeIf { region.contains(it.x, it.y) }?.let {
        cover.getPixel(it.x, it.y)
    }
    val bandPositions = FloatArray(width) { x -> x.toFloat() / (width - 1) * bands.lastIndex }
    val dominantInk = if (sampledColor != null) chromaOf(sampledColor) else {
        findDominantInk(pixels, width, height, bands, bandPositions)
    }

    for (y in 0 until height) {
        ensureActive()
        val row = y.toFloat() / (height - 1)
        val rowWeight = 0.50f * smoothUnit((abs(row - 0.5f) - 0.04f) / 0.46f)
        for (x in 0 until width) {
            val index = y * width + x
            val color = pixels[index]
            val position = bandPositions[x]
            val firstIndex = position.toInt()
            val secondIndex = min(firstIndex + 1, bands.lastIndex)
            val fraction = position - firstIndex
            val first = bands[firstIndex]
            val second = bands[secondIndex]
            val firstBackground = nearestColor(first.palette, color)
            val secondBackground = if (firstIndex == secondIndex) firstBackground else nearestColor(second.palette, color)
            val firstDistance = distanceFromBackground(first, color, firstBackground, row, rowWeight)
            val secondDistance = if (firstIndex == secondIndex) firstDistance else {
                distanceFromBackground(second, color, secondBackground, row, rowWeight)
            }
            val distance = firstDistance + (secondDistance - firstDistance) * fraction
            val cutoff = first.cutoff + (second.cutoff - first.cutoff) * fraction
            val contrast = if (sampledColor == null) 0f else {
                val foregroundDistance = sqrt(colorDistanceSquared(color, sampledColor))
                (distance - foregroundDistance).coerceIn(-30f, 30f) * 0.84f + 7f
            }
            val background = if (fraction < 0.5f) firstBackground else secondBackground
            val affinity = dominantInk?.let { chromaAffinity(color, it) } ?: 1f
            // A darker print texture can still carry the ink hue of a bright stroke.
            val hueSupport = dominantInk?.let { ink ->
                val backgroundAffinity = chromaAffinity(background, ink)
                val evidence = (distance / 12f).coerceIn(0f, 1f)
                (affinity - backgroundAffinity * 0.88f).coerceAtLeast(0f) * evidence * 46f
            } ?: 0f
            val probability = smoothUnit((distance + contrast + hueSupport - cutoff + 7f) / 22f)
            // When a single ink accounts for most clear foreground pixels, keep unrelated
            // highlights from becoming foreground seeds. The manual sample can also guide mixed titles.
            val inkWeight = if (dominantInk != null && sampledColor == null) {
                val match = chromaAffinity(color, dominantInk, maxDifference = 0.46f)
                val intensity = maxOf(Color.red(color), Color.green(color), Color.blue(color))
                val penalty = 0.70f * smoothUnit((intensity - 52f) / 72f)
                1f - penalty * (1f - smoothUnit((match - 0.33f) / 0.45f))
            } else 1f
            matte[index] = probability * inkWeight
            backgroundColors[index] = background
        }
    }

    refineTitleMatte(pixels, matte, width, height)
    for (index in pixels.indices) {
        val color = pixels[index]
        pixels[index] = unblend(color, backgroundColors[index], matte[index] * (Color.alpha(color) / 255f))
    }
    removeSpecks(pixels, width, height, removeWeak = dominantInk != null && sampledColor == null)
    if (pixels.count { Color.alpha(it) < 16 } < pixels.size / 50) return@withContext null
    val artwork = trimmedArtwork(pixels, width, height) ?: return@withContext null
    ExtractedAlbumTitle(artwork, Rect(region), pixels)
}

/** Fills the extracted lettering on a display-only copy using the surrounding cover colors. */
internal suspend fun fillAlbumTitleOnCover(cover: Bitmap, title: ExtractedAlbumTitle): Bitmap =
    withContext(Dispatchers.Default) {
        val region = title.region
        val area = Rect(region).apply {
            inset(-20, -20)
            intersect(0, 0, cover.width, cover.height)
        }
        val width = area.width()
        val height = area.height()
        val pixels = cover.readPixels(area)
        val known = BooleanArray(pixels.size) { true }
        val left = region.left - area.left
        val top = region.top - area.top
        val radius = (max(cover.width, cover.height) / 480).coerceIn(1, 3)
        for (y in 0 until region.height()) {
            ensureActive()
            for (x in 0 until region.width()) {
                if (Color.alpha(title.matte[y * region.width() + x]) < 32) continue
                for (row in max(0, top + y - radius)..min(height - 1, top + y + radius)) {
                    val start = row * width + max(0, left + x - radius)
                    val end = row * width + min(width, left + x + radius + 1)
                    known.fill(false, start, end)
                }
            }
        }
        if (known.none { it }) return@withContext cover
        fillFromSurroundings(pixels, known, width, height)
        cover.copy(Bitmap.Config.ARGB_8888, true).apply {
            setPixels(pixels, 0, width, area.left, area.top, width, height)
        }
    }

private fun Bitmap.readPixels(region: Rect = Rect(0, 0, width, height)): IntArray =
    IntArray(region.width() * region.height()).also {
        getPixels(it, 0, region.width(), region.left, region.top, region.width(), region.height())
    }

private fun trimmedArtwork(pixels: IntArray, width: Int, height: Int): Bitmap? {
    var left = width
    var top = height
    var right = -1
    var bottom = -1
    var hasVisiblePixel = false
    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = Color.alpha(pixels[y * width + x])
            if (alpha <= 5) continue
            hasVisiblePixel = hasVisiblePixel || alpha >= 96
            left = min(left, x)
            right = max(right, x)
            top = min(top, y)
            bottom = max(bottom, y)
        }
    }
    if (!hasVisiblePixel) return null

    // Keep a one-pixel margin for the antialiased edge.
    left = max(0, left - 1)
    top = max(0, top - 1)
    right = min(width - 1, right + 1)
    bottom = min(height - 1, bottom + 1)
    val artwork = Bitmap.createBitmap(right - left + 1, bottom - top + 1, Bitmap.Config.ARGB_8888)
    artwork.setPixels(pixels, top * width + left, width, 0, 0, artwork.width, artwork.height)
    return artwork
}

private data class BackgroundBand(
    val palette: IntArray,
    val upperPalette: IntArray,
    val lowerPalette: IntArray,
    val cutoff: Float
)

private fun distanceFromBackground(
    band: BackgroundBand,
    color: Int,
    nearest: Int,
    row: Float,
    rowWeight: Float
): Float {
    val distance = sqrt(colorDistanceSquared(color, nearest))
    if (rowWeight == 0f) return distance
    // The area above a title can have very different colors from the area below it.
    val nearbyPalette = if (row < 0.5f) band.upperPalette else band.lowerPalette
    if (nearbyPalette === band.palette) return distance
    val nearbyDistance = sqrt(colorDistanceSquared(color, nearestColor(nearbyPalette, color)))
    return distance + (nearbyDistance - distance).coerceAtLeast(0f) * rowWeight
}

private data class InkChroma(val redGreen: Float, val blueGreen: Float)

private fun chromaOf(color: Int): InkChroma? {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    val spread = maxOf(red, green, blue) - minOf(red, green, blue)
    if (spread < 22) return null
    return InkChroma((red - green) / spread.toFloat(), (blue - green) / spread.toFloat())
}

private fun chromaAffinity(color: Int, ink: InkChroma, maxDifference: Float = 0.76f): Float {
    val red = Color.red(color)
    val green = Color.green(color)
    val blue = Color.blue(color)
    val spread = maxOf(red, green, blue) - minOf(red, green, blue)
    if (spread < 14) return 0f
    val difference = abs((red - green) / spread.toFloat() - ink.redGreen) +
        abs((blue - green) / spread.toFloat() - ink.blueGreen)
    val confidence = ((spread - 14) / 35f).coerceAtMost(1f)
    return (1f - difference / maxDifference).coerceAtLeast(0f) * confidence
}

private fun findDominantInk(
    pixels: IntArray,
    width: Int,
    height: Int,
    bands: List<BackgroundBand>,
    bandPositions: FloatArray
): InkChroma? {
    val hues = Array(24) { mutableListOf<Int>() }
    val hsv = FloatArray(3)
    val step = max(1, max(width, height) / 125)
    var candidateCount = 0
    for (y in 0 until height step step) {
        for (x in 0 until width step step) {
            val color = pixels[y * width + x]
            val model = bands[bandPositions[x].roundToInt()]
            val separation = colorDistanceSquared(color, nearestColor(model.palette, color))
            val minimum = model.cutoff + 14f
            if (separation < minimum * minimum) continue
            candidateCount++
            Color.colorToHSV(color, hsv)
            if (hsv[1] < 0.39f || hsv[2] < 0.26f) continue
            hues[(hsv[0] / 15f).toInt().coerceIn(0, hues.lastIndex)].add(color)
        }
    }
    if (candidateCount < 12) return null
    val center = hues.indices.maxBy { index ->
        hues[index].size + hues[(index + 1) % hues.size].size + hues[(index + hues.lastIndex) % hues.size].size
    }
    val neighbors = hues[center] + hues[(center + 1) % hues.size] + hues[(center + hues.lastIndex) % hues.size]
    if (neighbors.size < candidateCount * 0.73f) return null
    val colors = neighbors.mapNotNull(::chromaOf)
    if (colors.isEmpty()) return null
    return InkChroma(colors.map { it.redGreen }.average().toFloat(), colors.map { it.blueGreen }.average().toFloat())
}

private fun localBackgroundModels(cover: Bitmap, region: Rect, strength: Float): List<BackgroundBand> {
    val count = (region.width() / 130).coerceIn(2, 7)
    val spacing = region.width() / (count - 1f)
    val gap = (min(region.width(), region.height()) / 16).coerceIn(6, 24)
    val sideRows = if (region.height() > 120) 24 else 16
    val horizontalSurface = region.top >= gap || region.bottom + gap <= cover.height
    return List(count) { band ->
        val center = region.left + (spacing * band).roundToInt()
        val left = max(region.left, center - (spacing * 0.66f).roundToInt())
        val right = min(region.right, center + (spacing * 0.66f).roundToInt())
        val step = max(1, (right - left) / 64)
        val upper = ArrayList<Int>()
        val lower = ArrayList<Int>()
        val sides = ArrayList<Int>()
        // An inset selection can touch the strokes. Sample two wider bands away from that edge.
        for (distance in intArrayOf(gap, gap * 2)) {
            val top = region.top - distance
            val bottom = region.bottom - 1 + distance
            for (x in left until right step step) {
                if (top >= 0) upper += cover.getPixel(x, top)
                if (bottom < cover.height) lower += cover.getPixel(x, bottom)
            }
            val sideStep = max(1, region.height() / sideRows)
            val outsideLeft = region.left - distance
            val outsideRight = region.right - 1 + distance
            if (!horizontalSurface || band == 0 || band == count - 1) {
                for (y in region.top until region.bottom step sideStep) {
                    if (outsideLeft >= 0 && (!horizontalSurface || band == 0)) sides += cover.getPixel(outsideLeft, y)
                    if (outsideRight < cover.width && (!horizontalSurface || band == count - 1)) sides += cover.getPixel(outsideRight, y)
                }
            }
        }
        if (upper.isEmpty() && lower.isEmpty() && sides.isEmpty()) {
            for (x in left until right step step) {
                sides += cover.getPixel(x, region.top)
                sides += cover.getPixel(x, region.bottom - 1)
            }
        }
        val samples = upper + lower + sides
        val palette = backgroundPalette(samples.toIntArray())
        val distances = samples.map { color ->
            sqrt(colorDistanceSquared(color, nearestColor(palette, color)))
        }.sorted()
        val variation = distances[(distances.lastIndex * 0.85f).toInt()]
        BackgroundBand(
            palette = palette,
            upperPalette = if (upper.size >= 16) backgroundPalette(upper.toIntArray()) else palette,
            lowerPalette = if (lower.size >= 16) backgroundPalette(lower.toIntArray()) else palette,
            cutoff = (variation + 16f + (strength - 0.5f) * 56f).coerceAtLeast(3f)
        )
    }
}

private fun backgroundPalette(samples: IntArray): IntArray {
    val bins = samples.groupBy { color ->
        (Color.red(color) / 24 shl 8) or (Color.green(color) / 24 shl 4) or (Color.blue(color) / 24)
    }.values.sortedByDescending { it.size }
    val centers = ArrayList<Int>()
    for (bin in bins) {
        val color = bin.first()
        if (centers.none { colorDistanceSquared(color, it) < 21f * 21f }) centers.add(color)
        if (centers.size == 7) break
    }
    val weights = IntArray(centers.size)
    repeat(5) {
        val red = IntArray(centers.size)
        val green = IntArray(centers.size)
        val blue = IntArray(centers.size)
        weights.fill(0)
        for (sample in samples) {
            val index = centers.indices.minBy { colorDistanceSquared(centers[it], sample) }
            weights[index]++
            red[index] += Color.red(sample)
            green[index] += Color.green(sample)
            blue[index] += Color.blue(sample)
        }
        for (index in centers.indices) {
            val count = weights[index]
            if (count > 0) centers[index] = Color.rgb(red[index] / count, green[index] / count, blue[index] / count)
        }
    }
    return centers.filterIndexed { index, _ -> weights[index] >= max(1, samples.size / 65) }
        .ifEmpty { listOf(centers.first()) }.toIntArray()
}

private fun nearestColor(palette: IntArray, color: Int): Int {
    var closest = palette[0]
    var bestDistance = colorDistanceSquared(closest, color)
    for (index in 1 until palette.size) {
        val candidate = palette[index]
        val distance = colorDistanceSquared(candidate, color)
        if (distance < bestDistance) {
            closest = candidate
            bestDistance = distance
        }
    }
    return closest
}

internal fun colorDistanceSquared(first: Int, second: Int): Float {
    val red = Color.red(first) - Color.red(second)
    val green = Color.green(first) - Color.green(second)
    val blue = Color.blue(first) - Color.blue(second)
    return 0.30f * red * red + 0.46f * green * green + 0.24f * blue * blue
}

private fun unblend(color: Int, background: Int, alpha: Float): Int {
    if (alpha < 0.023f) return Color.TRANSPARENT
    if (alpha >= 0.995f) return color
    fun channel(original: Int, base: Int): Int =
        ((original - base * (1f - alpha)) / alpha).roundToInt().coerceIn(0, 255)
    return Color.argb(
        (255 * alpha).roundToInt(),
        channel(Color.red(color), Color.red(background)),
        channel(Color.green(color), Color.green(background)),
        channel(Color.blue(color), Color.blue(background))
    )
}

private fun removeSpecks(pixels: IntArray, width: Int, height: Int, removeWeak: Boolean) {
    val visited = BooleanArray(pixels.size)
    val queue = IntArray(pixels.size)
    val minimumSize = (pixels.size / 18000).coerceIn(2, 14)
    for (start in pixels.indices) {
        if (visited[start] || pixels[start] == Color.TRANSPARENT) continue
        var next = 0
        var end = 1
        var peakAlpha = 0
        queue[0] = start
        visited[start] = true
        while (next < end) {
            val position = queue[next++]
            peakAlpha = max(peakAlpha, Color.alpha(pixels[position]))
            val x = position % width
            val y = position / width
            for (row in max(0, y - 1)..min(height - 1, y + 1)) {
                for (column in max(0, x - 1)..min(width - 1, x + 1)) {
                    val neighbor = row * width + column
                    if (!visited[neighbor] && pixels[neighbor] != Color.TRANSPARENT) {
                        visited[neighbor] = true
                        queue[end++] = neighbor
                    }
                }
            }
        }
        if (end < minimumSize || (removeWeak && peakAlpha < 120)) {
            for (index in 0 until end) pixels[queue[index]] = Color.TRANSPARENT
        }
    }
}

// Downsample valid colors, then interpolate the missing pixels from the filled coarser level.
private fun fillFromSurroundings(pixels: IntArray, known: BooleanArray, width: Int, height: Int) {
    if (width == 1 && height == 1) return
    val halfWidth = (width + 1) / 2
    val halfHeight = (height + 1) / 2
    val coarse = IntArray(halfWidth * halfHeight)
    val coarseKnown = BooleanArray(coarse.size)
    for (y in 0 until halfHeight) {
        for (x in 0 until halfWidth) {
            var red = 0
            var green = 0
            var blue = 0
            var count = 0
            for (row in y * 2 until min(height, y * 2 + 2)) {
                for (column in x * 2 until min(width, x * 2 + 2)) {
                    val index = row * width + column
                    if (!known[index]) continue
                    val color = pixels[index]
                    red += Color.red(color)
                    green += Color.green(color)
                    blue += Color.blue(color)
                    count++
                }
            }
            val index = y * halfWidth + x
            if (count > 0) {
                coarse[index] = Color.rgb(red / count, green / count, blue / count)
                coarseKnown[index] = true
            }
        }
    }
    fillFromSurroundings(coarse, coarseKnown, halfWidth, halfHeight)
    val fine = Bitmap.createBitmap(coarse, halfWidth, halfHeight, Bitmap.Config.ARGB_8888)
    val interpolated = Bitmap.createScaledBitmap(fine, width, height, true)
    val colors = interpolated.readPixels()
    if (interpolated !== fine) interpolated.recycle()
    fine.recycle()
    for (index in pixels.indices) {
        if (!known[index]) pixels[index] = colors[index]
    }
}
