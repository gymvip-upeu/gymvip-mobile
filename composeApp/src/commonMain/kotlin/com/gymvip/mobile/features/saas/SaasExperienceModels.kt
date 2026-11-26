package com.gymvip.mobile.features.saas

import com.gymvip.mobile.features.training.PublishedBranchDto
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

@Serializable
data class PublicGymShowcaseDto(
    val id: String,
    val code: String = "",
    val name: String,
    val description: String? = null,
    val planTier: String = "ENTERPRISE",
    val slug: String? = null,
    val headline: String? = null,
    val tagline: String? = null,
    val aboutText: String? = null,
    val contactWhatsapp: String? = null,
    val instagramHandle: String? = null,
    val minPricePen: Double? = null,
    val maxPricePen: Double? = null,
    val reviewsCount: Long = 0,
    val avgRating: Double = 0.0,
    val activeGroupPromosCount: Long = 0,
    val branches: List<PublishedBranchDto> = emptyList()
)

@Serializable
data class GymReviewDto(
    val id: String,
    val organizationId: String,
    val ratingOverall: Int,
    val ratingCleanliness: Int,
    val ratingEquipment: Int,
    val ratingAttention: Int,
    val ratingValue: Int,
    val comment: String,
    val verifiedEligibilitySource: String = "",
    val gymReply: String? = null,
    val createdAtUtc: String = ""
)

@Serializable
data class CreateGymReviewRequest(
    val branchId: String? = null,
    val ratingOverall: Int,
    val ratingCleanliness: Int,
    val ratingEquipment: Int,
    val ratingAttention: Int,
    val ratingValue: Int,
    val comment: String
)

@Serializable
data class GroupPromotionDto(
    val id: String,
    val organizationId: String,
    val organizationName: String? = null,
    val title: String,
    val description: String? = null,
    val minParticipants: Int = 2,
    val maxParticipants: Int = 4,
    val discountPercent: Double = 15.0,
    val validFrom: String = "",
    val validUntil: String = "",
    val status: String = "ACTIVA",
    val terms: String? = null
)

@Serializable
data class GroupEnrollmentMemberDto(
    val invitedEmail: String,
    val memberRole: String,
    val status: String,
    val acceptedConsent: Boolean = false
)

@Serializable
data class GroupEnrollmentDto(
    val id: String,
    val promotionId: String,
    val organizationId: String,
    val promotionTitle: String? = null,
    val invitationCode: String,
    val status: String,
    val discountPercent: Double = 0.0,
    val basePricePerPersonPen: Double = 0.0,
    val discountedPricePerPersonPen: Double = 0.0,
    val expiresAtUtc: String = "",
    val members: List<GroupEnrollmentMemberDto> = emptyList()
)

@Serializable
data class CreateGroupEnrollmentRequest(
    val promotionId: String,
    val referenceBasePricePen: Double? = null,
    val invitedEmails: List<String> = emptyList()
)

@Serializable
data class JoinGroupEnrollmentRequest(
    val acceptConsent: Boolean = true
)

@Serializable
data class PhysicalProfileDto(
    val configured: Boolean = false,
    val heightCm: Double? = null,
    val weightKg: Double? = null,
    val primaryGoal: String? = null,
    val experienceLevel: String? = null,
    val weeklyAvailabilityDays: Int? = null,
    val injuriesOrNotes: String? = null,
    val bmiValue: Double? = null,
    val bmiClassification: String? = null,
    val bmiDisclaimer: String = EducationalBmiCalculator.DISCLAIMER
)

@Serializable
data class SavePhysicalProfileRequest(
    val heightCm: Double?,
    val weightKg: Double?,
    val primaryGoal: String?,
    val experienceLevel: String?,
    val weeklyAvailabilityDays: Int?,
    val injuriesOrNotes: String? = null,
    val consentSaved: Boolean = true
)

@Serializable
data class WeeklyTrainingPlanDto(
    val id: String = "",
    val title: String,
    val goal: String,
    val level: String = "PRINCIPIANTE",
    val weekStartDate: String? = null,
    val daysJson: String = "[]",
    val isActive: Boolean = true
)

@Serializable
data class SaveWeeklyPlanRequest(
    val id: String? = null,
    val title: String,
    val goal: String,
    val level: String = "PRINCIPIANTE",
    val daysJson: String
)

@Serializable
data class NotificationPreferencesDto(
    val channelPushOperational: Boolean = true,
    val channelPushReminders: Boolean = true,
    val channelPushPromotions: Boolean = false,
    val channelEmailOperational: Boolean = true,
    val channelEmailPromotions: Boolean = false,
    val channelWhatsappOperational: Boolean = true,
    val channelWhatsappPromotions: Boolean = false
)

@Serializable
data class AppReleaseManifestDto(
    val available: Boolean = false,
    val platform: String = "ANDROID",
    val versionCode: Int = 200,
    val versionName: String = "2.0.0",
    val downloadUrl: String = "",
    val sha256Checksum: String = "",
    val fileSizeBytes: Long = 0,
    val releaseNotes: String = "",
    val updateAvailable: Boolean = false,
    val updateRequired: Boolean = false
)

data class EducationalBmiEvaluation(
    val bmi: Double,
    val categoryLabel: String,
    val disclaimer: String
)

object EducationalBmiCalculator {
    const val DISCLAIMER =
        "Indicador referencial e informativo OMS. No distingue masa muscular de grasa corporal ni constituye diagnóstico médico o nutricional."

    fun evaluate(heightCm: Double?, weightKg: Double?): EducationalBmiEvaluation? {
        if (heightCm == null || weightKg == null) return null
        require(heightCm in 100.0..250.0) { "La estatura debe ubicarse entre 100 cm y 250 cm." }
        require(weightKg in 30.0..300.0) { "El peso debe ubicarse entre 30 kg y 300 kg." }
        val heightM = heightCm / 100.0
        val raw = weightKg / (heightM * heightM)
        val rounded = (raw * 100.0).roundToInt() / 100.0
        val label = when {
            rounded < 18.5 -> "Bajo peso (referencia OMS)"
            rounded < 25.0 -> "Peso saludable (referencia OMS)"
            rounded < 30.0 -> "Sobrepeso (referencia OMS)"
            rounded < 35.0 -> "Obesidad grado I (referencia OMS)"
            rounded < 40.0 -> "Obesidad grado II (referencia OMS)"
            else -> "Obesidad grado III (referencia OMS)"
        }
        return EducationalBmiEvaluation(bmi = rounded, categoryLabel = label, disclaimer = DISCLAIMER)
    }
}
