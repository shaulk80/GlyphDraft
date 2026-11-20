package com.glyphdraft.engine.models

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

/** A local HTTP server for the download tests. Bodies are registered by path; the requested paths are recorded. */
class TestModelServer : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
    private val bodies = ConcurrentHashMap<String, ByteArray>()
    private val statuses = ConcurrentHashMap<String, Int>()
    private val stalling = ConcurrentHashMap.newKeySet<String>()
    private val release = CountDownLatch(1)
    private val stopped = AtomicBoolean(false)

    /** Paths that were requested, in order. */
    val requests = CopyOnWriteArrayList<String>()

    init {
        server.createContext("/") { exchange -> handle(exchange) }
        server.start()
    }

    val port: Int get() = server.address.port

    fun serve(path: String, body: ByteArray) {
        bodies[path] = body
    }

    /** Answers [path] with an error status instead of the body. */
    fun failWith(path: String, status: Int) {
        statuses[path] = status
    }

    /** Sends half of the body of [path], then keeps the connection open until the server is closed. */
    fun stall(path: String) {
        stalling.add(path)
    }

    /** A manifest entry for [body] served at [path]; the folder path on disk is [diskPath]. */
    fun fileFor(path: String, body: ByteArray, diskPath: String = path.trimStart('/')): ModelFile {
        serve(path, body)
        return ModelFile(diskPath, "http://127.0.0.1:$port$path", sha256Hex(body), body.size.toLong())
    }

    private fun handle(exchange: HttpExchange) {
        val path = exchange.requestURI.path
        requests.add(path)
        val status = statuses[path]
        if (status != null) {
            exchange.sendResponseHeaders(status, -1)
            exchange.close()
            return
        }

        val body = bodies[path]
        if (body == null) {
            exchange.sendResponseHeaders(404, -1)
            exchange.close()
            return
        }

        exchange.sendResponseHeaders(200, body.size.toLong())
        val out = exchange.responseBody
        if (path in stalling) {
            out.write(body, 0, body.size / 2)
            out.flush()
            release.await()
        } else {
            out.write(body)
        }

        exchange.close()
    }

    override fun close() {
        release.countDown()
        if (stopped.compareAndSet(false, true)) {
            server.stop(0)
        }
    }

    companion object {
        fun sha256Hex(bytes: ByteArray): String =
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    }
}
