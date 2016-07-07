package com.jdarb.mud.server

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.jdarb.mud.server.auth.AuthenticationVerticle
import com.jdarb.mud.server.bridge.TCPBridge
import com.jdarb.mud.server.db.DynamoDBUserPersister
import io.vertx.core.*
import kotlin.concurrent.thread

private val vertx = Vertx.vertx()

private val dynamoDBUserPersister = DynamoDBUserPersister(DynamoDB(AmazonDynamoDBClient()))

private val verticlesToDeploy = listOf<Pair<Verticle, Boolean>>(
        ConnectionMonitor() to false,
        TCPBridge() to false,
        AuthenticationVerticle(dynamoDBUserPersister) to true,
        GlobalChat() to false)

fun main(args: Array<String>) = startServer()

private fun startServer() {
    verticlesToDeploy.forEach { vertx.deployVerticle(it.first, DeploymentOptions().setWorker(it.second)) }

    Runtime.getRuntime().addShutdownHook(thread(start = false) {
        stopServer()
    })
}

private fun stopServer() {
    dynamoDBUserPersister.close()
    vertx.close()
}

//private fun handleWebsocketConnection(websocket: ServerWebSocket): Unit {
//    websocket.apply {
//        println("Websocket listening at: ${remoteAddress()}")
//        write("Welcome to MUD.")
//        greet()
//        closeHandler { println("Closed connection to ${remoteAddress()}") }
//    }
//}

internal fun <T> blocking(blockingCodeHandler: (Future<T>) -> Unit, resultHandler: (AsyncResult<T>) -> Unit) =
        vertx.executeBlocking(blockingCodeHandler, resultHandler)