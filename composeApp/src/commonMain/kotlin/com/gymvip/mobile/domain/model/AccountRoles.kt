package com.gymvip.mobile.domain.model

/** Nombres canónicos de roles para login, autoridades Spring y sesiones persistidas. */
fun Iterable<String>.normalizedAccountRoles(): List<String> =
    map { it.trim().uppercase().removePrefix("ROLE_") }.filter(String::isNotBlank).distinct()
