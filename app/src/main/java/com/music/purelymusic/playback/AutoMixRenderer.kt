// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.playback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlinx.coroutines.CancellationException

/** The temporary file has the incoming track's native timeline, metadata and untouched PCM tail. */
@androidx.annotation.OptIn(UnstableApi::class)
internal data class RenderedMix(val plan: TransitionPlan, val file: File, val original: MediaItem, val handoverBeforeMixMs: Long) {
    fun mediaItem(): MediaItem = original.buildUpon().setUri(Uri.fromFile(file))
        .setMimeType("audio/wav").setCustomCacheKey(null).setTag(this).build()
}

@androidx.annotation.OptIn(UnstableApi::class)
internal class AutoMixRenderer(private val context: Context) {
    private val decoder = LocalPcmDecoder(context)
    private val directory = File(context.cacheDir, "automix-pcm").apply { mkdirs() }

    fun clearStaleFiles() { directory.listFiles()?.forEach { it.delete() } }

    fun render(outgoing: MediaItem, incoming: MediaItem, analysis: TrackAnalysis, plan: TransitionPlan, check: () -> Unit): RenderedMix? {
        val outUri = outgoing.localConfiguration?.uri ?: return null
        val inUri = incoming.localConfiguration?.uri ?: return null
        // Bound disk/CPU use for podcasts, live streams and unusually long recordings.
        if (analysis.durationMs > 600_000L || plan.incomingStartMs < plan.prerollMs) return null
        val rate = 48_000
        val heap = Runtime.getRuntime()
        val availableMemory = heap.maxMemory() - heap.totalMemory() + heap.freeMemory()
        val workingMemory = plan.fadeMs * rate * 2 * 4 * 11 / 1_000L + 4_000_000L
        if (availableMemory < workingMemory) return null
        val file = try {
            File.createTempFile("mix-", ".wav", directory)
        } catch (error: IOException) {
            Log.w("AutoMixRenderer", "AutoMix cache unavailable", error)
            return null
        }
        var complete = false
        try {
            val frames = (plan.fadeMs * rate / 1_000L).toInt()
            val guardFrames = (plan.prerollMs * rate / 1_000L).toInt()
            val pre = decoder.window(outUri, plan.startMs - plan.prerollMs, plan.prerollMs, rate, check)
            val source = decoder.window(outUri, plan.outgoingSourceMs, ceil(plan.fadeMs * plan.outgoingSpeed.toDouble()).toLong() + 100, rate, check)
            val stretched = stretch(source, rate, plan.outgoingSpeed, frames, check)
            // Join the original outgoing waveform into the stretched segment over 12 ms.
            val nativeHead = decoder.window(outUri, plan.startMs, 20L, rate, check)
            val joinFrames = minOf(rate * 12 / 1_000, frames)
            for (frame in 0 until joinFrames) {
                val t = frame.toFloat() / joinFrames
                for (channel in 0..1) {
                    val i = frame * 2 + channel
                    stretched[i] = nativeHead[i] * (1 - t) + stretched[i] * t
                }
            }
            val next = decoder.window(inUri, plan.incomingStartMs, plan.fadeMs, rate, check)
            val mixed = DjPcmMixer.render(stretched, next, rate, plan, check)
            check()
            val startFrame = plan.incomingStartMs * rate / 1_000L
            val firstGuardFrame = startFrame - guardFrames
            val totalFrames = analysis.durationMs * rate / 1_000L
            var written = 0L
            BufferedOutputStream(file.outputStream(), 128 * 1_024).use { output ->
                writeHeader(output, totalFrames, rate)
                val bytes = ByteArray(4 * 4_096)
                var byteCount = 0
                fun emit(left: Float, right: Float) {
                    for (channel in 0..1) {
                        val sample = if (channel == 0) left else right
                        val pcm = (sample * 32768f).roundToInt().coerceIn(-32768, 32767)
                        bytes[byteCount++] = pcm.toByte()
                        bytes[byteCount++] = (pcm shr 8).toByte()
                    }
                    if (byteCount == bytes.size) { output.write(bytes); byteCount = 0 }
                    written++
                }
                fun writeFrame(frame: Long, left: Float, right: Float) {
                    when {
                        frame in firstGuardFrame until startFrame -> {
                            val i = (frame - firstGuardFrame).toInt() * 2
                            emit(pre[i], pre[i + 1])
                        }
                        frame in startFrame until startFrame + frames -> {
                            val i = (frame - startFrame).toInt() * 2
                            emit(mixed[i], mixed[i + 1])
                        }
                        else -> emit(left, right)
                    }
                }
                decoder.decode(inUri, 0, analysis.durationMs, check) { block ->
                    val blockEnd = minOf(totalFrames, ((block.sourceFrame + block.frames).toDouble() * rate / block.sampleRate).roundToLong())
                    val first = (block.sourceFrame.toDouble() * rate / block.sampleRate).roundToLong().coerceAtLeast(0)
                    require(first - written <= rate / 5) { "Discontinuous incoming PCM" }
                    while (written < minOf(first, totalFrames)) writeFrame(written, 0f, 0f)
                    while (written < blockEnd) {
                        val position = (written.toDouble() * block.sampleRate / rate - block.sourceFrame).coerceAtLeast(0.0)
                        val index = position.toInt().coerceAtMost(block.frames - 1)
                        val nextIndex = minOf(index + 1, block.frames - 1)
                        val t = (position - index).toFloat().coerceIn(0f, 1f)
                        val l = block.samples[index * 2]
                        val r = block.samples[index * 2 + 1]
                        writeFrame(written, l + (block.samples[nextIndex * 2] - l) * t, r + (block.samples[nextIndex * 2 + 1] - r) * t)
                    }
                }
                require(totalFrames - written <= rate / 5) { "Incomplete incoming PCM" }
                while (written < totalFrames) writeFrame(written, 0f, 0f)
                if (byteCount > 0) output.write(bytes, 0, byteCount)
            }
            check()
            complete = true
            return RenderedMix(plan, file, incoming, DjPcmMixer.quietHandoverBeforeEndMs(pre, rate))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Log.w("AutoMixRenderer", "Could not prepare a joint PCM mix", error)
            return null
        } finally {
            if (!complete) file.delete()
        }
    }

