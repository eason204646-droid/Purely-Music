// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt
import kotlinx.coroutines.CancellationException

/** Decodes two short windows for silence and beat analysis; never changes the audio file. */
internal class LocalTrackAnalyzer(private val context: Context) {
    private data class WindowData(val points: List<EnergyFrame>, val chroma: List<Float>?)
    private class EnergyBin {
        var power = 0.0
        var bassPower = 0.0
        var count = 0
    }
    private class PcmSamples(maxSamples: Int, val sampleRate: Float) {
        val values = FloatArray(maxSamples)
        var size = 0
        fun add(value: Float) {
            if (size < values.size) values[size++] = value
        }
    }

    fun analyze(uri: Uri, checkCancellation: () -> Unit = {}): TrackAnalysis? {
        if (uri.scheme == "http" || uri.scheme == "https") return null
        val extractor = MediaExtractor()
        try {
            if (uri.scheme == null) extractor.setDataSource(uri.toString())
            else extractor.setDataSource(context, uri, null)

            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            } ?: return null
            val format = extractor.getTrackFormat(track)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
            val durationUs = format.getLong(MediaFormat.KEY_DURATION)
            if (durationUs < 8_000_000L) return null
            extractor.selectTrack(track)

            val windowUs = minOf(WINDOW_US, durationUs / 2)
            val first = decodeWindow(extractor, format, mime, 0L, windowUs, checkCancellation)
            // Even short songs need a separate ending: their tempo/harmony can differ from the intro.
            val last = decodeWindow(extractor, format, mime, durationUs - windowUs, durationUs, checkCancellation)
            if (first.points.isEmpty() || last.points.isEmpty()) return null

            val firstThreshold = maxOf(0.004f, first.points.maxOf { it.energy } * 0.035f)
            val lastThreshold = maxOf(0.004f, last.points.maxOf { it.energy } * 0.035f)
            val firstAudible = SilenceDetector.firstAudible(first.points, firstThreshold) ?: return null
            val lastAudible = SilenceDetector.lastAudible(last.points, lastThreshold)
                ?.plus(BIN_MS) ?: return null
            val openingBeat = BeatAnalyzer.estimate(first.points)
            val closingBeat = BeatAnalyzer.estimate(last.points)
            val openingRegion = first.points.filter { it.timeMs in firstAudible..(firstAudible + 8_000L) }
            val closingRegion = last.points.filter { it.timeMs in (lastAudible - 10_000L)..lastAudible }
            return TrackAnalysis(
                durationMs = durationUs / 1_000L,
                firstAudibleMs = (firstAudible - 80L).coerceAtLeast(0L),
                lastAudibleMs = lastAudible.coerceAtMost(durationUs / 1_000L),
                bpm = openingBeat?.bpm,
                firstBeatMs = openingBeat?.anchorMs,
                averageEnergy = rms(openingRegion),
                openingChroma = first.chroma,
                closingChroma = last.chroma,
                closingBpm = closingBeat?.bpm,
                closingBeatMs = closingBeat?.anchorMs,
                earlyExitMs = TransitionOpportunities.earlyExit(last.points, durationUs / 1_000L),
                strongEntryMs = TransitionOpportunities.strongEntry(first.points),
                beatConfidence = openingBeat?.confidence ?: 0f,
                closingBeatConfidence = closingBeat?.confidence ?: 0f,
                openingPhraseMs = openingBeat?.phraseAnchorMs,
                closingPhraseMs = closingBeat?.phraseAnchorMs,
                closingEnergy = rms(closingRegion),
                openingDensity = density(openingRegion),
                closingDensity = density(closingRegion),
                openingSections = sections(first.points),
                closingSections = sections(last.points)
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // Leave unsupported/damaged tracks on their original playback path.
            return null
        } finally {
            extractor.release()
        }
    }

