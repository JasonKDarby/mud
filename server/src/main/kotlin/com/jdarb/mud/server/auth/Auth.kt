package com.jdarb.mud.server.auth

import io.vertx.core.AbstractVerticle
import io.vertx.core.eventbus.Message
import io.vertx.ext.auth.shiro.ShiroAuth
import org.apache.shiro.authc.*
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection
import java.util.*

data class User(val username: String, val password: String)

interface UserAccountManager {
    fun getUserIfItExists(username: String): Optional<User>

    fun createUser(user: User): Unit
}

class AuthenticationVerticle(val userAccountManager: UserAccountManager) : AbstractVerticle() {
    override fun start() {
        ShiroAuth.create(vertx, DynamoDBRealm(userAccountManager))
        vertx.eventBus().consumer("auth.isUsernameTaken", isUsernameTakenHandler())
        vertx.eventBus().consumer("auth.createUser", createUserHandler())
    }

    private fun isUsernameTakenHandler() = { message: Message<String> ->
        val user = userAccountManager.getUserIfItExists(message.body())
        message.reply(user.isPresent)
    }

    private fun createUserHandler() = { message: Message<User> ->
        userAccountManager.createUser(message.body())
        message.reply(true)
    }
}

class DynamoDBRealm(val userAccountManager: UserAccountManager) : AuthorizingRealm() {
    override fun doGetAuthenticationInfo(rawToken: AuthenticationToken): AuthenticationInfo? =
            (rawToken as UsernamePasswordToken).run {
                val user = userAccountManager.getUserIfItExists(username)
                if (user.isPresent and (user.get().password == String(password))) {
                    SimpleAuthenticationInfo(username, password, name)
                } else throw UnknownAccountException()
            }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo? {
        throw UnsupportedOperationException()
    }

}