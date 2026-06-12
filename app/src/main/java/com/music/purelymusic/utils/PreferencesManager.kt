//Copyright (c) [2026] [eason204646]
//[purelymusic] is licensed under Mulan PSL v2.
//You can use this software according to the terms and conditions of the Mulan
//PSL v2.
//You may obtain a copy of Mulan PSL v2 at:
//         http://license.coscl.org.cn/MulanPSL2
//THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY
//KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
//NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
//See the Mulan PSL v2 for more details.
//
//Mulan Permissive Software License，Version 2
//
//Mulan Permissive Software License，Version 2 (Mulan PSL v2)
//
//January 2020 http://license.coscl.org.cn/MulanPSL2
package com.music.purelymusic.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * 偏好设置管理工具
 * 用于持久化存储用户设置
 */
object PreferencesManager {
    private const val PREFS_NAME = "purelymusic_prefs"
    
    // 语言设置
    private const val KEY_LANGUAGE = "language"
    const val LANGUAGE_ZH = "zh"
    const val LANGUAGE_EN = "en"
    
    // 歌词设置
    private const val KEY_LYRIC_GLOW = "lyric_glow"
    private const val KEY_LYRIC_FILTER = "lyric_filter"
    private const val KEY_LYRIC_STYLE = "lyric_style"
    
    // 自动获取源设置
    private const val KEY_AUTO_FETCH_SOURCE = "auto_fetch_source"
    const val SOURCE_NETEASE = "netease"   // 网易云（默认）
    const val SOURCE_MIXED = "mixed"        // 混合（QQ封面 + 咪咕歌词）
    
    // 自动从元数据获取封面和歌词开关
    private const val KEY_AUTO_FETCH_METADATA = "auto_fetch_metadata"

    // 自动切歌交叉渐入渐出开关
    private const val KEY_CROSSFADE_ENABLED = "crossfade_enabled"
    private const val KEY_CROSSFADE_DURATION_SECONDS = "crossfade_duration_seconds"
    private const val DEFAULT_CROSSFADE_DURATION_SECONDS = 3

    // 均衡器开关
    private const val KEY_EQUALIZER_ENABLED = "equalizer_enabled"
    
    private var prefs: SharedPreferences? = null
    
    /**
     * 初始化SharedPreferences
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
    
    /**
     * 获取语言设置
     */
    fun getLanguage(): String {
        return prefs?.getString(KEY_LANGUAGE, LANGUAGE_ZH) ?: LANGUAGE_ZH
    }
    
    /**
     * 保存语言设置
     */
    fun saveLanguage(language: String) {
        prefs?.edit()?.putString(KEY_LANGUAGE, language)?.apply()
    }
    
    /**
     * 获取歌词发光特效设置
     */
    fun getLyricGlow(): Boolean {
        return prefs?.getBoolean(KEY_LYRIC_GLOW, true) ?: true
    }
    
    /**
     * 保存歌词发光特效设置
     */
    fun saveLyricGlow(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_LYRIC_GLOW, enabled)?.apply()
    }
    
    /**
     * 获取歌词过滤脏字设置
     */
    fun getLyricFilter(): Boolean {
        return prefs?.getBoolean(KEY_LYRIC_FILTER, false) ?: false
    }
    
    /**
     * 保存歌词过滤脏字设置
     */
    fun saveLyricFilter(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_LYRIC_FILTER, enabled)?.apply()
    }
    
    /**
     * 获取歌词样式设置
     */
    fun getLyricStyle(): String {
        return prefs?.getString(KEY_LYRIC_STYLE, "multi") ?: "multi"
    }
    
    /**
     * 保存歌词样式设置
     */
    fun saveLyricStyle(style: String) {
        prefs?.edit()?.putString(KEY_LYRIC_STYLE, style)?.apply()
    }
    
    /**
     * 获取自动获取源设置
     */
    fun getAutoFetchSource(): String {
        return prefs?.getString(KEY_AUTO_FETCH_SOURCE, SOURCE_NETEASE) ?: SOURCE_NETEASE
    }
    
    /**
     * 保存自动获取源设置
     */
    fun saveAutoFetchSource(source: String) {
        prefs?.edit()?.putString(KEY_AUTO_FETCH_SOURCE, source)?.apply()
    }
    
    /**
     * 获取自动从元数据获取封面和歌词设置
     * 默认为 true（开启）
     */
    fun getAutoFetchMetadata(): Boolean {
        return prefs?.getBoolean(KEY_AUTO_FETCH_METADATA, true) ?: true
    }
    
    /**
     * 保存自动从元数据获取封面和歌词设置
     */
    fun saveAutoFetchMetadata(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_FETCH_METADATA, enabled)?.apply()
    }

    /**
     * 获取自动切歌交叉渐入渐出设置
     */
    fun getCrossfadeEnabled(): Boolean {
        return prefs?.getBoolean(KEY_CROSSFADE_ENABLED, false) ?: false
    }

    /**
     * 保存自动切歌交叉渐入渐出设置
     */
    fun saveCrossfadeEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_CROSSFADE_ENABLED, enabled)?.apply()
    }

    /**
     * 获取自动切歌交叉渐入渐出时长设置
     */
    fun getCrossfadeDurationSeconds(): Int {
        return prefs?.getInt(KEY_CROSSFADE_DURATION_SECONDS, DEFAULT_CROSSFADE_DURATION_SECONDS)
            ?: DEFAULT_CROSSFADE_DURATION_SECONDS
    }

    /**
     * 保存自动切歌交叉渐入渐出时长设置
     */
    fun saveCrossfadeDurationSeconds(seconds: Int) {
        prefs?.edit()?.putInt(KEY_CROSSFADE_DURATION_SECONDS, seconds.coerceIn(1, 10))?.apply()
    }

    fun getEqualizerEnabled(): Boolean {
        return prefs?.getBoolean(KEY_EQUALIZER_ENABLED, false) ?: false
    }

    fun saveEqualizerEnabled(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_EQUALIZER_ENABLED, enabled)?.apply()
    }

    // 均衡器频段增益持久化
    private const val KEY_EQUALIZER_BANDS = "equalizer_bands"

    fun saveEqualizerBands(bands: List<Short>) {
        val json = bands.joinToString(",") { it.toString() }
        prefs?.edit()?.putString(KEY_EQUALIZER_BANDS, json)?.apply()
    }

    fun getEqualizerBands(): List<Short>? {
        val json = prefs?.getString(KEY_EQUALIZER_BANDS, null) ?: return null
        return try {
            json.split(",").map { it.trim().toShort() }
        } catch (e: Exception) { null }
    }

    // 🚩 v2.5: 睡眠定时器默认时长（分钟），0 = 关闭
    private const val KEY_SLEEP_TIMER_DEFAULT = "sleep_timer_default_minutes"

    fun getSleepTimerDefaultMinutes(): Int {
        return prefs?.getInt(KEY_SLEEP_TIMER_DEFAULT, 30) ?: 30
    }

    fun saveSleepTimerDefaultMinutes(minutes: Int) {
        prefs?.edit()?.putInt(KEY_SLEEP_TIMER_DEFAULT, minutes.coerceIn(0, 120))?.apply()
    }
}