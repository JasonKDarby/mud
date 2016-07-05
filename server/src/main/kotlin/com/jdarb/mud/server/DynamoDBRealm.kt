package com.jdarb.mud.server

import org.apache.shiro.authc.AuthenticationInfo
import org.apache.shiro.authc.AuthenticationToken
import org.apache.shiro.authc.UsernamePasswordToken
import org.apache.shiro.authz.AuthorizationInfo
import org.apache.shiro.realm.AuthorizingRealm
import org.apache.shiro.subject.PrincipalCollection

class DynamoDBRealm : AuthorizingRealm() {
    override fun doGetAuthenticationInfo(rawToken: AuthenticationToken): AuthenticationInfo? {
        val token = rawToken as UsernamePasswordToken

    }

    override fun doGetAuthorizationInfo(principals: PrincipalCollection): AuthorizationInfo? {
        throw UnsupportedOperationException()
    }

}