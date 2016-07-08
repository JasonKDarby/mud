package com.jdarb.mud.client

import com.rahulrav.futures.Future
import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.ToolBar
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import org.fxmisc.richtext.StyleClassedTextArea

private val eb = EventBusTCPBridgeClient()

class Client : Application() {

    override fun start(primaryStage: Stage) {
        initPrimaryStage(primaryStage).show()
    }

    override fun stop() {
        if(eb.connected) eb.shutdown()
    }

}

private val initialMessagesText = "Type 'connect <IP>:<port>'$defaultNl"

private val messages = StyleClassedTextArea().apply { isEditable = false; later { appendText(initialMessagesText) } }

private val input = TextFieldWithOnActionHack().apply { setOnAction { clearTextAndInput(this) } }

private val disconnect = ButtonWithOnActionHack("Disconnect", "close")

private val connect = ButtonWithOnActionHack("Connect to localhost:8080", "connect localhost:8080")

private val footer = ToolBar(connect, disconnect)

//This function is used as a processor for ActionEvents which come up in various JavaFX components.
private fun clearTextAndInput(textField: TextFieldWithOnActionHack, textArea: StyleClassedTextArea = messages): Future<Unit> =
        if (textField.text.isNotEmpty()) textField.text.let { rawText ->
            textField.clear()
            input(rawText, textField, textArea)
        } else Future.submit { }

//You can use this function to simulate user input. It is what is actually used for real user input as well. It will
//handle preventing users from entering input before previous input has been processed by the server.
private fun input(rawText: String, componentWithOnActionHack: OnActionHack, textArea: StyleClassedTextArea = messages): Future<Unit> {
    textArea.appendUserText(rawText)
    componentWithOnActionHack.clearOnAction()
    return Future.submit { processInput(rawText, textArea, eb) }.apply {
        onSuccess { componentWithOnActionHack.setDefaultOnAction() }
        onError {
            componentWithOnActionHack.setDefaultOnAction()
            textArea.appendErrorText(it)
        }
    }
}

private fun Exception.toServerErrorMessage() = when (this.message) {
    //TODO: this doesn't make a whole lot of sense
    null -> "Whoops, there was an error without any text.".toServerText()
    else -> "ERROR: ${this.message!!.toServerText()}"
}

internal fun StyleClassedTextArea.appendStyledText(text: String, cssClass: String) = later {
    val from = this.length - 1
    val to = from+text.length
    appendText(text)
    setStyleClass(from, to, cssClass)
}

internal fun StyleClassedTextArea.appendUserText(text: String) =
        appendStyledText(text.toUserText(), "user")

internal fun StyleClassedTextArea.appendErrorText(error: Exception) =
        appendStyledText(error.toServerErrorMessage(), "error")

internal fun StyleClassedTextArea.appendServerText(text: String) =
        appendStyledText(text.toServerText(), "server")

internal fun StyleClassedTextArea.appendClientText(text: String) =
        appendStyledText(text+defaultNl, "client")

private fun String.toUserText(userInputIndicator: String = defaultUserInputIndicator, nl: String = defaultNl): String =
    "$userInputIndicator$this$nl"

internal fun String.toServerText(nl: String = defaultNl): String = "$this$nl"

//Some things in JavaFX need to be interacted with synchronously and this is a good way to do it.
internal fun later(toDoLater: () -> Unit) = javafx.application.Platform.runLater(toDoLater)

private fun initPrimaryStage(primaryStage: Stage) = primaryStage.apply {
    val componentSpacing = 10.0
    val (defaultHeight, defaultWidth, defaultMinHeight, defaultMinWidth) = listOf(600.0, 600.0, 300.0, 300.0)
    title = "MUD"
    scene = Scene(VBox(componentSpacing, messages, input, footer)
            .apply { setPrefSize(defaultHeight, defaultWidth) }, defaultHeight, defaultWidth).apply {
        stylesheets.add("style.css")
    }

    VBox.setVgrow(messages, Priority.ALWAYS)
    height = defaultHeight; width = defaultWidth; minHeight = defaultMinHeight; minWidth = defaultMinWidth
    setOnCloseRequest { eb.shutdown() }
}

//I know this looks pretty dumb but it's because JavaFX has a bunch of components that should have an interface that
//don't. If you need to prevent a user from activating a component while it's processing you can use this approach to
//treat components with setOnAction as an interface.
private interface OnActionHack {
    fun clearOnAction()
    fun setDefaultOnAction()
}

private class ButtonWithOnActionHack(text: String, val defaultInput: String) : OnActionHack, Button(text) {
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