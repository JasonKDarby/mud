package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.eventbus.EventBus

internal const val connectionMonitorAddress = "connection.monitor"

class ConnectionMonitor(val eb: EventBus) : AbstractVerticle() {
    override fun start() {
        val bridgeEventConsumer = eb.consumer<Buffer>(connectionMonitorAddress)
        bridgeEventConsumer.handler { message ->
            println("${message.body().toString()}")
        }
    }
}