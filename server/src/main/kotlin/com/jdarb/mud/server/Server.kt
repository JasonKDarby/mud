package com.jdarb.mud.server

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.shiro.ShiroAuth
import io.vertx.ext.auth.shiro.ShiroAuthOptions

private val vertx = Vertx.vertx()
private val authConfig = JsonObject().put("properties_path", "classpath:vertx-users.properties")
internal val authProvider = ShiroAuth.create(vertx, ShiroAuthOptions().setConfig(authConfig))
private val eb = vertx.eventBus()
private val httpServer = vertx.createHttpServer()

private val verticlesToDeploy = listOf<Verticle>()

fun main(args: Array<String>) = startServer()

private fun startServer() {
    verticlesToDeploy.forEach { vertx.deployVerticle(it) }

    httpServer.websocketHandler { handleWebsocketConnection(it) }.listen(8080)
    eb.consumer<String>("server") { message ->
        println(message.body())
    }
}

private fun handleWebsocketConnection(websocket: ServerWebSocket): Unit {
    websocket.apply {
        println("Websocket listening at: ${remoteAddress()}")
        write("Welcome to MUD.")
        greet()
//        handler { message ->
//            println("Received message: ${message.toString()}")
//            write(processInput(message.toString()))
//        }
        closeHandler { println("Closed connection to ${remoteAddress()}") }
    }
}

internal fun ServerWebSocket.write(text: String) = this.write(Buffer.buffer(text))