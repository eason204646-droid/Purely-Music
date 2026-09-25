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
import kotlin.math.abs
import kotlin.math.sqrt

/** Decodes two short windows for silence and beat analysis; never changes the audio file. */
internal class LocalTrackAnalyzer(private val context: Context) {
    private data class WindowData(val points: List<EnergyFrame>, val chroma: List<Float>?)
    private class PcmSamples(maxSamples: Int, val sampleRate: Float) {
        val values = FloatArray(maxSamples)
        var size = 0
        fun add(value: Float) {
            if (size < values.size) values[size++] = value
        }
    }

    fun analyze(uri: Uri): TrackAnalysis? {
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

            val first = decodeWindow(extractor, format, mime, 0L, minOf(durationUs, 20_000_000L))
            val last = if (durationUs > 25_000_000L) {
                decodeWindow(extractor, format, mime, durationUs - 20_000_000L, durationUs)
            } else first
            if (first.points.isEmpty() || last.points.isEmpty()) return null

            val firstThreshold = maxOf(0.004f, first.points.maxOf { it.energy } * 0.035f)
            val lastThreshold = maxOf(0.004f, last.points.maxOf { it.energy } * 0.035f)
            val firstAudible = SilenceDetector.firstAudible(first.points, firstThreshold) ?: 0L
            val lastAudible = SilenceDetector.lastAudible(last.points, lastThreshold)
                ?.plus(BIN_MS) ?: durationUs / 1_000L
            val openingBeat = BeatAnalyzer.estimate(first.points)
            val closingBeat = BeatAnalyzer.estimate(last.points)
            return TrackAnalysis(
                durationMs = durationUs / 1_000L,
                firstAudibleMs = (firstAudible - 80L).coerceAtLeast(0L),
                lastAudibleMs = lastAudible.coerceAtMost(durationUs / 1_000L),
                bpm = openingBeat?.bpm,
                firstBeatMs = openingBeat?.anchorMs,
                averageEnergy = first.points.map { it.energy }.average().toFloat(),
                openingChroma = first.chroma,
                closingChroma = last.chroma,
                closingBpm = closingBeat?.bpm,
                closingBeatMs = closingBeat?.anchorMs
            )
        } catch (_: Exception) {
            // Unsupported local codecs and damaged files use the ordinary fade plan.
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
        endUs: Long
    ): WindowData {
        val codec = MediaCodec.createDecoderByType(mime)
        try {
            codec.configure(format, null, null, 0)
            codec.start()
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val sums = HashMap<Long, Double>()
            val counts = HashMap<Long, Int>()
            val info = MediaCodec.BufferInfo()
            var inputEnded = false
            var outputEnded = false
            var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            var samples = PcmSamples(sampleRate * 6, sampleRate / 4f)
            var iterations = 0
            val deadlineMs = SystemClock.elapsedRealtime() + 8_000L

            while (!outputEnded && iterations++ < 5_000 &&
                SystemClock.elapsedRealtime() < deadlineMs) {
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
                        if (samples.size == 0) samples = PcmSamples(sampleRate * 6, sampleRate / 4f)
                        if (outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            encoding = outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                    }
                    in 0..Int.MAX_VALUE -> {
                        val output = codec.getOutputBuffer(index)
                        if (output != null && info.size > 0 && sampleRate > 0 && channels > 0) {
                            accumulate(
                                output, info, sampleRate, channels, encoding,
                                startUs, endUs, sums, counts, samples
                            )
                        }
                        codec.releaseOutputBuffer(index, false)
                        if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputEnded = true
                    }
                }
            }
            if (!outputEnded) return WindowData(emptyList(), null)
            val points = sums.keys.sorted().mapNotNull { bucket ->
                val count = counts[bucket] ?: return@mapNotNull null
                EnergyFrame(bucket * BIN_MS, sqrt(sums.getValue(bucket) / count).toFloat())
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
        sums: MutableMap<Long, Double>,
        counts: MutableMap<Long, Int>,
        samples: PcmSamples
    ) {
        if (encoding != AudioFormat.ENCODING_PCM_16BIT &&
            encoding != AudioFormat.ENCODING_PCM_FLOAT) return
        val bytesPerSample = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
        val frameBytes = bytesPerSample * channels
        val frames = info.size / frameBytes
        val pcm = output.duplicate().order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frames step 4) {
            val timeUs = info.presentationTimeUs + frame * 1_000_000L / sampleRate
            if (timeUs < startUs || timeUs >= endUs) continue
            val offset = info.offset + frame * frameBytes
            if (offset + frameBytes > pcm.limit()) break
            var power = 0.0
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
                if (abs(sample) > abs(chromaSample)) chromaSample = sample
            }
            val bucket = timeUs / (BIN_MS * 1_000L)
            sums[bucket] = (sums[bucket] ?: 0.0) + power / channels
            counts[bucket] = (counts[bucket] ?: 0) + 1
            samples.add(chromaSample)
        }
    }

    companion object {
        private const val BIN_MS = 40L
    }
}
