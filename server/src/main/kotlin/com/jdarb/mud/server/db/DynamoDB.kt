package com.jdarb.mud.server.db

import com.amazonaws.services.dynamodbv2.document.DynamoDB
import com.amazonaws.services.dynamodbv2.document.Item
import com.amazonaws.services.dynamodbv2.document.Table
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap
import com.amazonaws.services.dynamodbv2.model.*
import com.jdarb.mud.server.auth.User
import com.jdarb.mud.server.auth.UserAccountManager
import java.util.*

class DynamoDBUserPersister(val dynamoDB: DynamoDB) : UserAccountManager {

    init {
        setup()
    }

    private val authorizationTablePrefix = "mud-dev-"

    private fun md(input: String) = "$authorizationTablePrefix$input"

    private fun DynamoDB.getMudTable(tableName: String) = this.getTable(md(tableName))

    private fun DynamoDB.getUsersTable(): Table = this.getMudTable("users")

    private fun setup() {
        val usersTable = dynamoDB.getUsersTable()
        try {
            usersTable.describe()
        } catch (e: ResourceNotFoundException) {
            dynamoDB.createUsersTable()
        }
    }

    private fun DynamoDB.createUsersTable() = this.createTable(
            md("users"),
            listOf(KeySchemaElement("username", KeyType.HASH)),
            listOf(AttributeDefinition("username", ScalarAttributeType.S)),
            ProvisionedThroughput(1L, 1L)
    ).waitForActive().apply {
        if (tableStatus != "ACTIVE") throw IllegalStateException("Issue when creating table ${md("users")}: $this")
    }

    private fun DynamoDB.queryUsersWithUsername(username: String) = getUsersTable().query(
            QuerySpec()
                    .withKeyConditionExpression("username = :v_username")
                    .withValueMap(ValueMap().withString(":v_username", username)))

    override fun createUser(user: User): Unit {
        dynamoDB.getUsersTable().putItem(
                Item().withPrimaryKey("username", user.username).withString("password", user.password))
    }

    override fun getUserIfItExists(username: String): Optional<User> =
            dynamoDB.queryUsersWithUsername(username).run {
                when (count()) {
                    0 -> Optional.empty<User>()
                    1 -> {
                        val userItem = first()
                        val user = User(username = userItem.getString("username"), password = userItem.getString("password"))
                        Optional.of(user)
                    }
                    else -> throw IllegalStateException("More than one user returned for username $username")
                }
            }

    fun close() {
        dynamoDB.shutdown()
    }
}