package com.gymvip.mobile.presentation.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gymvip.mobile.presentation.theme.GoldPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VipTopAppBar(
    title: String,
    onOpenDrawer: () -> Unit,
    onSyncClick: (() -> Unit)? = null,
    isSyncing: Boolean = false
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GoldPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "V",
                        color = Color.Black,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Text(
                    text = "  $title",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp)) {
                Text(text = "☰", style = MaterialTheme.typography.titleLarge, color = GoldPrimary)
            }
        },
        actions = {
            if (onSyncClick != null) {
                IconButton(onClick = onSyncClick, modifier = Modifier.size(48.dp)) {
                    Text(
                        text = if (isSyncing) "⏳" else "🔄",
                        style = MaterialTheme.typography.titleMedium,
                        color = GoldPrimary
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
