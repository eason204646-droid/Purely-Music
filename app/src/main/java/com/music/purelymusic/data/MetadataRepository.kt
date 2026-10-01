// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.data

import com.google.gson.Gson
import com.music.purelymusic.model.LrcJsonResponse
import com.music.purelymusic.model.MiguDetailResponse
import com.music.purelymusic.model.QqApiResponse
import com.music.purelymusic.model.WyApiResponse
import com.music.purelymusic.utils.NetworkAccess
import com.music.purelymusic.utils.OfflineModeException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.Request
import okhttp3.Response

data class MetadataFetchResult(
    val coverPath: String? = null,
    val lyricPath: String? = null,
    val albumName: String? = null,
    val albumArtist: String? = null,
    val error: String? = null
)

/** 负责联网补全封面、歌词与专辑信息。 */
class MetadataRepository(
    private val apiKey: String,
    private val fileStore: AppFileStore,
    private val gson: Gson = Gson(),
    private val networkAccess: NetworkAccess = NetworkAccess.shared
) {
    private val client = networkAccess.client.newBuilder()
        .connectTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .readTimeout(TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
        .build()

    suspend fun fetch(title: String, artist: String, source: String): MetadataFetchResult =
        withContext(Dispatchers.IO) {
            if (networkAccess.isOffline) return@withContext MetadataFetchResult()
            if (apiKey.isBlank()) {
                return@withContext MetadataFetchResult(error = "未配置 MUSIC_API_KEY")
            }
            val keywords = URLEncoder.encode("$title $artist", Charsets.UTF_8.name())
            runCatching {
                if (source == "mixed") fetchMixed(keywords) else fetchNetease(keywords)
            }.getOrElse { error ->
                if (error is CancellationException) throw error
                if (networkAccess.isOffline) return@getOrElse MetadataFetchResult()
                MetadataFetchResult(error = "获取歌曲信息失败：${error.message ?: error.javaClass.simpleName}")
            }
        }

    private suspend fun fetchNetease(keywords: String): MetadataFetchResult {
        val response = gson.fromJson(
            requestText("https://api.yaohud.cn/api/music/wy?key=$apiKey&msg=$keywords&n=1"),
            WyApiResponse::class.java
        )
        if (response.code != 200) return MetadataFetchResult(error = "音乐服务返回错误：${response.msg}")

        val coverPath = response.data.picture.takeIf(String::isNotBlank)
            ?.let { runCatching { downloadCover(it) }.getOrNull() }
        val lyricPath = response.data.lrc.takeIf(String::isNotBlank)
            ?.let { runCatching { downloadNeteaseLyrics(it) }.getOrNull() }
        return MetadataFetchResult(
            coverPath = coverPath,
            lyricPath = lyricPath,
            albumName = response.data.album.takeIf(String::isNotBlank),
            albumArtist = response.data.songname.takeIf(String::isNotBlank),
            error = if (coverPath == null && lyricPath == null) "未获取到可用封面或歌词" else null
        )
    }

    private suspend fun fetchMixed(keywords: String): MetadataFetchResult = coroutineScope {
        val cover = async { runCatching { fetchQqCover(keywords) }.getOrNull() }
        val lyric = async { runCatching { fetchMiguLyrics(keywords) }.getOrNull() }
        val coverResult = cover.await()
        val lyricResult = lyric.await()
        MetadataFetchResult(
            coverPath = coverResult?.first,
            lyricPath = lyricResult,
            albumArtist = coverResult?.second,
            error = if (coverResult == null && lyricResult == null) "混合模式未获取到封面或歌词" else null
        )
    }

    private suspend fun fetchQqCover(keywords: String): Pair<String?, String?>? {
        val response = gson.fromJson(
            requestText("https://api.yaohud.cn/api/music/qq?key=$apiKey&msg=$keywords&n=1"),
            QqApiResponse::class.java
        )
        if (response.code != 200 || response.data.picture.isBlank()) return null
        return downloadCover(response.data.picture) to response.data.songname.takeIf(String::isNotBlank)
    }

    private suspend fun fetchMiguLyrics(keywords: String): String? {
        val response = gson.fromJson(
            requestText("https://api.yaohud.cn/api/music/migu?key=$apiKey&msg=$keywords&n=1"),
            MiguDetailResponse::class.java
        )
        if (response.code != 200 || response.data.lrc_url.isBlank()) return null
        return saveDirectLyrics(response.data.lrc_url)
    }

    private suspend fun downloadCover(url: String): String? {
        return executeRequest(url).use { response ->
            checkSuccessful(response)
            val body = response.body ?: return null
            val contentLength = body.contentLength()
            require(contentLength <= MAX_COVER_BYTES || contentLength < 0) { "封面文件过大" }
            fileStore.saveStream(
                body.byteStream(),
                "cover_${System.currentTimeMillis()}.jpg",
                MAX_COVER_BYTES
            )
        }
    }

    private suspend fun downloadNeteaseLyrics(url: String): String? {
        val response = gson.fromJson(requestText(url, MAX_LYRIC_BYTES), LrcJsonResponse::class.java)
        val lyric = response.data?.lyric?.takeIf(String::isNotBlank) ?: return null
        return fileStore.saveText(lyric, "lrc_${System.currentTimeMillis()}.lrc")
    }

    private suspend fun saveDirectLyrics(url: String): String? {
        val lyric = requestText(url, MAX_LYRIC_BYTES).takeIf(String::isNotBlank) ?: return null
        return fileStore.saveText(lyric, "lrc_${System.currentTimeMillis()}.lrc")
    }

    private fun requestText(url: String, maxBytes: Long = MAX_RESPONSE_BYTES): String {
        return executeRequest(url).use { response ->
            checkSuccessful(response)
            val body = response.body ?: error("响应内容为空")
            val contentLength = body.contentLength()
            require(contentLength <= maxBytes || contentLength < 0) { "响应内容过大" }
            String(readLimitedBytes(body.byteStream(), maxBytes), Charsets.UTF_8)
        }
    }

    private fun executeRequest(rawUrl: String): Response {
        if (networkAccess.isOffline) throw OfflineModeException()
        val secureUrl = if (rawUrl.startsWith("http://", ignoreCase = true)) {
            "https://${rawUrl.substring(7)}"
        } else {
            rawUrl
        }
        require(secureUrl.startsWith("https://", ignoreCase = true)) { "仅允许 HTTPS 地址" }
        return client.newCall(Request.Builder().url(secureUrl).build()).execute()
    }

    private fun checkSuccessful(response: Response) {
        require(response.isSuccessful) { "HTTP ${response.code}" }
    }

    private fun readLimitedBytes(input: InputStream, maxBytes: Long): ByteArray {
        val output = ByteArrayOutputStream()
        input.use { source ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
                val read = source.read(buffer)
                if (read < 0) break
                total += read
                require(total <= maxBytes) { "响应内容过大" }
                output.write(buffer, 0, read)
            }
        }
        return output.toByteArray()
    }

    companion object {
        private const val TIMEOUT_MS = 10_000
        private const val MAX_RESPONSE_BYTES = 4L * 1024 * 1024
        private const val MAX_COVER_BYTES = 15L * 1024 * 1024
        private const val MAX_LYRIC_BYTES = 2L * 1024 * 1024
    }
}
