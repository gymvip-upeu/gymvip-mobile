package com.gymvip.mobile.data.repository

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.domain.error.AppError
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MemberWithoutMembershipTest {
    @Test fun missingMembershipIsValidAndDoesNotBecomeANetworkError() = runTest {
        val client=GymVipHttpClient.create(MockEngine {
            respond("""{"client":{"id":"qa-client","firstName":"Socio","lastName":"Prueba"},"coverage":{"state":"SIN_MEMBRESIA","allowed":false,"planName":null,"daysRemaining":null},"periods":[]}""",
                HttpStatusCode.OK,headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val result=MemberApi(client,"http://test").getProfile()
            assertTrue(result.isSuccess)
            assertEquals("Socio",result.getOrThrow().firstName)
        } finally {client.close()}
    }
    @Test fun invalidResponseIsASyncProblemInsteadOfMissingInternet() = runTest {
        val client=GymVipHttpClient.create(MockEngine {
            respond("""{"coverage":{"daysRemaining":"invalid"}}""",HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType,"application/json"))
        },enableLogging=false)
        try {
            val result=MemberApi(client,"http://test").getProfile()
            assertIs<AppError.Sync>(result.exceptionOrNull())
        } finally {client.close()}
    }
}
