package com.gymvip.mobile.domain.model

/** Canonical role names for login, Spring authorities and persisted sessions. */
fun Iterable<String>.normalizedAccountRoles(): List<String> =
    map { it.trim().uppercase().removePrefix("ROLE_") }.filter(String::isNotBlank).distinct()
