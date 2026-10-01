// Copyright (c) 2026 eason204646. Licensed under Mulan PSL v2.
package com.music.purelymusic.utils

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Call
import okhttp3.EventListener
import java.io.IOException

/** 所有应用内 HTTP 请求共用此策略，包括图片加载和 Retrofit。 */
class NetworkAccess(initiallyOffline: Boolean = false) {
    @Volatile
    var isOffline: Boolean = initiallyOffline
        private set

    private val activeCalls = mutableSetOf<Call>()

    private val offlineInterceptor = Interceptor { chain ->
        if (isOffline) throw OfflineModeException()
        chain.proceed(chain.request())
    }

    val client: OkHttpClient = OkHttpClient.Builder()
        .eventListenerFactory {
            object : EventListener() {
                override fun callStart(call: Call) {
                    synchronized(this@NetworkAccess) { activeCalls.add(call) }
                }

                override fun callEnd(call: Call) {
                    synchronized(this@NetworkAccess) { activeCalls.remove(call) }
                }

                override fun callFailed(call: Call, ioe: IOException) {
                    synchronized(this@NetworkAccess) { activeCalls.remove(call) }
                }
            }
        }
        .addInterceptor(offlineInterceptor)
        // 重定向、重试等后续请求也必须重新检查离线状态。
        .addNetworkInterceptor(offlineInterceptor)
        .build()

    @Synchronized
    fun setOfflineMode(enabled: Boolean) {
        if (isOffline == enabled) return
        isOffline = enabled
        if (enabled) {
            // execute() 返回响应头后请求已离开 Dispatcher，但响应体可能还在下载。
            activeCalls.toList().forEach(Call::cancel)
            client.dispatcher.cancelAll()
        }
    }

    companion object {
        val shared = NetworkAccess()
    }
}

class OfflineModeException : IOException("离线模式已开启，联网功能已暂停")
