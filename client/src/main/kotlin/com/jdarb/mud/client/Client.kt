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
    override fun start(primaryStage: Stage) = initPrimaryStage(primaryStage).show()
}

private val defaultNl = System.lineSeparator()

private val defaultUserInputIndicator = " > "

private val initialMessagesText = "Type 'connect <IP>:<port>'$defaultNl"

private val messages = TextArea().apply {
    isEditable = false
    appendText(initialMessagesText)
}

private val input = TextField().apply {
    setOnAction { onInputAction(it, this) }
}

private fun onInputAction(value: ActionEvent, textField: TextField = input, textArea: TextArea = messages): Future<Unit> =
        if (textField.text.isNotEmpty()) textField.text.let { rawText ->
            textArea.appendText(rawText.toUserText())
            textField.clear()
            Future.submit {
                textField.clearOnAction()
                Future.submit { processInput(rawText) }.apply {
                    onSuccess { textField.setDefaultOnAction() }
                    onError {
                        textField.setDefaultOnAction()
                        textArea.appendText(it.toServerErrorMessage())
                    }
                }
            }.flatMap { it }
        }
        else Future.submit { }

private fun Exception.toServerErrorMessage() = when (this.message) {
    //TODO: this doesn't make a whole lot of sense
    null -> "Whoops, there was an error without any text.".toServerText()
    else -> {
        "ERROR: ${this.message!!.toServerText()}"
    }
}

private fun String.toUserText(userInputIndicator: String = defaultUserInputIndicator, nl: String = defaultNl): String =
"$userInputIndicator$this$nl"

private fun String.toServerText(nl: String = defaultNl): String = "$this$nl"

private fun TextField.setDefaultOnAction() = setOnAction { onInputAction(it) }

private fun TextField.clearOnAction() = setOnAction {  }

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