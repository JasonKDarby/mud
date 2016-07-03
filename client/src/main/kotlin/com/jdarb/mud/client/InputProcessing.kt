package com.jdarb.mud.client

import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.WebSocket
import javafx.scene.control.TextArea

internal fun processInput(input: String, resultTarget: TextArea) = when {
    input.startsWith("connect ") -> {
        val arguments = input.removePrefix("connect ").split(":")
        connection.connect(arguments[0], Integer.parseInt(arguments[1]))
        resultTarget.appendText("Connected".toServerText())
        Unit
    }
    input == "close" -> {
        connection.close()
        resultTarget.appendText("Closed connection".toServerText())
    }
    else -> {
        connection.sendMessage(input) { resultTarget.appendText(it.toString().toServerText()) }
    }
}

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

    fun connect(inputHost: String, inputPort: Int) {
        if(!connected) {
            host = inputHost
            port = inputPort
            httpClient = vertx.createHttpClient(HttpClientOptions().setDefaultHost(host).setDefaultPort(port))
            httpClient.websocket("") { ws = it }
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

    fun sendMessage(message: String, responseHandler: (Buffer) -> Unit) {
        if(connected) {
            ws.handler(responseHandler)
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