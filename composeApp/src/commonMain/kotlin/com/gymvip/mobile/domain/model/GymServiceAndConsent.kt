package com.gymvip.mobile.domain.model

data class GymService(
    val id: String,
    val name: String,
    val description: String,
    val schedule: String,
    val location: String,
    val isIncluded: Boolean = true
)

data class MemberConsent(
    val accountId: String,
    val termsAccepted: Boolean,
    val privacyAcknowledged: Boolean,
    val whatsappConsent: Boolean,
    val documentVersion: String = "v1.0",
    val acceptedAt: String? = null
)

