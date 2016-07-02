package com.jdarb.mud.client

import com.rahulrav.futures.Future
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.scene.Scene
import javafx.scene.control.TextArea
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage

class Client : Application() {
    companion object {
        @JvmStatic fun main(args: Array<String>) = launch(Client::class.java)
    }

    override fun start(primaryStage: Stage) = initPrimaryStage(primaryStage).show()

}

private val nl = System.lineSeparator()

private val userInputIndicator = " > "

private val initialMessagesText = "Type 'connect <IP>:<port>'$nl"

private val inputDelay = 200

private val messages = TextArea().apply {
    isEditable = false
    appendText(initialMessagesText)
}

private val input = TextField().apply {
    setOnAction { onInputAction(it, this, messages, inputDelay.toLong()) }
}

private fun onInputAction(value: ActionEvent, input: TextField, messages: TextArea, inputDelay: Long): Unit =
        if (input.text.isNotEmpty()) {
            val text = input.text //We need a dedicated text variable because input.clear wipes input.text
            messages.appendText(userInputIndicator+text+nl)
            input.clear()
            Future.submit {
                input.setOnAction {}
                Future.submit { processInput(text) }
                Thread.sleep(inputDelay)
                input.setOnAction { onInputAction(value, input, messages, inputDelay) }
            }
            Unit
        }
        else Unit //If something should be done when no text is entered the action should go here

private fun processInput(input: String) = Unit

private val footer = ToolBar()

private fun initPrimaryStage(primaryStage: Stage) = primaryStage.apply {
    title = "MUD"
    scene = Scene(VBox(10.0, messages, input, footer).apply { setPrefSize(600.0, 600.0) }, 300.0, 250.0)
    VBox.setVgrow(messages, Priority.ALWAYS)
    height = 600.0
    width = 600.0
    minHeight = 300.0
    minWidth = 300.0
}