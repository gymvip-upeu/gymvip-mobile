package com.gymvip.mobile.data.repository

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.gymvip.mobile.data.local.GymVipDatabase
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import com.gymvip.mobile.data.remote.api.MemberApi
import com.gymvip.mobile.domain.error.AppError
import com.gymvip.mobile.data.remote.dto.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class MembershipPlansTest {
    @Test fun mergedCoverageDoesNotBorrowConditionsFromAnotherOrCancelledPeriod() {
        val workspace = MemberWorkspaceDto(
            client = MemberClientDto("member"),
            coverage = MemberCoverageDto(state = "VIGENTE", planName = "Cobertura registrada", startDate = "2026-10-01", lastIncludedDate = "2026-11-30", endExclusive = "2026-12-01"),
            periods = listOf(
                MemberPeriodDto("cancelled", start_date = "2026-10-01", end_date = "2026-12-01", conditions = "Cancelado", cancelled_at_utc = "2026-10-01T00:00:00Z"),
                MemberPeriodDto("newer", start_date = "2026-11-01", end_date = "2026-12-01", conditions = "Otro período")
            )
        )
        val summary = workspace.toMembershipDto()
        assertEquals("2026-11-30", summary.endDate)
        assertNull(summary.durationDays)
        assertNull(summary.conditions)
        assertEquals("Cobertura registrada", summary.planName)
    }

    @Test fun memberPlansUseTheirOwnEndpointAndOfflineCacheIsAccountScoped() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        var status = HttpStatusCode.OK
        var body = """[{"id":"annual","name":"Anual","durationDays":365,"pricePen":990.00},{"id":"monthly","name":"Mensual","durationDays":30,"pricePen":120.00}]"""
        val client = GymVipHttpClient.create(MockEngine { request ->
            assertEquals("/api/v1/member-app/plans", request.url.encodedPath)
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }, enableLogging = false)
        try {
            val repository = MembershipPlansRepository(MemberApi(client, "http://test"), GymVipDatabase(driver))
            val online = repository.load("socio-1").getOrThrow()
            assertFalse(online.isCached)
            assertEquals(listOf(30, 365), online.plans.map { it.durationDays })
            assertEquals(120.0, online.plans.first().pricePen)
            status = HttpStatusCode.ServiceUnavailable
            body = """{"message":"No disponible"}"""
            assertTrue(repository.load("socio-1").getOrThrow().isCached)
            assertTrue(repository.load("socio-2").isFailure)
            status = HttpStatusCode.Unauthorized
            assertIs<AppError.Unauthorized>(repository.load("socio-1").exceptionOrNull())
            status = HttpStatusCode.Forbidden
            assertIs<AppError.Forbidden>(repository.load("socio-1").exceptionOrNull())
            status = HttpStatusCode.OK
            body = "[]"
            assertTrue(repository.load("socio-1").getOrThrow().plans.isEmpty())
            status = HttpStatusCode.ServiceUnavailable
            assertTrue(repository.load("socio-1").getOrThrow().plans.isEmpty())
        } finally { client.close(); driver.close() }
    }

    @Test fun currentContractDetailsComeFromMatchingPeriodAndSurviveLocalRead() = runTest {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        GymVipDatabase.Schema.create(driver)
        val client = GymVipHttpClient.create(MockEngine {
            respond("""{"client":{"id":"member","firstName":"Socio"},
                "coverage":{"state":"VIGENTE","planName":"Mensual vigente","startDate":"2026-11-01","lastIncludedDate":"2026-11-30","endExclusive":"2026-12-01","daysRemaining":0},
                "periods":[
                  {"id":"future","plan_name":"Anual futuro","start_date":"2026-12-01","end_date":"2027-12-01","duration_days":365,"conditions":"Otra condición"},
                  {"id":"current","plan_name":"Mensual vigente","start_date":"2026-11-01","end_date":"2026-12-01","duration_days":30,"conditions":"Condición registrada","benefits":"Sala de máquinas"}]}
            """, HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
        }, enableLogging = false)
        try {
            val repo = MemberRepositoryImpl(MemberApi(client, "http://test"), GymVipDatabase(driver))
            val summary = repo.refreshMembership("account").getOrThrow()
            assertEquals("current", summary.id)
            assertEquals(30, summary.durationDays)
            assertEquals("Condición registrada", summary.conditions)
            assertTrue(summary.isAccessAllowed, "El último día válido depende del estado autoritativo, no de daysRemaining > 0")
            val local = repo.observeMembership("account").first()!!
            assertEquals(summary.conditions, local.conditions)
            assertEquals(summary.benefits, local.benefits)
            assertNull(repo.observeMembership("another-account").first())
        } finally { client.close(); driver.close() }
    }
}
