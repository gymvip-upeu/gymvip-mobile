package pe.edu.upeu.gymvip.data.remote.dto

import kotlinx.serialization.Serializable
import pe.edu.upeu.gymvip.domain.model.*

@Serializable data class DocumentoGestionDto(val countryCode: String = "PE", val documentType: String = "DNI",
    val documentNumber: String = "", val isPrimary: Boolean = false)
@Serializable data class SocioGestionDto(val id: String, val memberCode: String = "", val firstName: String = "",
    val lastName: String = "", val birthDate: String = "", val phone: String? = null, val email: String? = null,
    val status: String = "", val documents: List<DocumentoGestionDto> = emptyList(), val version: Long = 0, val notes: String? = null) {
    fun dominio(): SocioGestion {
        val doc = documents.firstOrNull { it.isPrimary } ?: documents.firstOrNull()
        return SocioGestion(id, memberCode, firstName, lastName, birthDate, phone.orEmpty(), email.orEmpty(), status,
            doc?.countryCode ?: "PE", doc?.documentType ?: "DNI", doc?.documentNumber.orEmpty(), version, notes.orEmpty())
    }
}
@Serializable data class PaginaSociosDto(val content: List<SocioGestionDto> = emptyList(), val number: Int = 0,
    val totalPages: Int = 0, val totalElements: Long = 0)
@Serializable data class TotalesGestionDto(val totalElements: Long = 0)
@Serializable data class GuardarSocioDto(val firstName: String, val lastName: String, val countryCode: String,
    val documentType: String, val documentNumber: String, val phone: String?, val email: String?,
    val birthDate: String? = null, val version: Long? = null, val notes: String? = null)
@Serializable data class PlanGestionDto(val id: String, val name: String, val description: String? = null,
    val durationDays: Int, val pricePen: Double, val benefits: String? = null, val conditions: String? = null, val status: String) {
    fun dominio() = PlanGestion(id, name, description.orEmpty(), durationDays, pricePen, benefits.orEmpty(), conditions.orEmpty(), status)
}
@Serializable data class GuardarPlanDto(val name: String, val description: String, val durationDays: Int,
    val pricePen: Double, val benefits: String, val conditions: String)
@Serializable data class CuentaGestionDto(val id: String, val email: String, val roles: List<String>, val status: String)
@Serializable data class CoberturaGestionDto(val state: String, val planName: String? = null,
    val lastIncludedDate: String? = null, val daysRemaining: Long? = null, val allowed: Boolean, val reason: String? = null)
@Serializable data class AsistenciaGestionDto(val message: String)
