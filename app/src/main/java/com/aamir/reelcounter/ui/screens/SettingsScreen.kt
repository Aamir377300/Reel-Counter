package com.aamir.reelcounter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamir.reelcounter.data.OverlayPosition
import com.aamir.reelcounter.ui.viewmodel.SettingsViewModel

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(factory = SettingsViewModel.factory())
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(24.dp))
        Text("Daily limit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            "Notify & vibrate once when you hit ${settings.dailyLimit} reels.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Slider(
            value = settings.dailyLimit.toFloat(),
            onValueChange = { vm.setDailyLimit(it.toInt()) },
            valueRange = 10f..500f,
            steps = 48
        )
        Text("${settings.dailyLimit} reels / day", fontWeight = FontWeight.Medium)

        Spacer(Modifier.height(24.dp))
        SettingSwitch(
            title = "Show overlay",
            subtitle = "Live pill on Reels / Shorts screens",
            checked = settings.overlayEnabled,
            onCheckedChange = vm::setOverlayEnabled
        )

        Spacer(Modifier.height(12.dp))
        Text("Overlay position", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.overlayPosition == OverlayPosition.TOP_LEFT,
                onClick = { vm.setOverlayPosition(OverlayPosition.TOP_LEFT) },
                label = { Text("Top left") }
            )
            FilterChip(
                selected = settings.overlayPosition == OverlayPosition.TOP_RIGHT,
                onClick = { vm.setOverlayPosition(OverlayPosition.TOP_RIGHT) },
                label = { Text("Top right") }
            )
        }

        Spacer(Modifier.height(24.dp))
        SettingSwitch(
            title = "Debug mode",
            subtitle = "Log view IDs with tag REEL_DEBUG (adb logcat -s REEL_DEBUG)",
            checked = settings.debugMode,
            onCheckedChange = vm::setDebugMode
        )

        Spacer(Modifier.height(32.dp))
        Text("Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { confirmReset = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Reset today") }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Clear all data") }
    }

    if (confirmReset) {
        ConfirmDialog(
            title = "Reset today's stats?",
            body = "This deletes today's reel counts, distance, and time. History for other days is kept.",
            confirmLabel = "Reset",
            onConfirm = {
                vm.resetToday()
                confirmReset = false
            },
            onDismiss = { confirmReset = false }
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear all data?",
            body = "This permanently deletes every day of history on this device.",
            confirmLabel = "Clear all",
            onConfirm = {
                vm.clearAll()
                confirmClear = false
            },
            onDismiss = { confirmClear = false }
        )
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ConfirmDialog(
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
