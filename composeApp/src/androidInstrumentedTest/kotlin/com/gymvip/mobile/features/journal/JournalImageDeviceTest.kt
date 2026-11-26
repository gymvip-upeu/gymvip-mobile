package com.gymvip.mobile.features.journal

import android.graphics.BitmapFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Test
import org.junit.Assert.*
import com.gymvip.mobile.di.AppContainer
import com.gymvip.mobile.data.remote.api.GymVipHttpClient
import io.ktor.client.request.get
import io.ktor.client.call.body
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

class JournalImageDeviceTest {
    @Test fun excessiveContentFailsExplicitlyInsteadOfExportingTruncatedData() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        try {
            renderJournal(context, JournalShareCard("2026-11-26", List(100) { "Texto muy largo ".repeat(30) }, "Estadísticas", false, null))
            fail("La exportación no debe ocultar contenido seleccionado")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Reduce el contenido"))
        }
    }

    @Test fun cancelledRenderStopsAndAllowsNextImage() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val card = JournalShareCard("2026-11-26", listOf("45 minutos"), "Minimal", true, null)
        try {
            renderJournal(context, card) { throw kotlinx.coroutines.CancellationException("Cancelado para prueba") }
            fail("Debe propagarse la cancelación")
        } catch (_: kotlinx.coroutines.CancellationException) { }
        assertTrue(renderJournal(context, card).isNotEmpty())
    }
    /** Opt-in para renovar únicamente los roles cacheados de la sesión local usando /auth/me real. */
    @Test fun prepareExistingMemberForManualReviewWhenRequested() = runBlocking {
        org.junit.Assume.assumeTrue(InstrumentationRegistry.getArguments().getString("journal.prepareMember") == "true")
        val store = AppContainer.secureTokenStore
        assertNotNull("Se necesita una sesión existente", store.getAccessToken())
        val client = GymVipHttpClient.create(tokenStore = store)
        try {
            val response = client.get("${GymVipHttpClient.baseUrl}/api/v1/auth/me")
            assertEquals(200, response.status.value)
            val body = response.body<JsonObject>()
            val roles = body["roles"]!!.jsonArray.map { item -> item.jsonObject["authority"]!!.jsonPrimitive.content.removePrefix("ROLE_") }
            assertTrue("La sesión debe pertenecer a un socio real", "CLIENTE" in roles)
            store.saveAccountRoles(roles)
        } finally { client.close() }
    }
    @Test fun allTemplatesProduceValidHighResolutionPng() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (story in listOf(true, false)) for (template in listOf("Minimal", "Bold", "Estadísticas")) {
            val bytes = renderJournal(context, JournalShareCard("2026-11-26", listOf("45 min de entrenamiento", "Fuerza · Piernas", "Energía 4/5 · Esfuerzo 3/5", "Racha: 2 días"), template, story, null))
            assertTrue(bytes.size > 1000)
            val bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.size)
            assertNotNull(bitmap); assertEquals(1080,bitmap.width); assertEquals(if (story) 1920 else 1080,bitmap.height)
            bitmap.recycle()
        }
    }
}
