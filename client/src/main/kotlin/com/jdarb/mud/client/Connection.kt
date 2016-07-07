package com.jdarb.mud.client

import io.vertx.core.AsyncResult
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.net.NetSocket
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameHelper
import io.vertx.ext.eventbus.bridge.tcp.impl.protocol.FrameParser
import java.util.Optional
import java.util.concurrent.CountDownLatch

internal object connection {
    private val vertx = Vertx.vertx()
    private val client = vertx.createNetClient()
    private lateinit var socket: NetSocket
    private lateinit var host: String
    //I would rather set this to lateinit or something but you can't.  I would prefer to not put logic around testing
    //for MIN_VALUE.
    private var port = Int.MIN_VALUE

    var connected = false
        private set

    private val notConnectedException = Exception("You are not connected.")

    fun connect(inputHost: String, inputPort: Int, responseHandler: (String) -> Unit) {
        if(!connected) {
            host = inputHost
            port = inputPort
            var t = Optional.empty<Throwable>()
            val latch = CountDownLatch(1)
            client.connect(port, host) { result: AsyncResult<NetSocket> ->
                if(result.succeeded()) {
                    socket = result.result()

                    socket.register("global.chat.read")
                    socket.send("global.chat.client", JsonObject().put("action", "/send").put("text", "wowowowowowow"))

                    val parser = FrameParser() { parse ->
                        if(parse.failed()) throw Exception("Failed to parse server message.", parse.cause())
                        else {
                            val message = frameToMessage(parse.result())

                            if(message.body.isPresent) responseHandler(message.body.get().getString("message"))
                        }
                    }
                    socket.handler(parser)
                    latch.countDown()
                } else {
                    t = Optional.of(result.cause())
                    latch.countDown()
                }
            }
            latch.await()
            if (t.isPresent) throw Exception(t.get())
            socket.closeHandler {
                println("Closed socket")
                connected = false
            }
            connected = true
        } else throw Exception("Already connected to $host:$port")
    }

    fun close() {
        if(connected) {
            socket.close()
            connected = false
        } else throw notConnectedException
    }

    fun sendMessage(message: String) {
        if(connected) {
            socket.send("connection.monitor", JsonObject().put("message", message))
        } else throw notConnectedException
    }

    //Call this during application shutdown
    fun shutdown() {
        if(connected) {
            close()
        }
        client.close()
        vertx.close()
        connected = false
    }
}

private fun NetSocket.send(address: String, body: JsonObject) = FrameHelper.sendFrame("send", address, body, this)

private fun NetSocket.register(address: String) = FrameHelper.sendFrame("register", address, null, this)

private fun NetSocket.unregister(address: String) = FrameHelper.sendFrame("unregister", address, null, this)

private fun NetSocket.publish(address: String, body: JsonObject) = FrameHelper.sendFrame("publish", address, body, this)


private enum class Type {
    message,
    send,
    publish,
    register,
    unregister
}

private data class Message(
        val type: Type,
        val headers: Optional<List<Pair<String, String>>>,
        val body: Optional<JsonObject>,
        val address: String,
        val replyAddress: Optional<String>
)

private fun frameToMessage(frame: JsonObject): Message {
    val type = Type.valueOf(frame.getString("type"))
    val headers = frame.getJsonObject("headers")?.run { Optional.of(map { it.key to it.value.toString() }) }
            ?: Optional.empty<List<Pair<String, String>>>()
    val body = frame.getJsonObject("body").run {
        if (isEmpty) Optional.empty<JsonObject>()
        else Optional.of(this)
    }
    val address = frame.getString("address")
    val replyAddress = frame.getJsonObject("replyAddress")?.run { Optional.of(encode()) } ?: Optional.empty<String>()
    return Message(type, headers, body, address, replyAddress)
}