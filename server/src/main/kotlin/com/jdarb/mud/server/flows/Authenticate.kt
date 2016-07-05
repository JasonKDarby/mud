package com.jdarb.mud.server.flows

import com.jdarb.mud.server.authProvider
import com.jdarb.mud.server.blocking
import com.jdarb.mud.server.db.createUser
import com.jdarb.mud.server.db.usernameIsTaken
import com.jdarb.mud.server.dynamoDB
import com.jdarb.mud.server.write
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject

internal fun ServerWebSocket.greet() {
    wh("Do you have an existing account? y/n") { message ->
        when (message.toLowerCase()) {
            "y", "yes" -> login(newUser = false)
            "n", "no" -> createAccount()
            else -> write("Sorry, do you have an existing account? Type y or n")
        }
    }
}

private fun ServerWebSocket.login(newUser: Boolean) {
    wh("Please login.", "username:") { username ->
        wh("password:") { password ->
            val authInfo = JsonObject().put("username", username).put("password", password)
            authProvider.authenticate(authInfo) { result ->
                if(result.succeeded()) {
                    if (!newUser) write("Welcome back, $username.")
                    //TODO: call next part of the chain with result.result() passed as an argument.
                } else {
                    write("Invalid credentials.")
                    login(newUser = false)
                }
            }
        }
    }
}

private fun ServerWebSocket.createAccount() {
    write("Ok, let's create an account.")
    wh("Please enter a username:") { username ->
        blocking({ future ->
            future.complete(dynamoDB.usernameIsTaken(username))
        }) { result: AsyncResult<Boolean> ->
            if (result.failed()) {
                serverError()
            } else {
                when (result.result()) {
                    true -> {
                        write("Sorry, '$username' is taken.")
                        createAccount()
                    }
                    false -> {
                        wh("Create a password:") { firstPassword ->
                            wh("Please confirm your password:") { secondPassword ->
                                if(firstPassword == secondPassword) {
                                    blocking({ future: Future<Unit> ->
                                        dynamoDB.createUser(username, firstPassword)
                                        future.complete()
                                    }) { result ->
                                        if (result.failed()) serverError()
                                        else {
                                            write("Thanks for joining us, $username.")
                                            //We could skip ahead with the logged in user but I think this is a more
                                            //normal approach.  Plus it saves a bit on maintenance since we will only
                                            //have authentication in one place.
                                            login(newUser = true)
                                        }
                                    }
                                } else {
                                    write("Passwords do not match.")
                                    createAccount()
                                }
                            }
                        }
                    }
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

private fun ServerWebSocket.serverError() {
    write("Server error.")
    greet()
}