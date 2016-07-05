package com.jdarb.mud.server

import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject

internal fun ServerWebSocket.greet() {
    wh("Do you have an existing account? y/n") { message ->
        when (message.toLowerCase()) {
            "y", "yes" -> login()
            "n", "no" -> createAccount()
            else -> write("Sorry, do you have an existing account? Type y or n")
        }
    }
}

private fun ServerWebSocket.login() {
    wh("Please login.", "username:") { username ->
        wh("password:") { password ->
            val authInfo = JsonObject().put("username", username).put("password", password)
            authProvider.authenticate(authInfo) { result ->
                if(result.succeeded()) write("Welcome back, $username.")
                else {
                    write("Invalid credentials.")
                    login()
                }
            }
        }
    }
}

private fun ServerWebSocket.createAccount() {
    write("Ok, let's create an account.")
    wh("Please enter a username:") { username ->
        wh("Create a password:") { firstPassword ->
            wh("Please confirm your password:") { secondPassword ->
                if(firstPassword == secondPassword) {
                    //TODO: create the user's account, make sure to mark it as blocking.
                    write("Thanks for joining us, $username.")
                    //We could skip ahead with the logged in user but I think this is a more normal approach.  Plus it
                    //saves a bit on maintenance since we will only have authentication in one place.
                    login()
                } else {
                    write("Passwords do not match.")
                    createAccount()
                }
            }
        }
    }
}

//Write/handle.  This is a shortcut to make both reading and writing websocket handler code easier.
private fun ServerWebSocket.wh(vararg messages: String, h: (String) -> Unit) {
    handler { buffer -> h(buffer.toString()) }
    messages.forEach { write(it) }
}