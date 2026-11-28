// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.music.purelymusic.model.LrcLine
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import java.security.MessageDigest

/** Stores complete translations of the parsed lyric timeline. Call from an IO dispatcher. */
internal class LyricTranslationCache(private val directory: File) {
    fun read(lines: List<LrcLine>): List<LrcLine>? = runCatching {
        val file = fileFor(lines)
        if (!file.isFile) return null
        val translations = file.reader(Charsets.UTF_8).use { JsonParser.parseReader(it).asJsonArray }
        if (translations.size() != lines.size) return null

        lines.mapIndexed { index, line ->
            val value = translations[index]
            val translation = if (line.content.isNotBlank() && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                value.asString.takeIf(String::isNotBlank)
            } else null
            line.copy(translation = translation)
        }.takeIf(::hasCompleteLyricTranslation)
    }.getOrNull()

    fun write(lines: List<LrcLine>) {
        if (!hasCompleteLyricTranslation(lines)) return
        val destination = fileFor(lines).toPath()
        Files.createDirectories(directory.toPath())
        val temporary = Files.createTempFile(directory.toPath(), "translation-", ".tmp")
        try {
            Files.newBufferedWriter(temporary, Charsets.UTF_8).use {
                Gson().toJson(lines.map(LrcLine::translation), it)
            }
            Files.move(temporary, destination, ATOMIC_MOVE, REPLACE_EXISTING)
        } finally {
            Files.deleteIfExists(temporary)
        }
    }

    private fun fileFor(lines: List<LrcLine>): File {
        val source = buildString {
            append("v1:auto:zh\n")
            for (line in lines) {
                append(line.time).append('|')
                append(line.isContinuation).append('|')
                append(line.content.length).append('|').append(line.content)
            }
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(source.toByteArray(Charsets.UTF_8))
        val key = digest.joinToString("") { "%02x".format(it) }
        return directory.resolve("$key.json")
    }
}

internal fun hasCompleteLyricTranslation(lines: List<LrcLine>): Boolean {
    val translatable = lines.filter { it.content.isNotBlank() }
    return translatable.isNotEmpty() && translatable.all { !it.translation.isNullOrBlank() }
}
