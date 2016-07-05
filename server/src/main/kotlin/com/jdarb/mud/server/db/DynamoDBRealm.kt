package com.jdarb.mud.server.db

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.*
import com.jdarb.mud.server.dynamoDB
import org.apache.shiro.authc.*
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

private val authorizationTablePrefix = "mud-dev-"
private fun md(input: String) = "$authorizationTablePrefix$input"
private fun DynamoDB.getMudTable(tableName: String) = this.getTable(md(tableName))

class DynamoDBRealm : AuthorizingRealm() {
    override fun doGetAuthenticationInfo(rawToken: AuthenticationToken): AuthenticationInfo? =
            (rawToken as UsernamePasswordToken).run {
                val items = dynamoDB.queryUsersWithUsername(username)
                when (items.count()) {
                    0 -> throw UnknownAccountException()
                    1 -> {
                        val user = items.first()
                        val storedPassword = user.getString("password")
                        val givenPassword = String(password)
                        if(givenPassword == storedPassword)
                            SimpleAuthenticationInfo(username, password, name)
                        else throw UnknownAccountException()
                    }
                    else -> throw IllegalStateException(
                            "${dynamoDB.getUsersTable().tableName} has more than one result for username $username")
                }
            }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo? {
        throw UnsupportedOperationException()
    }

}

private fun DynamoDB.getUsersTable(): Table = this.getMudTable("users")

private fun DynamoDB.createUsersTable() = this.createTable(
        md("users"),
        listOf(KeySchemaElement("username", KeyType.HASH)),
        listOf(AttributeDefinition("username", ScalarAttributeType.S)),
        ProvisionedThroughput(1L, 1L)
).waitForActive().apply {
    if (tableStatus != "ACTIVE") throw IllegalStateException("Issue when creating table ${md("users")}: $this")
}

internal fun DynamoDB.createTablesIfNotExist(): Unit {
    val usersTable = this.getUsersTable()
    try { usersTable.describe() } catch (e: ResourceNotFoundException) { this.createUsersTable() }
}

internal fun DynamoDB.usernameIsTaken(username: String) =
        if (queryUsersWithUsername(username).count() > 0) true else false

private fun DynamoDB.queryUsersWithUsername(username: String) = getUsersTable().query(
        QuerySpec()
                .withKeyConditionExpression("username = :v_username")
                .withValueMap(ValueMap().withString(":v_username", username)))

internal fun DynamoDB.createUser(username: String, password: String): Unit {
    getUsersTable().putItem(Item().withPrimaryKey("username", username).withString("password", password))
}