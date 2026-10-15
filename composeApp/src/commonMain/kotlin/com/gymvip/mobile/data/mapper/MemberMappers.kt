package com.gymvip.mobile.data.mapper

import com.gymvip.mobile.data.local.Member_profile
import com.gymvip.mobile.data.local.Membership_summary
import com.gymvip.mobile.data.local.Payment_history
import com.gymvip.mobile.data.remote.dto.MemberProfileDto
import com.gymvip.mobile.data.remote.dto.MembershipSummaryDto
import com.gymvip.mobile.data.remote.dto.PaymentSummaryDto
import com.gymvip.mobile.domain.model.AccountStatus
import com.gymvip.mobile.domain.model.MemberProfile
import com.gymvip.mobile.domain.model.MembershipStatus
import com.gymvip.mobile.domain.model.MembershipSummary
import com.gymvip.mobile.domain.model.MoneyPen
import com.gymvip.mobile.domain.model.PaymentMethod
import com.gymvip.mobile.domain.model.PaymentSummary

fun MemberProfileDto.toDomain(lastSyncedAt: String): MemberProfile {
    val domainStatus = when (status.uppercase()) {
        "ACTIVO" -> AccountStatus.ACTIVO
        "INACTIVO" -> AccountStatus.INACTIVO
        "BLOQUEADO" -> AccountStatus.BLOQUEADO
        else -> AccountStatus.ACTIVO
    }
    return MemberProfile(
        accountId = id,
        email = email,
        firstName = firstName,
        lastName = lastName,
        documentType = documentType,
        documentNumber = documentNumber,
        phone = phone,
        status = domainStatus,
        lastSyncedAt = lastSyncedAt
    )
}

fun Member_profile.toDomain(): MemberProfile {
    val domainStatus = when (status.uppercase()) {
        "ACTIVO" -> AccountStatus.ACTIVO
        "INACTIVO" -> AccountStatus.INACTIVO
        "BLOQUEADO" -> AccountStatus.BLOQUEADO
        else -> AccountStatus.ACTIVO
    }
    return MemberProfile(
        accountId = accountId,
        email = email,
        firstName = firstName,
        lastName = lastName,
        documentType = documentType,
        documentNumber = documentNumber,
        phone = phone,
        status = domainStatus,
        lastSyncedAt = lastSyncedAt
    )
}

fun MembershipSummaryDto.toDomain(accountId: String, lastSyncedAt: String): MembershipSummary {
    val domainStatus = when (status.uppercase()) {
        "VIGENTE" -> MembershipStatus.VIGENTE
        "PROXIMA_A_VENCER" -> MembershipStatus.PROXIMA_A_VENCER
        "VENCIDA" -> MembershipStatus.VENCIDA
        else -> MembershipStatus.SIN_MEMBRESIA
    }
    return MembershipSummary(
        id = id,
        accountId = accountId,
        planName = planName,
        startDate = startDate,
        endDate = endDate,
        status = domainStatus,
        daysRemaining = daysRemaining,
        lastSyncedAt = lastSyncedAt
    )
}

fun Membership_summary.toDomain(): MembershipSummary {
    val domainStatus = when (status.uppercase()) {
        "VIGENTE" -> MembershipStatus.VIGENTE
        "PROXIMA_A_VENCER" -> MembershipStatus.PROXIMA_A_VENCER
        "VENCIDA" -> MembershipStatus.VENCIDA
        else -> MembershipStatus.SIN_MEMBRESIA
    }
    return MembershipSummary(
        id = id,
        accountId = accountId,
        planName = planName,
        startDate = startDate,
        endDate = endDate,
        status = domainStatus,
        daysRemaining = daysRemaining.toInt(),
        lastSyncedAt = lastSyncedAt
    )
}

fun PaymentSummaryDto.toDomain(accountId: String, lastSyncedAt: String): PaymentSummary {
    val method = try {
        PaymentMethod.valueOf(paymentMethod.uppercase())
    } catch (_: Exception) {
        PaymentMethod.OTRO
    }
    return PaymentSummary(
        id = id,
        accountId = accountId,
        membershipId = membershipId,
        amount = MoneyPen.fromDouble(amountPen),
        paymentMethod = method,
        externalReference = externalReference,
        paidAt = paidAt,
        notes = notes,
        lastSyncedAt = lastSyncedAt
    )
}

fun Payment_history.toDomain(): PaymentSummary {
    val method = try {
        PaymentMethod.valueOf(paymentMethod.uppercase())
    } catch (_: Exception) {
        PaymentMethod.OTRO
    }
    return PaymentSummary(
        id = id,
        accountId = accountId,
        membershipId = membershipId,
        amount = MoneyPen(amountCents),
        paymentMethod = method,
        externalReference = externalReference,
        paidAt = paidAt,
        notes = notes,
        lastSyncedAt = lastSyncedAt
    )
}
