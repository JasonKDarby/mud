package com.jdarb.mud.client

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketFrame
import io.vertx.core.json.JsonObject
import java.util.Optional
import java.util.concurrent.CountDownLatch

internal object connection {
    private val vertx = Vertx.vertx()
    private lateinit var httpClient: HttpClient
    private lateinit var ws: WebSocket
    private lateinit var host: String
    //I would rather set this to lateinit or something but you can't.  I would prefer to not put logic around testing
    //for MIN_VALUE.
    private var port = Int.MIN_VALUE

    var connected = false
        private set

    private val notConnectedException = Exception("You are not connected.")

    fun connect(inputHost: String, inputPort: Int, responseHandler: (Buffer) -> Unit) {
        if(!connected) {
            host = inputHost
            port = inputPort
            //This whole thing about catching the error was difficult to debug and I'm kind of proud of it.
            var t = Optional.empty<Throwable>()
            val latch = CountDownLatch(1)
            httpClient = vertx.createHttpClient(HttpClientOptions().setDefaultHost(host).setDefaultPort(port))
            //You have to pass the closure for throwables or else some will just be printed and not thrown.
            httpClient.websocket(
                    "/eventbus/websocket",
                    { websocket ->
                        ws = websocket
                        ws.write("publish", "connection.monitor", "publish from client")
                        ws.write("register", "connection.monitor", "register from client")
                        ws.write("send", "connection.monitor", "send from client")
                        ws.write("unregister", "connection.monitor", "unregister from client")
                        latch.countDown()
                    },
                    { throwable -> t = Optional.of(throwable); latch.countDown() }
            )
            latch.await()
            ws.handler(responseHandler)
            if (t.isPresent) throw Exception(t.get())
            connected = true
        } else throw Exception("Already connected to $host:$port")
    }

    fun close() {
        if(connected) {
            ws.close()
            httpClient.close()
            connected = false
        } else throw notConnectedException
    }

    fun sendMessage(message: String) {
        if(connected) {
            ws.write(Buffer.buffer(message))
        } else throw notConnectedException
    }

    //Call this during application shutdown
    fun shutdown() {
        if(connected) {
            close()
            connected = false
        }
        vertx.close()
    }
}

private fun WebSocket.write(type: String, address: String, body: String) =
        writeFrame(WebSocketFrame.textFrame(JsonObject().put("type", type).put("address", address).put("body", body).encode(), true))

private fun WebSocket.write(text: String) = write(Buffer.buffer(text))