package com.music.purelymusic.playback

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.SystemClock
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.BufferedOutputStream
import java.io.RandomAccessFile
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.math.round
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Real Android codecs, rendered PCM and playback lifecycle using disposable generated audio. */
@RunWith(AndroidJUnit4::class)
@androidx.annotation.OptIn(UnstableApi::class)
class AutoMixPlaybackTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context = instrumentation.targetContext
    private val files = mutableListOf<File>()
    private var runtime: PlaybackRuntime? = null

    private fun <T> main(block: () -> T): T {
        val result = AtomicReference<T>()
        instrumentation.runOnMainSync { result.set(block()) }
        return result.get()
    }

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean) {
        val deadline = SystemClock.elapsedRealtime() + timeoutMs
        while (!condition() && SystemClock.elapsedRealtime() < deadline) Thread.sleep(20L)
        assertTrue("Timed out after $timeoutMs ms", condition())
    }

    private fun wave(name: String, seconds: Int = 40, bpm: Double = 123.4, sampleRate: Int = 48_000, closingBpm: Double = bpm): File {
        val file = File(context.cacheDir, "automix-test-$name.wav").also { files += it }
        val sampleCount = sampleRate * seconds
        BufferedOutputStream(file.outputStream()).use { stream ->
            fun ascii(text: String) { stream.write(text.toByteArray(Charsets.US_ASCII)) }
            fun integer(value: Int, bytes: Int = 4) {
                repeat(bytes) { stream.write((value ushr (it * 8)) and 255) }
            }
            ascii("RIFF"); integer(36 + sampleCount * 2); ascii("WAVEfmt "); integer(16)
            integer(1, 2); integer(1, 2); integer(sampleRate); integer(sampleRate * 2)
            integer(2, 2); integer(16, 2); ascii("data"); integer(sampleCount * 2)
            for (index in 0 until sampleCount) {
                val time = index.toDouble() / sampleRate
                val tempo = if (time < seconds / 2.0) bpm else closingBpm
                val phase = ((time - 1.13) % (60.0 / tempo) + 60.0 / tempo) % (60.0 / tempo)
                val sample = if (time < 1.0 || time >= seconds - 2.0) 0.0 else {
                    0.035 * sin(2.0 * PI * 330.0 * time) +
                        0.5 * exp(-phase * 38.0) * sin(2.0 * PI * 65.0 * phase)
                }
                integer((sample * 32_767).toInt(), 2)
            }
        }
        return file
    }

    private fun item(file: File, album: Boolean = false) = MediaItem.Builder()
        .setMediaId(file.name)
        .setUri(Uri.fromFile(file))
        .setMediaMetadata(MediaMetadata.Builder().apply {
            if (album) { setAlbumTitle("Continuous test album"); setArtist("Test artist") }
        }.build())
        .build()

    private fun start(first: File, second: File, album: Boolean = false): PlaybackRuntime = main {
        PlaybackRuntime.get(context).also {
            runtime = it
            it.configureCrossfade(false, 3)
            it.configureAutoMix(true)
            it.player.repeatMode = Player.REPEAT_MODE_OFF
            it.player.setMediaItems(listOf(item(first, album), item(second, album)))
            it.player.prepare()
            it.player.play()
        }
    }

    @After fun cleanup() {
        main { runtime?.release() }
        files.forEach { it.delete() }
    }

    @Test fun decodesFractionalTempoAndRealAudioBoundaries() {
        val file = wave("analysis")
        val analysis = LocalTrackAnalyzer(context).analyze(Uri.fromFile(file))
        assertNotNull(analysis)
        assertEquals(40_000L, analysis!!.durationMs)
        assertTrue(analysis.firstAudibleMs in 850L..1_100L)
        assertTrue(analysis.lastAudibleMs in 37_800L..38_100L)
        assertNotNull(analysis.bpm)
        assertEquals(123.4f, analysis.bpm!!, 0.25f)
        assertEquals(123.4f, analysis.closingBpm!!, 0.25f)
        assertTrue(analysis.beatConfidence >= 0.65f)
        assertTrue(analysis.closingBeatConfidence!! >= 0.65f)
    }

    @Test fun analyzesTheActualEndingOfATwentyFourSecondTrack() {
        val analysis = LocalTrackAnalyzer(context).analyze(Uri.fromFile(wave("short", 24)))!!
        assertTrue(analysis.lastAudibleMs in 21_800L..22_100L)
    }

    @Test fun shortTracksAnalyzeTheirActualOutroInsteadOfReusingIntroTempo() {
        val analysis = LocalTrackAnalyzer(context).analyze(Uri.fromFile(wave("changing-tempo", bpm = 100.0, closingBpm = 124.0)))!!
        assertEquals(100f, analysis.bpm!!, 0.25f)
        assertEquals(124f, analysis.closingBpm!!, 0.25f)
    }

    private fun aac(source: File, rate: Int): File {
        val file = File(context.cacheDir, "automix-test-compressed.m4a").also { files += it }
        val codec = MediaCodec.createEncoderByType("audio/mp4a-latm")
        val muxer = MediaMuxer(file.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        var muxerStarted = false
        try {
            val format = MediaFormat.createAudioFormat("audio/mp4a-latm", rate, 1).apply {
                setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            }
            codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            codec.start()
            RandomAccessFile(source, "r").use { input ->
                input.seek(44)
                val bytes = ByteArray(4_096)
                val info = MediaCodec.BufferInfo()
                var frames = 0L
                var inputEnded = false
                var outputEnded = false
                var track = -1
                val deadline = SystemClock.elapsedRealtime() + 15_000L
                while (!outputEnded) {
                    check(SystemClock.elapsedRealtime() < deadline)
                    if (!inputEnded) {
                        val index = codec.dequeueInputBuffer(1_000)
                        if (index >= 0) {
                            val buffer = codec.getInputBuffer(index)!!
                            buffer.clear()
                            val size = input.read(bytes, 0, minOf(bytes.size, buffer.remaining()))
                            if (size < 0) {
                                codec.queueInputBuffer(index, 0, 0, frames * 1_000_000L / rate, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputEnded = true
                            } else {
                                buffer.put(bytes, 0, size)
                                codec.queueInputBuffer(index, 0, size, frames * 1_000_000L / rate, 0)
                                frames += size / 2
                            }
                        }
                    }
                    val index = codec.dequeueOutputBuffer(info, 1_000)
                    if (index == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        track = muxer.addTrack(codec.outputFormat)
                        muxer.start()
                        muxerStarted = true
                    } else if (index >= 0) {
                        if (info.size > 0 && info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG == 0) {
                            muxer.writeSampleData(track, codec.getOutputBuffer(index)!!, info)
                        }
                        outputEnded = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                        codec.releaseOutputBuffer(index, false)
                    }
                }
            }
        } finally {
            if (muxerStarted) muxer.stop()
            muxer.release()
            codec.stop()
            codec.release()
        }
        return file
    }

    @Test fun rendersMatchedBeatsAndPreservesTheUnmixedIncomingSamples() {
        val outgoing = wave("render-out", bpm = 123.4)
        val incoming = wave("render-in", bpm = 120.7)
        val analyzer = LocalTrackAnalyzer(context)
        val a = analyzer.analyze(Uri.fromFile(outgoing))!!
        val b = analyzer.analyze(Uri.fromFile(incoming))!!
        val plan = AutoMixPlanner.plan(a.durationMs, a.copy(closingDensity = 0.4f), b.copy(openingDensity = 0.4f))!!
        assertEquals(TransitionStyle.BEAT_MIX, plan.style)
        val rendered = AutoMixRenderer(context).render(item(outgoing), item(incoming), b, plan) {}!!
        files += rendered.file
        assertEquals(44 + 40_000L * 48 * 4, rendered.file.length())
        val decoder = LocalPcmDecoder(context)
        val mixed = decoder.window(Uri.fromFile(rendered.file), plan.incomingStartMs, plan.fadeMs, 48_000) {}
        val measured = mutableListOf<EnergyFrame>()
        // Bass still belongs to the stretched outgoing passage in this interval.
        val measureMs = (plan.fadeMs * plan.handoffFraction - 1_000).toLong()
        for (ms in 0L until measureMs step 10L) {
            val first = (ms * 48).toInt()
            var power = 0.0
            for (frame in first until first + 480) power += mixed[frame * 2] * mixed[frame * 2]
            measured += EnergyFrame(ms, sqrt(power / 480).toFloat())
        }
        val estimate = BeatAnalyzer.estimate(measured)
        assertNotNull("Outgoing rhythm was lost in the rendered mix", estimate)
        assertEquals(120.7f, estimate!!.bpm, 0.5f)
        val expectedPhase = (b.firstBeatMs!! - plan.incomingStartMs).toDouble()
        val beats = (estimate.anchorMs - expectedPhase) / plan.beatMs
        val errorMs = abs(beats - round(beats)) * plan.beatMs
        assertTrue("Rendered beat phase error: $errorMs ms", errorMs < 35.0)
        fun toneAmplitude(hz: Double): Double {
            var real = 0.0
            var imaginary = 0.0
            for (frame in 48_000 until 96_000) {
                real += mixed[frame * 2] * cos(2 * PI * hz * frame / 48_000)
                imaginary += mixed[frame * 2] * sin(2 * PI * hz * frame / 48_000)
            }
            return 2 * sqrt(real * real + imaginary * imaginary) / 48_000
        }
        // Tempo changes must preserve the outgoing melody's original frequency.
        assertTrue(toneAmplitude(330.0) > 0.02)
        assertTrue(toneAmplitude(330.0 * plan.outgoingSpeed) < 0.006)
        val originalTail = decoder.window(Uri.fromFile(incoming), 30_000, 2_000, 48_000) {}
        val renderedTail = decoder.window(Uri.fromFile(rendered.file), 30_000, 2_000, 48_000) {}
        assertArrayEquals(originalTail, renderedTail, 0f)
        assertTrue(mixed.all { abs(it) <= 0.9801f })
    }

    @Test fun cancelledRenderingRemovesItsTemporaryAudio() {
        val outgoing = wave("cancel-out", 24)
        val incoming = wave("cancel-in", 24)
        val analyzer = LocalTrackAnalyzer(context)
        val a = analyzer.analyze(Uri.fromFile(outgoing))!!
        val b = analyzer.analyze(Uri.fromFile(incoming))!!
        val plan = AutoMixPlanner.plan(a.durationMs, a, b)!!
        val directory = File(context.cacheDir, "automix-pcm")
        val before = directory.listFiles()?.map { it.name }?.toSet() ?: emptySet()
        try {
            AutoMixRenderer(context).render(item(outgoing), item(incoming), b, plan) { throw CancellationException("test") }
            fail("Cancellation was swallowed")
        } catch (_: CancellationException) { }
        assertEquals(before, directory.listFiles()?.map { it.name }?.toSet() ?: emptySet<String>())
    }

    @Test fun rendersCompressedAudioWithDifferentSampleRatesAndPlayableMetadata() {
        val outgoing = wave("format-out", 24)
        val incoming = aac(wave("format-in", 24, sampleRate = 44_100), 44_100)
        val analyzer = LocalTrackAnalyzer(context)
        val a = analyzer.analyze(Uri.fromFile(outgoing))!!
        val b = analyzer.analyze(Uri.fromFile(incoming))!!
        val plan = AutoMixPlanner.plan(a.durationMs, a, b)!!
        val original = item(incoming).buildUpon().setMimeType("audio/mp4").build()
        val rendered = AutoMixRenderer(context).render(item(outgoing), original, b, plan) {}!!
        files += rendered.file
        val decoder = LocalPcmDecoder(context)
        val expected = decoder.window(Uri.fromFile(incoming), 18_000, 1_000, 48_000) {}
        val actual = decoder.window(Uri.fromFile(rendered.file), 18_000, 1_000, 48_000) {}
        assertArrayEquals(expected, actual, 1f / 32768)
        val player = main {
            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                setMediaItem(rendered.mediaItem())
                prepare()
                seekTo(18_000)
                play()
            }
        }
        try {
            waitFor(5_000) { main { player.isPlaying } }
            main {
                assertEquals(original.mediaId, player.currentMediaItem!!.mediaId)
                assertEquals(b.durationMs, player.duration)
            }
        } finally { main { player.release() } }
    }

    @Test fun transitionsThroughOneRenderedStreamAndKeepsNativeIncomingTimeline() {
        val first = wave("outgoing", bpm = 123.4)
        val second = wave("incoming", bpm = 120.7)
        val runtime = start(first, second)
        waitFor(8_000L) { main { runtime.player.isPlaying } }
        // Let analysis finish, then move close to the transition without waiting for a whole track.
        Thread.sleep(2_000L)
        val originalPlayer = main { runtime.player.apply { seekTo(15_000L) } }
        waitFor(20_000L) { main { runtime.isAutoMixTransitioning } }
        main { assertEquals(0f, originalPlayer.volume, 0f) }
        waitFor(22_000L) { main { runtime.player !== originalPlayer } }
        main {
            assertEquals(second.name, runtime.player.currentMediaItem!!.mediaId)
            assertEquals(1f, runtime.player.volume, 0.0001f)
            assertEquals(1f, runtime.player.playbackParameters.pitch, 0f)
            assertEquals(1f, runtime.player.playbackParameters.speed, 0f)
            assertEquals(40_000L, runtime.player.duration)
            assertEquals(2, runtime.player.mediaItemCount)
            assertTrue(runtime.player.currentMediaItem!!.localConfiguration!!.tag is RenderedMix)
            assertTrue(runtime.player.currentPosition > 2_000L)
        }
        main { runtime.player.seekTo(5_000L) }
        waitFor(2_000L) { main { runtime.player.currentMediaItem!!.localConfiguration!!.uri == Uri.fromFile(second) } }
        main { assertTrue(abs(runtime.player.currentPosition - 5_000L) < 1_000L) }
        // Restoring a rendered item must preserve the queue, repeat mode and seek destination.
        main {
            runtime.player.repeatMode = Player.REPEAT_MODE_ALL
            runtime.player.seekTo(0, 5_000)
            assertEquals(Uri.fromFile(second), runtime.player.getMediaItemAt(1).localConfiguration!!.uri)
            assertEquals(Player.REPEAT_MODE_ALL, runtime.player.repeatMode)
            assertEquals(2, runtime.player.mediaItemCount)
        }
    }

    @Test fun pausingDuringMixRestoresOriginalDeck() {
        val first = wave("pause-out")
        val second = wave("pause-in")
        val runtime = start(first, second)
        waitFor(8_000L) { main { runtime.player.isPlaying } }
        Thread.sleep(2_000L)
        val originalPlayer = main { runtime.player.apply { seekTo(15_000L) } }
        waitFor(20_000L) { main { runtime.isAutoMixTransitioning } }
        main { runtime.player.pause() }
        waitFor(2_000L) { main { !runtime.isAutoMixTransitioning } }
        main {
            assertSame(originalPlayer, runtime.player)
            assertEquals(1f, runtime.player.volume, 0f)
            assertEquals(1f, runtime.player.playbackParameters.speed, 0f)
        }
        waitFor(2_000L) { File(context.cacheDir, "automix-pcm").listFiles().isNullOrEmpty() }
    }

    @Test fun editingQueueKeepsPlaybackAndSeekingRestoresAllNativeQueueReferences() {
        val first = wave("queue-out")
        val second = wave("queue-in")
        val runtime = start(first, second)
        waitFor(8_000L) { main { runtime.player.isPlaying } }
        Thread.sleep(2_000L)
        val oldPlayer = main { runtime.player.apply { seekTo(15_000) } }
        waitFor(38_000L) { main { runtime.player !== oldPlayer } }
        val position = main {
            assertTrue(runtime.player.currentMediaItem!!.localConfiguration!!.tag is RenderedMix)
            val position = runtime.player.currentPosition
            runtime.player.addMediaItem(item(first))
            position
        }
        main {
            assertEquals(3, runtime.player.mediaItemCount)
            assertTrue(runtime.player.currentMediaItem!!.localConfiguration!!.tag is RenderedMix)
            assertTrue(abs(runtime.player.currentPosition - position) < 1_000L)
            assertFalse(runtime.isAutoMixTransitioning)
            runtime.player.seekTo(0, 5_000)
        }
        waitFor(2_000L) { main { runtime.player.getMediaItemAt(1).localConfiguration!!.uri == Uri.fromFile(second) } }
    }

    @Test fun fixedCrossfadeStillUsesItsSeparatePlaybackPath() {
        val first = wave("crossfade-out", 24)
        val second = wave("crossfade-in", 24)
        val runtime = start(first, second)
        main { runtime.configureAutoMix(false); runtime.configureCrossfade(true, 3) }
        waitFor(8_000L) { main { runtime.player.isPlaying } }
        val oldPlayer = main { runtime.player.apply { seekTo(20_000) } }
        waitFor(7_000L) { main { runtime.player !== oldPlayer } }
        main {
            assertFalse(runtime.isAutoMixTransitioning)
            assertEquals(Uri.fromFile(second), runtime.player.currentMediaItem!!.localConfiguration!!.uri)
            assertEquals(1f, runtime.player.volume, 0f)
        }
    }

    @Test fun sequentialAlbumKeepsNaturalTransition() {
        val first = wave("album-out", 24)
        val second = wave("album-in", 24)
        val runtime = start(first, second, album = true)
        waitFor(8_000L) { main { runtime.player.isPlaying } }
        val originalPlayer = main { runtime.player.apply { seekTo(20_000L) } }
        waitFor(7_000L) { main { runtime.player.currentMediaItem?.mediaId == second.name } }
        main {
            assertSame(originalPlayer, runtime.player)
            assertFalse(runtime.isAutoMixTransitioning)
        }
    }
}
