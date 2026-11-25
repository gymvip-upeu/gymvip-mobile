package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.*
import com.gymvip.mobile.data.remote.api.*
import com.gymvip.mobile.domain.repository.AuthState
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class FirstAccessRepositoryTest {
    @Test fun resetUsesSessionEmailAndRevokedSessionCannotKeepCachedAccess() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);GymVipDatabase.Schema.create(driver)
        val store=Store();var revoked=false;var resetRequested=false
        val client=GymVipHttpClient.create(MockEngine { request ->
            val path=request.url.encodedPath
            if(path=="/api/v1/auth/me" && revoked) respond("{}",HttpStatusCode.Unauthorized,headersOf(HttpHeaders.ContentType,"application/json"))
            else {
                val body=when(path) {
                    "/api/v1/auth/login" -> """{"token":"fixture","id":"member","email":"member@example.test","roles":["CLIENTE"]}"""
                    "/api/v1/member-app/session" -> """{"mustChangePassword":false}"""
                    "/api/v1/member-app/me" -> """{"client":{"id":"client","email":"member@example.test","firstName":"Socio"}}"""
                    "/api/v1/member-app/password-reset" -> { resetRequested=true;"""{"message":"Enlace enviado"}""" }
                    else -> "{}"
                }
                respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            }
        },enableLogging=false)
        try {
            val repository=AuthRepositoryImpl(AuthApi(client,"http://test"),MemberApi(client,"http://test"),store,GymVipDatabase(driver))
            assertTrue(repository.login("member@example.test","PersonalPassword2026!").isSuccess)
            assertIs<AuthState.Authenticated>(repository.getAuthState().first())
            assertTrue(repository.requestOwnPasswordReset().isSuccess);assertTrue(resetRequested)
            revoked=true
            repository.validateActiveSession()
            assertIs<AuthState.Unauthenticated>(repository.getAuthState().first());assertNull(store.token)
        } finally {client.close();driver.close()}
    }
    private class Store : SecureTokenStore {
        var token: String? = null
        var account: String? = null
        var roles: List<String> = emptyList()
        override suspend fun saveTokens(accessToken: String, refreshToken: String?, accountId: String) { token=accessToken;account=accountId }
        override suspend fun getAccessToken()=token
        override suspend fun getRefreshToken(): String?=null
        override suspend fun getAccountId()=account
        override suspend fun clear() { token=null;account=null;roles=emptyList() }
        override suspend fun saveAccountRoles(roles: List<String>) { this.roles=roles }
        override suspend fun getAccountRoles()=roles
        override suspend fun saveRememberedIdentifier(identifier: String) {}
        override suspend fun getRememberedIdentifier(): String?=null
        override suspend fun clearRememberedIdentifier() {}
    }
    @Test fun temporaryLoginRequiresNewPasswordWithoutFetchingOrCachingProtectedProfile() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);GymVipDatabase.Schema.create(driver)
        val db=GymVipDatabase(driver);val store=Store();var protectedRequests=0
        val client=GymVipHttpClient.create(MockEngine { request ->
            val body=when(request.url.encodedPath) {
                "/api/v1/auth/login" -> """{"token":"fixture","id":"member","email":"member@example.test","roles":["CLIENTE"]}"""
                "/api/v1/member-app/session" -> """{"mustChangePassword":true}"""
                else -> {protectedRequests++;"{}"}
            }
            respond(body,HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val repository=AuthRepositoryImpl(AuthApi(client,"http://test"),MemberApi(client,"http://test"),store,db)
            assertTrue(repository.login("member@example.test","Temporary123!").isSuccess)
            assertIs<AuthState.PasswordChangeRequired>(repository.getAuthState().first())
            assertEquals(0,protectedRequests)
            assertNull(db.gymVipDatabaseQueries.selectProfileByAccountId("member").executeAsOneOrNull())
        } finally {client.close();driver.close()}
    }
    @Test fun unavailableSessionStatusCannotFallThroughIntoAuthenticatedHome() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);GymVipDatabase.Schema.create(driver)
        val store=Store()
        val client=GymVipHttpClient.create(MockEngine { request ->
            if(request.url.encodedPath.endsWith("/login")) respond("""{"token":"fixture","id":"member","email":"member@example.test","roles":["CLIENTE"]}""",HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
            else respond("{}",HttpStatusCode.ServiceUnavailable,headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val repository=AuthRepositoryImpl(AuthApi(client,"http://test"),MemberApi(client,"http://test"),store,GymVipDatabase(driver))
            assertTrue(repository.login("member@example.test","Temporary123!").isFailure)
            assertFalse(repository.getAuthState().first() is AuthState.Authenticated)
            assertNull(store.token)
        } finally {client.close();driver.close()}
    }
    @Test fun failedPasswordChangeKeepsFirstAccessAndSuccessfulChangeRequiresFreshLogin() = runTest {
        val driver=JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY);GymVipDatabase.Schema.create(driver)
        val store=Store();var reject=true
        val client=GymVipHttpClient.create(MockEngine { request ->
            val path=request.url.encodedPath
            val status=if(path.endsWith("/first-password") && reject) HttpStatusCode.BadRequest else HttpStatusCode.OK
            val body=when {
                path.endsWith("/login") -> """{"token":"fixture","id":"member","email":"member@example.test","roles":["CLIENTE"]}"""
                path.endsWith("/session") -> """{"mustChangePassword":true}"""
                path.endsWith("/first-password") && !reject -> """{"changed":true,"requiresLogin":true}"""
                else -> """{"message":"No se pudo cambiar"}"""
            }
            respond(body,status,headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val repository=AuthRepositoryImpl(AuthApi(client,"http://test"),MemberApi(client,"http://test"),store,GymVipDatabase(driver))
            repository.login("member@example.test","Temporary123!")
            assertTrue(repository.createFirstPassword("PersonalPassword2026!").isFailure)
            assertIs<AuthState.PasswordChangeRequired>(repository.getAuthState().first())
            reject=false
            assertTrue(repository.createFirstPassword("PersonalPassword2026!").isSuccess)
            assertIs<AuthState.Unauthenticated>(repository.getAuthState().first());assertNull(store.token)
        } finally {client.close();driver.close()}
    }
}
