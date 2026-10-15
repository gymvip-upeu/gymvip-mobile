package com.gymvip.mobile.presentation.screen.membership

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.component.EmptyView
import com.gymvip.mobile.presentation.component.LoadingView
import com.gymvip.mobile.presentation.component.MembershipStatusCard
import com.gymvip.mobile.presentation.component.OfflineBanner
import com.gymvip.mobile.presentation.component.PaymentItemCard
import com.gymvip.mobile.presentation.theme.GoldPrimary
import com.gymvip.mobile.presentation.viewmodel.MembershipViewModel

@Composable
fun MembershipScreen(viewModel: MembershipViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        OfflineBanner(isOffline = state.isOffline)

        TabRow(
            selectedTabIndex = state.selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = GoldPrimary,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[state.selectedSubTab]),
                    color = GoldPrimary
                )
            }
        ) {
            Tab(
                selected = state.selectedSubTab == 0,
                onClick = { viewModel.setSubTab(0) },
                text = { Text("Vigencia Actual", style = MaterialTheme.typography.titleSmall) }
            )
            Tab(
                selected = state.selectedSubTab == 1,
                onClick = { viewModel.setSubTab(1) },
                text = { Text("Historial de Pagos (PEN)", style = MaterialTheme.typography.titleSmall) }
            )
        }

        if (state.isLoading && state.membership == null && state.payments.isEmpty()) {
            LoadingView("Consultando datos de membresía...")
        } else {
            when (state.selectedSubTab) {
                0 -> MembershipDetailTab(state = state)
                1 -> PaymentHistoryTab(state = state)
            }
        }
    }
}

@Composable
private fun MembershipDetailTab(state: com.gymvip.mobile.presentation.viewmodel.MembershipUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        MembershipStatusCard(membership = state.membership)

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Reglamento y Condiciones de Membresía",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Cada mensualidad otorga exactamente 30 días calendario continuos de acceso.\n" +
                            "• No se aplican días de gracia ni extensiones automáticas (cero tolerancia).\n" +
                            "• Las renovaciones y pagos se tramitan exclusivamente de forma presencial en recepción.\n" +
                            "• Su historial permanece accesible en todo momento para su control contable.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PaymentHistoryTab(state: com.gymvip.mobile.presentation.viewmodel.MembershipUiState) {
    if (state.payments.isEmpty()) {
        EmptyView(
            icon = "💳",
            title = "Sin pagos registrados",
            message = "No se encontraron recibos de pago en su cuenta de socio."
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(state.payments, key = { it.id }) { payment ->
                PaymentItemCard(payment = payment)
            }
        }
    }
}
