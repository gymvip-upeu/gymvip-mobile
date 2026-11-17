package com.gymvip.mobile.presentation.navigation

import androidx.compose.runtime.Composable

@Composable
internal expect fun AppBackHandler(enabled: Boolean, onBack: () -> Unit)
