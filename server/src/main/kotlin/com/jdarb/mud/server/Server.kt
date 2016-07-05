package com.jdarb.mud.server

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.jdarb.mud.server.db.DynamoDBRealm
import com.jdarb.mud.server.db.createTablesIfNotExist
import com.jdarb.mud.server.flows.greet
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.ServerWebSocket
import io.vertx.ext.auth.shiro.ShiroAuth
import java.time.Instant
import kotlin.concurrent.thread

private val vertx = Vertx.vertx()
private val eb = vertx.eventBus()
private val httpServer = vertx.createHttpServer()

internal val dynamoDB = DynamoDB(AmazonDynamoDBClient())

internal val authProvider = ShiroAuth.create(vertx, DynamoDBRealm())

private val verticlesToDeploy = listOf<Verticle>()

const val port = 8080

fun main(args: Array<String>) = startServer()

private fun startServer() {
    verticlesToDeploy.forEach { vertx.deployVerticle(it) }

    dynamoDB.createTablesIfNotExist()

    httpServer.websocketHandler { handleWebsocketConnection(it) }.listen(port)
    eb.consumer<String>("server") { message ->
        println(message.body())
    }

    println("Server is up at ${Instant.now()} on port $port")

    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        stopServer()
    })
}

private fun stopServer() {
    vertx.close()
    dynamoDB.shutdown()
}

private fun handleWebsocketConnection(websocket: ServerWebSocket): Unit {
    websocket.apply {
        println("Websocket listening at: ${remoteAddress()}")
        write("Welcome to MUD.")
        greet()
        closeHandler { println("Closed connection to ${remoteAddress()}") }
    }
}

internal fun ServerWebSocket.write(text: String) = this.write(Buffer.buffer(text))

internal fun <T> blocking(blockingCodeHandler: (Future<T>) -> Unit, resultHandler: (AsyncResult<T>) -> Unit) =
        vertx.executeBlocking(blockingCodeHandler, resultHandler)