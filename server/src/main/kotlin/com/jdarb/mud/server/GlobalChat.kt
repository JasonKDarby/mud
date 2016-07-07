package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject

class GlobalChat : AbstractVerticle() {

    private lateinit var eb: EventBus

    //This list will be used to send messages to subscribed users.
    //It also serves as a mapping of usernames to addresses.
    private val subscribedUsers = mutableListOf<Pair<String, String>>()

    override fun start() {

        eb = vertx.eventBus()

        //Users can register
        val globalChatPublisher = eb.publisher<JsonObject>("global.chat.read")


        //
        val globalChatConsumer = eb.consumer<JsonObject>("global.chat.client") { message ->
            val action = message.body().getString("action")
            when (action) {
                "/join" -> {

                }
                "/leave" -> {

                }
                "/send" -> {
                    val text = message.body().getString("text")
                    globalChatPublisher.send(JsonObject().put("message", text))
                }
            }
            message.reply(200)
        }
    }
}
