package com.jdarb.mud.client

import org.fxmisc.richtext.StyleClassedTextArea

internal fun processInput(input: String, resultTarget: StyleClassedTextArea) = input.sanitize().let { sanitized ->
    when {
        CONNECT_REGEX.matches(sanitized) -> {
            val (@Suppress("UNUSED_VARIABLE") fullMatch, host, port) = CONNECT_REGEX.find(sanitized)!!.groupValues
            connection.connect(host, Integer.parseInt(port)) { resultTarget.appendServerText(it) }
            resultTarget.appendClientText("Connected.")
        }
        CLOSE_REGEX.matches(sanitized) -> {
            connection.close(); resultTarget.appendClientText("Closed connection.")
        }
        else -> if (connection.connected) {
            connection.sendMessage(input)
        } else throw Exception("Unrecognized command.")
    }
}

//This is trivial for now but due to the likelihood of increasing sanitize measures I'll leave it.
private fun String.sanitize() = this.trim()