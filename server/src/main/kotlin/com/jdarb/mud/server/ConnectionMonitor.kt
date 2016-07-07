package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle

internal const val connectionMonitorAddress = "connection.monitor"

class ConnectionMonitor : AbstractVerticle() {
    override fun start() {
        vertx.eventBus().consumer<Any>(connectionMonitorAddress).handler { message ->
            println("${message.address()} ${message?.body()?.toString() ?: "null body"} ${message?.body()?.javaClass.toString()}")
        }.completionHandler {
            println("Started consumer: ${it.succeeded()}")
        }
    }
}