package com.jdarb.mud.client

import org.fxmisc.richtext.StyleClassedTextArea

internal fun processInput(input: String, resultTarget: StyleClassedTextArea, eb: EventBusTCPBridgeClient) =
        input.sanitize().let { sanitized ->
            when {
                CONNECT_REGEX.matches(sanitized) -> {
                    val (@Suppress("UNUSED_VARIABLE") fullMatch, host, port) =
                            CONNECT_REGEX.find(sanitized)!!.groupValues
                    eb.connect(host, Integer.parseInt(port)) { resultTarget.appendServerText(it) }
                    resultTarget.appendClientText("Connected.")
                }
                CLOSE_REGEX.matches(sanitized) -> {
                    eb.close(); resultTarget.appendClientText("Closed EventBusTCPBridgeClient.")
                }
                else -> if (eb.connected) {
                    eb.sendMessage(input)
                } else throw Exception("Unrecognized command.")
            }
        }

//This is trivial for now but due to the likelihood of increasing sanitize measures I'll leave it.
private fun String.sanitize() = this.trim()