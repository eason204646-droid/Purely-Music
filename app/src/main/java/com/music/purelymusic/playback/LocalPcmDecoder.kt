// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.SystemClock
import java.nio.ByteOrder
import kotlin.math.roundToLong

/** Streaming local decoder. Blocks retain source timestamps and seek preroll. */
internal class LocalPcmDecoder(private val context: Context) {
    data class Block(val timeUs: Long, val sampleRate: Int, val samples: FloatArray) {
        val frames get() = samples.size / 2
        val sourceFrame get() = (timeUs * sampleRate / 1_000_000.0).roundToLong()
    }

    fun decode(uri: Uri, startMs: Long, endMs: Long, check: () -> Unit, consume: (Block) -> Unit) {
        require(uri.scheme != "http" && uri.scheme != "https")
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            if (uri.scheme == null) extractor.setDataSource(uri.toString()) else extractor.setDataSource(context, uri, null)
            val track = (0 until extractor.trackCount).first {
                extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
            }
            extractor.selectTrack(track)
            val format = extractor.getTrackFormat(track)
            var rate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            require(channels in 1..2) { "AutoMix requires mono or stereo audio" }
            var encoding = AudioFormat.ENCODING_PCM_16BIT
            val decoder = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME)!!)
            codec = decoder
            decoder.configure(format, null, null, 0)
            decoder.start()
            extractor.seekTo(startMs * 1_000L, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            val info = MediaCodec.BufferInfo()
            val deadline = SystemClock.elapsedRealtime() + 30_000L
            var inputEnded = false
            var outputEnded = false
            while (!outputEnded) {
                check()
                check(SystemClock.elapsedRealtime() < deadline) { "AutoMix decode deadline exceeded" }
                if (!inputEnded) {
                    val index = decoder.dequeueInputBuffer(1_000L)
                    if (index >= 0) {
                        val input = decoder.getInputBuffer(index)!!
                        input.clear()
                        val size = extractor.readSampleData(input, 0)
                        if (size < 0 || extractor.sampleTime > endMs * 1_000L + 100_000L) {
                            decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputEnded = true
                        } else {
                            decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val index = decoder.dequeueOutputBuffer(info, 1_000L)
                when {
                    index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                        rate = decoder.outputFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                        channels = decoder.outputFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                        require(channels in 1..2 && rate in 8_000..192_000)
                        if (decoder.outputFormat.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                            encoding = decoder.outputFormat.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        }
                        require(encoding == AudioFormat.ENCODING_PCM_16BIT || encoding == AudioFormat.ENCODING_PCM_FLOAT)
                    }
                    index >= 0 -> {
                        try {
                            val output = decoder.getOutputBuffer(index)?.duplicate()?.order(ByteOrder.LITTLE_ENDIAN)
                            if (output != null && info.size > 0 && info.presentationTimeUs < endMs * 1_000L) {
                                val bytes = if (encoding == AudioFormat.ENCODING_PCM_FLOAT) 4 else 2
                                val frames = info.size / (bytes * channels)
                                val samples = FloatArray(frames * 2)
                                for (frame in 0 until frames) for (channel in 0..1) {
                                    val offset = info.offset + (frame * channels + minOf(channel, channels - 1)) * bytes
                                    val value = if (bytes == 4) output.getFloat(offset) else output.getShort(offset) / 32768f
                                    samples[frame * 2 + channel] = if (value.isFinite()) value.coerceIn(-1f, 1f) else 0f
                                }
                                consume(Block(info.presentationTimeUs, rate, samples))
                            }
                        } finally { decoder.releaseOutputBuffer(index, false) }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0 ||
                            info.presentationTimeUs >= endMs * 1_000L
                    }
                }
            }
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    fun window(uri: Uri, startMs: Long, lengthMs: Long, rate: Int, check: () -> Unit): FloatArray {
        val firstFrame = startMs * rate / 1_000L
        val frames = (lengthMs * rate / 1_000L).toInt()
        val result = FloatArray(frames * 2)
        val covered = BooleanArray(frames)
        var decodedFrames = 0
        decode(uri, (startMs - 100L).coerceAtLeast(0), startMs + lengthMs + 20L, check) { block ->
            val from = maxOf(firstFrame, (block.sourceFrame.toDouble() * rate / block.sampleRate).roundToLong())
            val until = minOf(firstFrame + frames, ((block.sourceFrame + block.frames).toDouble() * rate / block.sampleRate).roundToLong())
            for (frame in from until until) {
                val position = (frame.toDouble() * block.sampleRate / rate - block.sourceFrame).coerceAtLeast(0.0)
                val index = position.toInt().coerceAtMost(block.frames - 1)
                val next = minOf(index + 1, block.frames - 1)
                val fraction = (position - index).toFloat().coerceIn(0f, 1f)
                for (channel in 0..1) {
                    val a = block.samples[index * 2 + channel]
                    result[(frame - firstFrame).toInt() * 2 + channel] = a + (block.samples[next * 2 + channel] - a) * fraction
                }
                val localFrame = (frame - firstFrame).toInt()
                if (!covered[localFrame]) { covered[localFrame] = true; decodedFrames++ }
            }
        }
        require(decodedFrames >= frames - rate / 200) { "Incomplete AutoMix passage" }
        return result
    }
}
