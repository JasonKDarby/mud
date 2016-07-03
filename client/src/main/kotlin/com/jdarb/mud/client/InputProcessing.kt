package com.jdarb.mud.client

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocketStream
import java.util.*
import java.util.concurrent.CountDownLatch

private val vertx = Vertx.vertx()
private var httpClient = Optional.empty<HttpClient>()
private var ws = Optional.empty<WebSocketStream>()
private var host = Optional.empty<String>()
private var port = Optional.empty<Int>()

internal fun processInput(input: String) = when {
    input.startsWith("connect ") -> {
        if (ws.isPresent)
            throw Exception("You are already connected to ${host.orElseThrow {
                Exception("Unanticipated error state.  Host is not set but websocket is connected.") }
            }:${port.orElseThrow {
                Exception("Unanticipated error state.  Port is not set but websocket is connected.") }
            }.")
        else {
            val arguments = input.removePrefix("connect ").split(":")
            port = Optional.of(Integer.parseInt(arguments[1]))
            host = Optional.of(arguments[0])
            httpClient = Optional.of(vertx.createHttpClient(
                    HttpClientOptions().setDefaultHost(host.get()).setDefaultPort(port.get())))
            Unit
        }
    }
    input == "close" -> if(httpClient.isPresent) {
        httpClient.get().close()
        ws = Optional.empty()
        host = Optional.empty()
        port = Optional.empty()
    } else throw Exception("Not connected.")
    else -> {
        if (httpClient.isPresent) {
            ws = Optional.of(httpClient.get().websocketStream(""))
            if (ws.isPresent) ws.get().let { ws ->
                ws.handler { websocket ->
                    val latch = CountDownLatch(1)
                    websocket.handler { message ->
                        println(message.toString())
                        websocket.close()
                    }
                    websocket.write(Buffer.buffer(input))
                }

                Unit
            } else throw Exception("You done goofed!")
        } else throw Exception("You done goofed!")
    }
}
