package com.gymvip.mobile.presentation.screen.faq

import com.gymvip.mobile.domain.model.Faq
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FaqScreenTest {
    private val hours = Faq("1", "¿Cuál es el horario?", "Sábados desde las 7.", "Gimnasio", 1)
    private val supplements = Faq("2", "¿Hay proteína?", "Consulta el catálogo.", "Suplementos", 2)
    private val faqs = listOf(hours, supplements)

    @Test
    fun searchesAcrossQuestionAndAnswerWithoutAccents() {
        assertEquals(listOf(hours), filterFaqs(faqs, "HORARIO  sabados", null))
    }

    @Test
    fun combinesSearchWithCategory() {
        assertEquals(emptyList(), filterFaqs(faqs, "proteina", "Gimnasio"))
        assertEquals(listOf(supplements), filterFaqs(faqs, "  ", "Suplementos"))
    }

    @Test
    fun formatsInternationalWhatsAppNumber() {
        assertEquals("https://wa.me/12025550123", receptionWhatsAppUrl("+1 (202) 555-0123"))
        assertNull(receptionWhatsAppUrl("Recepción"))
    }
}
