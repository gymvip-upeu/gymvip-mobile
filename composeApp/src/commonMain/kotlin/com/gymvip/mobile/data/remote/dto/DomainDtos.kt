package com.gymvip.mobile.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberProfileDto(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val documentType: String,
    val documentNumber: String,
    val phone: String? = null,
    val status: String = "ACTIVO"
)

@Serializable
data class MembershipSummaryDto(
    val id: String,
    val clientId: String? = null,
    val planName: String,
    val startDate: String,
    val endDate: String,
    val status: String,
    val daysRemaining: Int = 0
)

@Serializable
data class PaymentSummaryDto(
    val id: String,
    val membershipId: String? = null,
    val amountPen: Double,
    val paymentMethod: String,
    val externalReference: String? = null,
    val paidAt: String,
    val notes: String? = null
)

@Serializable
data class ProductDto(
    val id: String,
    val name: String,
    val description: String,
    val category: String,
    val pricePen: Double,
    val isPromotion: Boolean = false,
    val promotionDiscountPercent: Int? = null,
    val imageUrl: String? = null
)

@Serializable
data class FavoriteRequestDto(
    val productId: String,
    val personalNote: String = "",
    val productTitle: String = "",
    val productCategory: String = "SUPLEMENTO",
    val notes: String = ""
)

@Serializable
data class FavoriteUpdateNoteDto(
    val personalNote: String = "",
    val notes: String = ""
)

@Serializable
data class FavoriteResponseDto(
    val id: String,
    val accountId: String? = null,
    val productId: String = "",
    val productName: String = "",
    val productCategory: String = "",
    val pricePen: Double = 0.0,
    val personalNote: String = "",
    val notes: String? = null,
    val createdAt: String = "",
    val updatedAt: String = ""
)

// ==============================================================================
// DTOs para Conciliación con Backend (MemberAppController: /api/v1/member-app/*)
// ==============================================================================

@Serializable
data class MemberClientDocumentDto(
    val id: String? = null,
    val countryCode: String? = null,
    val documentType: String? = null,
    val documentNumber: String? = null,
    val isPrimary: Boolean = true
)

@Serializable
data class MemberClientDto(
    val id: String,
    val memberCode: String? = null,
    val firstName: String = "",
    val lastName: String = "",
    val fullName: String? = null,
    val phone: String? = null,
    val email: String = "",
    val status: String = "ACTIVO",
    val documents: List<MemberClientDocumentDto> = emptyList(),
    val createdAt: String? = null
)

@Serializable
data class MemberCoverageDto(
    val clientId: String? = null,
    val memberCode: String? = null,
    val fullName: String? = null,
    val document: String? = null,
    val clientStatus: String? = null,
    val contractStatus: String? = null,
    val state: String? = null,
    val allowed: Boolean = false,
    val reason: String? = null,
    val planId: String? = null,
    val planName: String? = null,
    val startDate: String? = null,
    val lastIncludedDate: String? = null,
    val endExclusive: String? = null,
    val daysRemaining: Long = 0,
    val timezone: String? = "America/Lima",
    val upcomingDays: Int = 7
)

@Serializable
data class MemberPeriodDto(
    val id: String,
    val plan_name: String? = null,
    val duration_days: Int = 30,
    val start_date: String? = null,
    val end_date: String? = null,
    val price_pen: Double? = null,
    val benefits: String? = null,
    val conditions: String? = null,
    val paid: Boolean = false
)

@Serializable
data class MemberPaymentDto(
    val id: String,
    val period_id: String? = null,
    val plan_name: String? = null,
    val amount_pen: Double = 0.0,
    val payment_method: String = "EFECTIVO",
    val external_reference: String? = null,
    val notes: String? = null,
    val payment_timestamp_utc: String? = null
)

@Serializable
data class MemberAttendanceDto(
    val id: String,
    val attendance_date: String,
    val checked_at_utc: String? = null,
    val recorded_by: String? = null
)

