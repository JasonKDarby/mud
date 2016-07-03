package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
private val vertx = Vertx.vertx()
private val eb = vertx.eventBus()
private val httpServer = vertx.createHttpServer()

private val verticlesToDeploy = listOf(TestVerticle())

fun main(args: Array<String>) = startServer()

private fun startServer() {
    verticlesToDeploy.forEach { vertx.deployVerticle(it) }
    httpServer.websocketHandler { websocket ->
        println("Websocket listening at: ${websocket.path()}")
        websocket.handler { message ->
            println("Received message: ${message.toString()}")
            websocket.writeBinaryMessage(message)
        }
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