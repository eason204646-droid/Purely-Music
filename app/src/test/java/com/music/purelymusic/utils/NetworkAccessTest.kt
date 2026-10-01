package com.music.purelymusic.utils

import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class NetworkAccessTest {
    private val request = Request.Builder().url("https://example.invalid/test").build()

    @Test
    fun startupInOfflineModeBlocksRequestsBeforeTheyReachTheBackend() {
        val network = NetworkAccess(initiallyOffline = true)
        val requests = AtomicInteger()
        val client = respondingClient(network, requests)

        assertThrows(OfflineModeException::class.java) { client.newCall(request).execute() }
        assertEquals(0, requests.get())
    }

    @Test
    fun existingClientsShareTheSwitchAndResumeAfterOfflineModeIsDisabled() {
        val network = NetworkAccess()
        val requests = AtomicInteger()
        val metadataClient = respondingClient(network, requests)
        val imageClient = respondingClient(network, requests)
        assertEquals(network.client.dispatcher, metadataClient.dispatcher)
        assertEquals(network.client.dispatcher, imageClient.dispatcher)

        metadataClient.newCall(request).execute().close()
        imageClient.newCall(request).execute().close()
        network.setOfflineMode(true)

        assertThrows(OfflineModeException::class.java) { metadataClient.newCall(request).execute() }
        assertThrows(OfflineModeException::class.java) { imageClient.newCall(request).execute() }
        assertEquals(2, requests.get())

        network.setOfflineMode(false)
        metadataClient.newCall(request).execute().close()
        imageClient.newCall(request).execute().close()
        assertEquals(4, requests.get())
        assertFalse(network.isOffline)
    }

    @Test
    fun enablingOfflineModeCancelsBothRunningAndQueuedRequests() {
        val network = NetworkAccess()
        network.client.dispatcher.maxRequests = 1
        val started = CountDownLatch(1)
        val release = CountDownLatch(1)
        val completed = CountDownLatch(2)
        val reachedBackend = AtomicInteger()
        val failures = AtomicInteger()
        val client = network.client.newBuilder().addInterceptor { chain ->
            reachedBackend.incrementAndGet()
            started.countDown()
            check(release.await(5, TimeUnit.SECONDS))
            response(chain.request())
        }.build()
        val callback = object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                failures.incrementAndGet()
                completed.countDown()
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
                completed.countDown()
            }
        }
        val running = client.newCall(request)
        val queued = client.newCall(request)

        try {
            running.enqueue(callback)
            assertTrue(started.await(5, TimeUnit.SECONDS))
            queued.enqueue(callback)
            assertEquals(1, client.dispatcher.queuedCallsCount())

            network.setOfflineMode(true)
            assertTrue(running.isCanceled())
            assertTrue(queued.isCanceled())
            release.countDown()

            assertTrue(completed.await(5, TimeUnit.SECONDS))
            assertEquals(2, failures.get())
            assertEquals(1, reachedBackend.get())
        } finally {
            release.countDown()
            client.dispatcher.cancelAll()
            client.dispatcher.executorService.shutdownNow()
        }
    }

    @Test
    fun enablingOfflineModeStopsDownloadsEvenAfterResponseHeadersHaveArrived() {
        val network = NetworkAccess()
        val release = CountDownLatch(1)
        val server = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        val serverThread = thread(isDaemon = true) {
            server.accept().use { socket ->
                socket.soTimeout = 5_000
                val reader = socket.getInputStream().bufferedReader()
                while (reader.readLine()?.isNotEmpty() == true) { /* Read request headers. */ }
                val output = socket.getOutputStream()
                output.write("HTTP/1.1 200 OK\r\nContent-Length: 100\r\nConnection: close\r\n\r\n".toByteArray())
                output.write(1)
                output.flush()
                release.await(5, TimeUnit.SECONDS)
            }
        }
        val call = network.client.newCall(
            Request.Builder().url("http://127.0.0.1:${server.localPort}/cover").build()
        )

        try {
            call.execute().use { response ->
                val input = response.body!!.byteStream()
                assertEquals(1, input.read())
                assertEquals(0, network.client.dispatcher.runningCallsCount())

                network.setOfflineMode(true)

                assertTrue(call.isCanceled())
                assertThrows(IOException::class.java) { input.read() }
            }
        } finally {
            release.countDown()
            server.close()
            serverThread.join(5_000)
        }
    }

    private fun respondingClient(network: NetworkAccess, requests: AtomicInteger): OkHttpClient =
        network.client.newBuilder().addInterceptor { chain ->
            requests.incrementAndGet()
            response(chain.request())
        }.build()

    private fun response(request: Request): Response = Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body("ok".toResponseBody())
        .build()
}
