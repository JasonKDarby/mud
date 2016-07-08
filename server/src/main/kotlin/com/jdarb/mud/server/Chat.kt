package com.jdarb.mud.server

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject

class Chat(val address: String) : AbstractVerticle() {

    private lateinit var eb: EventBus

    //This list will be used to send messages to subscribed users.
    //It also serves as a mapping of usernames to addresses.
    private val subscribedUsers = mutableListOf<Pair<String, String>>()

    override fun start() {

        eb = vertx.eventBus()

        //Users can register
        val globalChatPublisher = eb.publisher<JsonObject>("$address.chat.read")

        //
        val globalChatConsumer = eb.consumer<JsonObject>("$address.chat.client") { message ->
            val action = message.body().getString("action")
            when (action) {
                "/join" -> {

                }
                "/leave" -> {

                }
                "/send" -> {
                    val text = message.body().getString("text")
                    eb.publish(globalChatPublisher.address(),
                            JsonObject().put("username", message.replyAddress()).put("message", text))
                }
            }
            message.reply(200)
        }
    }
}
