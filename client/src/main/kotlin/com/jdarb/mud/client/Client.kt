package com.jdarb.mud.client

import com.rahulrav.futures.Future
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
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

private val input = TextFieldWithOnActionHack().apply {
    setOnAction { clearTextAndInput(this) }
}

private val disconnect = ButtonWithOnActionHack("close").apply {
    text = "Disconnect"
}

private val connect = ButtonWithOnActionHack("connect localhost:8080").apply {
    text = "Connect to localhost:8080"
}

private val footer = ToolBar(connect, disconnect)

//This function is used as a processor for ActionEvents which come up in various JavaFX components.
private fun clearTextAndInput(textField: TextFieldWithOnActionHack, textArea: TextArea = messages): Future<Unit> =
        if (textField.text.isNotEmpty()) textField.text.let { rawText ->
            textField.clear()
            input(rawText, textField, textArea)
        } else Future.submit { }

//You can use this function to simulate user input. It is what is actually used for real user input as well. It will
//handle preventing users from entering input before previous input has been processed by the server.
private fun input(rawText: String, componentWithOnActionHack: OnActionHack, textArea: TextArea = messages): Future<Unit> {
    later { textArea.appendText(rawText.toUserText()) }
    componentWithOnActionHack.clearOnAction()
    return Future.submit {
        Future.submit { processInput(rawText, textArea) }.apply {
            onSuccess { componentWithOnActionHack.setDefaultOnAction() }
            onError {
                componentWithOnActionHack.setDefaultOnAction()
                later { textArea.appendText(it.toServerErrorMessage()) }
            }
        }
    }.flatMap { it }
}

private fun Exception.toServerErrorMessage() = when (this.message) {
    //TODO: this doesn't make a whole lot of sense
    null -> "Whoops, there was an error without any text.".toServerText()
    else -> "ERROR: ${this.message!!.toServerText()}"
}

private fun String.toUserText(userInputIndicator: String = defaultUserInputIndicator, nl: String = defaultNl): String =
"$userInputIndicator$this$nl"

internal fun String.toServerText(nl: String = defaultNl): String = "$this$nl"

//Some things in JavaFX need to be interacted with synchronously and this is a good way to do it.
private fun later(toDoLater: () -> Unit) = javafx.application.Platform.runLater(toDoLater)

private fun initPrimaryStage(primaryStage: Stage) = primaryStage.apply {
    title = "MUD"
    scene = Scene(VBox(10.0, messages, input, footer).apply { setPrefSize(600.0, 600.0) }, 300.0, 250.0)
    VBox.setVgrow(messages, Priority.ALWAYS)
    height = 600.0
    width = 600.0
    minHeight = 300.0
    minWidth = 300.0
    setOnCloseRequest { connection.shutdown() }
}

//I know this looks pretty dumb but it's because JavaFX has a bunch of components that should have an interface that
//don't. If you need to prevent a user from activating a component while it's processing you can use this approach to
//treat components with setOnAction as an interface.
private interface OnActionHack {
    fun clearOnAction()
    fun setDefaultOnAction()
}

private class ButtonWithOnActionHack(val defaultInput: String) : OnActionHack, Button() {

    init {
        setDefaultOnAction()
    }

    override fun clearOnAction() = setOnAction {  }

    override fun setDefaultOnAction() = setOnAction { input(defaultInput, this) }

}

private class TextFieldWithOnActionHack : OnActionHack, TextField() {
    override fun clearOnAction() = setOnAction {  }

    override fun setDefaultOnAction() = setOnAction { clearTextAndInput(this) }

}