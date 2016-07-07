package com.jdarb.mud.server.bridge

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.ext.bridge.BridgeOptions
import io.vertx.ext.bridge.PermittedOptions
import io.vertx.ext.eventbus.bridge.tcp.TcpEventBusBridge

private const val port = 8080

private val allowAll = BridgeOptions()
        .addInboundPermitted(PermittedOptions())
        .addOutboundPermitted(PermittedOptions())

class TCPBridge : AbstractVerticle() {
    lateinit var bridge: TcpEventBusBridge

    override fun start() {
        bridge = TcpEventBusBridge.create(vertx, allowAll)
        bridge.listen(port) {
            vertx.eventBus().consumer("test") { message: Message<Any> ->
                println("Got test message")
            }
        }
    }

    override fun stop() {
        bridge.close()
    }
}