    private fun decodeWindow(
        extractor: MediaExtractor,
        format: MediaFormat,
        mime: String,
        startUs: Long,
        endUs: Long,
        checkCancellation: () -> Unit
    ): WindowData {
        val codec = MediaCodec.createDecoderByType(mime)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val bins = HashMap<Long, EnergyBin>()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var samples = PcmSamples(sampleRate * 3, sampleRate / 4f)
            var bassState = FloatArray(channels)
            var iterations = 0
            val deadlineMs = SystemClock.elapsedRealtime() + 8_000L

            while (!outputEnded && iterations++ < 10_000 &&
                SystemClock.elapsedRealtime() < deadlineMs) {
                checkCancellation()
                if (!inputEnded) {
                    val index = codec.dequeueInputBuffer(10_000L)
                    if (index >= 0) {
                        val input = codec.getInputBuffer(index) ?: break
                        input.clear()
                        val size = extractor.readSampleData(input, 0)
                        val sampleUs = extractor.sampleTime
                        if (size < 0 || sampleUs > endUs) {
                            codec.queueInputBuffer(index, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            codec.queueInputBuffer(index, 0, size, sampleUs, 0)
                            extractor.advance()
                        }
                    }
                }

                val index = codec.dequeueOutputBuffer(info, 10_000L)
                when (index) {
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        val outputFormat = codec.outputFormat
                        sampleRate = outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        if (samples.size == 0) samples = PcmSamples(sampleRate * 3, sampleRate / 4f)
                        bassState = FloatArray(channels)
                        if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            encoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    in 0..Int.MAX_VALUE -> {
                        val output = codec.getOutputBuffer(index)
                        if (output != null && info.size > 0 && sampleRate > 0 && channels > 0) {
                            accumulate(
                                output, info, sampleRate, channels, encoding,
                                startUs, endUs, bins, samples, bassState
                            )
                        }
                        codec.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEnded = true
                    }
                }
            }
            if (!outputEnded) return WindowData(emptyList(), null)
            var previousEnergy = 0f
            var previousBass = 0f
            var previousTime = Long.MIN_VALUE
            val points = bins.keys.sorted().map { bucket ->
                val bin = bins.getValue(bucket)
                val energy = sqrt(bin.power / bin.count).toFloat()
                val bass = sqrt(bin.bassPower / bin.count).toFloat()
                val timeMs = bucket * BIN_MS
                val onset = if (timeMs - previousTime == BIN_MS) {
                    (energy - previousEnergy).coerceAtLeast(0f) * 0.4f +
                        (bass - previousBass).coerceAtLeast(0f) * 0.6f
                } else 0f
                previousEnergy = energy
                previousBass = bass
                previousTime = timeMs
                EnergyFrame(timeMs, energy, onset, bass)
            }
            return WindowData(
                points,
                PitchClassAnalyzer.analyze(samples.values, samples.size, samples.sampleRate)
            )
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
    }

    private fun accumulate(
        output: ByteBuffer,
        info: MediaCodec.BufferInfo,
        sampleRate: Int,
        channels: Int,
        encoding: Int,
        startUs: Long,
        endUs: Long,
        bins: MutableMap<Long, EnergyBin>,
        samples: PcmSamples,
        bassState: FloatArray
    ) {
        if (encoding != AudioFormat.ENCODING_PCM_16BIT &&
            encoding != AudioFormat.ENCODING_PCM_FLOAT) return
        val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = bytesPerSample * channels
        val frames = info.size / frameBytes
        val pcm = output.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        val bassAlpha = (1.0 - exp(-2.0 * PI * 180.0 / (sampleRate / 4.0))).toFloat()
        for (frame in 0 until frames step 4) {
            val timeUs = info.presentationTimeUs + frame * 1_000_000L / sampleRate
            if (timeUs < startUs || timeUs >= endUs) continue
            val offset = info.offset + frame * frameBytes
            if (offset + frameBytes > pcm.limit()) break
            var power = 0.0
            var bassPower = 0.0
            var chromaSample = 0f
            for (channel in 0 until channels) {
                val sampleOffset = offset + channel * bytesPerSample
                val raw = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) {
                    pcm.getFloat(sampleOffset)
                } else {
                    pcm.getShort(sampleOffset) / 32768f
                }
                val sample = if (raw.isFinite()) raw.coerceIn(-1f, 1f) else 0f
                power += sample * sample
                bassState[channel] += bassAlpha * (sample - bassState[channel])
                bassPower += bassState[channel] * bassState[channel]
                // One consistent channel avoids the distortion of switching to the loudest one.
                if (channel == 0) chromaSample = sample
            }
            val bucket = timeUs / (BIN_MS * 1_000L)
            val bin = bins.getOrPut(bucket) { EnergyBin() }
            bin.power += power / channels
            bin.bassPower += bassPower / channels
            bin.count++
            // Analyze the actual ending's harmony, rather than the start of its decode window.
            if (startUs == 0L || timeUs >= endUs - 12_000_000L) samples.add(chromaSample)
        }
    }

    private fun rms(frames: List<EnergyFrame>): Float = if (frames.isEmpty()) 0f else
        sqrt(frames.map { (it.energy * it.energy).toDouble() }.average()).toFloat()

    private fun density(frames: List<EnergyFrame>): Float {
        if (frames.isEmpty()) return 0f
        val sorted = frames.map { it.energy }.sorted()
        val reference = sorted[(sorted.lastIndex * 0.9).toInt()]
        if (reference < 0.004f) return 0f
        return (frames.map { it.energy }.average() / reference).toFloat().coerceIn(0f, 1f)
    }

    private fun sections(frames: List<EnergyFrame>): List<MixSection> = frames.groupBy { it.timeMs / 2_000L }.map { (bin, points) ->
        val power = points.sumOf { (it.energy * it.energy).toDouble() }
        val bass = points.sumOf { (it.bass * it.bass).toDouble() }
        MixSection(bin * 2_000L, rms(points), (bass / power.coerceAtLeast(0.00001)).toFloat().coerceIn(0f, 1f), density(points))
    }.sortedBy { it.startMs }

    companion object {
        private const val BIN_MS = 10L
        private const val WINDOW_US = 48_000_000L
    }
}
