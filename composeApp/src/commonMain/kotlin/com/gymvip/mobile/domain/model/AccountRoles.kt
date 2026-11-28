package com.gymvip.mobile.domain.model

/** Nombres canónicos de roles para login, autoridades Spring y sesiones persistidas. */
fun Iterable<String>.normalizedAccountRoles(): List<String> =
    map { it.trim().uppercase().removePrefix("ROLE_") }.filter(String::isNotBlank).distinct()

fun Iterable<String>.accountRoleLabel(): String = when (val role = normalizedAccountRoles().firstOrNull()) {
    "SUPER_ADMIN" -> "Administrador de plataforma"
    "SOPORTE_PLATAFORMA" -> "Soporte de plataforma"
    "ADMIN", "ADMIN_EMPRESA" -> "Administrador de empresa"
    "GERENTE", "GERENTE_GENERAL" -> "Gerente general"
    "GERENTE_SEDE" -> "Responsable de sede"
    "RECEPCION" -> "Colaborador"
    "ENTRENADOR", "ENTRENADOR_JEFE" -> "Entrenador"
    "AUDITOR", "AUDITOR_EMPRESA" -> "Auditor de empresa"
    "CONTADOR" -> "Contabilidad"
    "RECURSOS_HUMANOS" -> "Recursos humanos"
    "CLIENTE" -> "Socio titular"
    null -> "Cuenta GYM VIP"
    else -> role.replace('_', ' ')
}
