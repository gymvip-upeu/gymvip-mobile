package pe.edu.upeu.gymvip.data.mapper

import pe.edu.upeu.gymvip.data.local.Member_profile
import pe.edu.upeu.gymvip.data.local.Membership_summary
import pe.edu.upeu.gymvip.data.local.Payment_history
import pe.edu.upeu.gymvip.data.remote.dto.MemberProfileDto
import pe.edu.upeu.gymvip.data.remote.dto.MembershipSummaryDto
import pe.edu.upeu.gymvip.data.remote.dto.PaymentSummaryDto
import pe.edu.upeu.gymvip.domain.model.AccountStatus
import pe.edu.upeu.gymvip.domain.model.MemberProfile
import pe.edu.upeu.gymvip.domain.model.MembershipStatus
import pe.edu.upeu.gymvip.domain.model.MembershipSummary
import pe.edu.upeu.gymvip.domain.model.MoneyPen
import pe.edu.upeu.gymvip.domain.model.PaymentMethod
import pe.edu.upeu.gymvip.domain.model.PaymentSummary

fun MemberProfileDto.toDomain(lastSyncedAt: String): MemberProfile {
    val domainStatus = when (status.uppercase()) {
        "ACTIVO" -> AccountStatus.ACTIVO
        "INACTIVO" -> AccountStatus.INACTIVO
        "BLOQUEADO" -> AccountStatus.BLOQUEADO
        else -> AccountStatus.BLOQUEADO
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
        lastSyncedAt = lastSyncedAt,
        memberCode = memberCode
    )
}

fun Member_profile.toDomain(): MemberProfile {
    val domainStatus = when (status.uppercase()) {
        "ACTIVO" -> AccountStatus.ACTIVO
        "INACTIVO" -> AccountStatus.INACTIVO
        "BLOQUEADO" -> AccountStatus.BLOQUEADO
        else -> AccountStatus.BLOQUEADO
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
    val domainStatus = estadoMembresia(status)
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
    val domainStatus = estadoMembresia(status)
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
