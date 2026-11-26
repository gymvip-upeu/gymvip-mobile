package com.gymvip.mobile.features.saas

import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class SaasExperience2Test {

    private val jsonHeaders = headersOf(HttpHeaders.ContentType, "application/json")

    @Test
    fun educationalBmiComputesOmsCategoryWithNonMedicalDisclaimerAndZeroSomatotypes() {
        val res = EducationalBmiCalculator.evaluate(heightCm = 175.0, weightKg = 70.0)
        assertNotNull(res)
        assertEquals(22.86, res.bmi)
        assertTrue(res.categoryLabel.contains("Peso saludable"))
        assertTrue(res.disclaimer.contains("no", ignoreCase = true))
        assertFalse(res.categoryLabel.contains("endomorfo", ignoreCase = true))
        assertFalse(res.categoryLabel.contains("mesomorfo", ignoreCase = true))

        assertFailsWith<IllegalArgumentException> {
            EducationalBmiCalculator.evaluate(heightCm = 40.0, weightKg = 70.0)
        }
    }

    @Test
    fun saasExperienceApiParsesPublicGymsReleaseManifestAndDefaultOptOutPrefs() = runTest {
        val engine = MockEngine { req ->
            when (req.url.encodedPath) {
                "/api/v1/public/gyms" -> respond(
                    """[{"id":"11111111-1111-4111-8111-111111111111","code":"vip-center-fit","name":"VIP Center Fit","reviewsCount":2,"avgRating":4.8,"activeGroupPromosCount":1,"branches":[]}]""",
                    HttpStatusCode.OK, jsonHeaders
                )
                "/api/v1/member-app/notification-preferences" -> respond(
                    """{"channelPushOperational":true,"channelPushReminders":true,"channelPushPromotions":false,"channelEmailOperational":true,"channelEmailPromotions":false,"channelWhatsappOperational":true,"channelWhatsappPromotions":false}""",
                    HttpStatusCode.OK, jsonHeaders
                )
                "/api/v1/app/releases/latest" -> respond(
                    """{"available":true,"platform":"ANDROID","versionCode":210,"versionName":"2.1.0","downloadUrl":"https://gymvip.pe/descargar","sha256Checksum":"abc123","fileSizeBytes":18500000,"releaseNotes":"Actualización segura","updateAvailable":true,"updateRequired":false}""",
                    HttpStatusCode.OK, jsonHeaders
                )
                else -> error("Unexpected path: ${req.url.encodedPath}")
            }
        }
        val client = GymVipHttpClient.create(engine = engine, enableLogging = false)
        try {
            val api = SaasExperienceApi(client, "https://mock.gymvip.pe")
            val gyms = api.listPublicGymsShowcase().getOrThrow()
            assertEquals(1, gyms.size)
            assertEquals(4.8, gyms.first().avgRating)

            val prefs = api.getNotificationPreferences().getOrThrow()
            assertTrue(prefs.channelPushOperational)
            assertFalse(prefs.channelPushPromotions)
            assertFalse(prefs.channelEmailPromotions)

            val rel = api.getLatestAppRelease(200).getOrThrow()
            assertTrue(rel.available)
            assertTrue(rel.updateAvailable)
            assertEquals("2.1.0", rel.versionName)
        } finally {
            client.close()
        }
    }
}