    private fun stretch(source: FloatArray, rate: Int, speed: Float, frames: Int, check: () -> Unit): FloatArray {
        if (abs(speed - 1f) < 0.0001f) return source.copyOf(frames * 2)
        val processor = SonicAudioProcessor()
        try {
            processor.setSpeed(speed)
            processor.setPitch(1f)
            processor.configure(AudioProcessor.AudioFormat(rate, 2, C.ENCODING_PCM_16BIT))
            processor.flush()
            val result = FloatArray(frames * 2)
            var written = 0
            fun drain() {
                val output = processor.output.order(ByteOrder.nativeOrder())
                while (output.hasRemaining()) {
                    val sample = output.short / 32768f
                    if (written < result.size) result[written++] = sample
                }
            }
            val input = ByteBuffer.allocateDirect(8_192).order(ByteOrder.nativeOrder())
            var offset = 0
            while (offset < source.size) {
                check()
                input.clear()
                val count = minOf(input.capacity() / 2, source.size - offset)
                repeat(count) { input.putShort((source[offset++] * 32768f).roundToInt().coerceIn(-32768, 32767).toShort()) }
                input.flip()
                processor.queueInput(input)
                drain()
            }
            processor.queueEndOfStream()
            while (!processor.isEnded) { check(); drain() }
            require(written == result.size) { "Incomplete tempo-matched passage" }
            return result
        } finally { processor.reset() }
    }

    private fun writeHeader(output: BufferedOutputStream, frames: Long, rate: Int) {
        val header = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        fun ascii(text: String) { header.put(text.toByteArray(Charsets.US_ASCII)) }
        ascii("RIFF"); header.putInt((36 + frames * 4).toInt()); ascii("WAVEfmt ")
        header.putInt(16); header.putShort(1); header.putShort(2); header.putInt(rate)
        header.putInt(rate * 4); header.putShort(4); header.putShort(16)
        ascii("data"); header.putInt((frames * 4).toInt())
        output.write(header.array())
    }
}
