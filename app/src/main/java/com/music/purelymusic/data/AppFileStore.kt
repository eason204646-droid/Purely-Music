// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

/** 管理应用私有目录中的音乐、封面与歌词文件。 */
class AppFileStore(context: Context) {
    private val appContext = context.applicationContext
    private val root = File(appContext.filesDir, "library").apply { mkdirs() }

    suspend fun copyFromUri(uri: Uri, requestedName: String): String? = withContext(Dispatchers.IO) {
        val finalName = if (requestedName.substringAfterLast('.', "").isNotBlank()) {
            requestedName
        } else {
            requestedName + resolveExtension(uri)
        }
        val destination = File(root, finalName)
        val temporary = File(root, ".$finalName.part")
        try {
            val input = appContext.contentResolver.openInputStream(uri) ?: return@withContext null

            input.use { source ->
                temporary.outputStream().buffered().use { target -> source.copyTo(target) }
            }
            if (destination.exists() && !destination.delete()) {
                temporary.delete()
                error("无法替换目标文件")
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            destination.absolutePath
        } catch (_: Exception) {
            temporary.delete()
            null
        }
    }

    fun deleteOwnedFile(path: String?): Boolean {
        if (path.isNullOrBlank()) return false
        return runCatching {
            val file = File(path).canonicalFile
            val filesRoot = appContext.filesDir.canonicalFile
            val isManagedDirectory = file.toPath().startsWith(root.canonicalFile.toPath())
            val isLegacyManagedFile = file.parentFile == filesRoot && LEGACY_PREFIXES.any(file.name::startsWith)
            if ((!isManagedDirectory && !isLegacyManagedFile) || !file.isFile) return false
            file.delete()
        }.getOrDefault(false)
    }

    suspend fun saveStream(
        input: InputStream,
        fileName: String,
        maxBytes: Long = Long.MAX_VALUE
    ): String? = withContext(Dispatchers.IO) {
        val destination = File(root, fileName)
        val temporary = File(root, ".$fileName.part")
        try {
            input.use { source ->
                temporary.outputStream().buffered().use { target ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var total = 0L
                    while (true) {
                        val read = source.read(buffer)
                        if (read < 0) break
                        total += read
                        require(total <= maxBytes) { "文件超过允许大小" }
                        target.write(buffer, 0, read)
                    }
                }
            }
            if (!temporary.renameTo(destination)) {
                temporary.copyTo(destination, overwrite = true)
                temporary.delete()
            }
            destination.absolutePath
        } catch (_: Exception) {
            temporary.delete()
            null
        }
    }

    suspend fun saveText(text: String, fileName: String): String? {
        val bytes = text.toByteArray(Charsets.UTF_8)
        return saveStream(ByteArrayInputStream(bytes), fileName, bytes.size.toLong())
    }

    private fun resolveExtension(uri: Uri): String {
        val displayName = appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }
        val displayExtension = displayName?.substringAfterLast('.', "")?.lowercase().orEmpty()
        if (displayExtension.matches(Regex("[a-z0-9]{1,8}"))) return ".$displayExtension"

        val mime = appContext.contentResolver.getType(uri)
        val mimeExtension = mime?.let(MimeTypeMap.getSingleton()::getExtensionFromMimeType)
        return mimeExtension?.let { ".$it" }.orEmpty()
    }

    companion object {
        private val LEGACY_PREFIXES = listOf("mus_", "cov_", "lrc_", "cover_", "pl_cov_")
    }
}
