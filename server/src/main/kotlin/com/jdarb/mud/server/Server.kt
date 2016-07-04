package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer

private val vertx = Vertx.vertx()
private val eb = vertx.eventBus()
private val httpServer = vertx.createHttpServer()

private val verticlesToDeploy = listOf(TestVerticle())

fun main(args: Array<String>) = startServer()

private fun startServer() {
    verticlesToDeploy.forEach { vertx.deployVerticle(it) }
    httpServer.websocketHandler { websocket ->
        println("Websocket listening at: ${websocket.remoteAddress()}")
        websocket.handler { message ->
            println("Received message: ${message.toString()}")
            websocket.write(Buffer.buffer("You sent: $message"))
        }
        websocket.closeHandler { println("Closed connection to ${websocket.remoteAddress()}") }
    }.listen(8080)
    eb.consumer<String>("server") { message ->
        println(message.body())
    }
}

private class TestVerticle : AbstractVerticle() {

    override fun start() {
        println("starting!")
    }

}