@Serializable
data class MemberWorkspaceDto(
    val client: MemberClientDto? = null,
    val notes: String? = null,
    val hasAppAccess: Boolean = false,
    val coverage: MemberCoverageDto? = null,
    val periods: List<MemberPeriodDto> = emptyList(),
    val payments: List<MemberPaymentDto> = emptyList(),
    val attendance: List<MemberAttendanceDto> = emptyList(),
    val attendanceCount: Long = 0,
    val totalPaidPen: Double = 0.0
) {
    fun toProfileDto(): MemberProfileDto {
        val c = client ?: MemberClientDto(id = "c_unknown")
        val primaryDoc = c.documents.firstOrNull { it.isPrimary } ?: c.documents.firstOrNull()
        return MemberProfileDto(
            id = c.id,
            email = c.email,
            firstName = c.firstName,
            lastName = c.lastName,
            documentType = primaryDoc?.documentType ?: "DNI",
            documentNumber = primaryDoc?.documentNumber ?: c.memberCode ?: "",
            phone = c.phone,
            status = c.status
        )
    }

    fun toMembershipDto(): MembershipSummaryDto {
        val cov = coverage
        val firstPeriod = periods.firstOrNull()
        val pName = cov?.planName ?: firstPeriod?.plan_name ?: "Sin Plan"
        val start = cov?.startDate ?: firstPeriod?.start_date ?: ""
        val end = cov?.lastIncludedDate ?: firstPeriod?.end_date ?: ""
        val stat = cov?.state ?: cov?.contractStatus ?: "SIN_MEMBRESIA"
        val rem = cov?.daysRemaining?.toInt() ?: 0
        return MembershipSummaryDto(
            id = firstPeriod?.id ?: "m_none",
            clientId = client?.id,
            planName = pName,
            startDate = start,
            endDate = end,
            status = stat,
            daysRemaining = rem
        )
    }

    fun toPaymentsDto(): List<PaymentSummaryDto> {
        return payments.map { p ->
            PaymentSummaryDto(
                id = p.id,
                membershipId = p.period_id,
                amountPen = p.amount_pen,
                paymentMethod = p.payment_method,
                externalReference = p.external_reference,
                paidAt = p.payment_timestamp_utc ?: "",
                notes = p.notes
            )
        }
    }
}

@Serializable
data class DynamicQrResponseDto(
    val token: String,
    val memberCode: String,
    val memberName: String,
    val expiresAtUtc: String,
    val ttlSeconds: Int = 60,
    val disclaimer: String = "No es autorización de ingreso. Presente este código en mostrador para verificación."
)

@Serializable
data class MemberSessionStatusDto(
    val mustChangePassword: Boolean = false
)

@Serializable
data class PasswordChangeRequestDto(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class PasswordChangeResponseDto(
    val changed: Boolean = false,
    val requiresLogin: Boolean = true
)

@Serializable
data class FaqDto(
    val id: String,
    val question: String,
    val answer: String,
    val category: String,
    val orderIndex: Int = 0
)

@Serializable
data class InboxItemDto(
    val id: String,
    val accountId: String? = null,
    val title: String,
    val message: String,
    val category: String = "COMUNICADO",
    val createdAt: String,
    val isRead: Boolean = false
)

@Serializable
data class GymServiceDto(
    val id: String,
    val name: String,
    val description: String,
    val schedule: String,
    val location: String,
    val isIncluded: Boolean = true
)

@Serializable
data class ConsentStatusDto(
    val termsAccepted: Boolean = false,
    val privacyAcknowledged: Boolean = false,
    val whatsappConsent: Boolean = false,
    val documentVersion: String = "v1.0",
    val acceptedAt: String? = null
)

@Serializable
data class ConsentSubmitRequestDto(
    val termsAccepted: Boolean,
    val privacyAcknowledged: Boolean,
    val whatsappConsent: Boolean = false,
    val documentVersion: String = "v1.0"
)
