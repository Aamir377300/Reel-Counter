package com.aamir.reelcounter.ui.screens

import android.Manifest
import android.app.Application
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aamir.reelcounter.ui.viewmodel.SetupViewModel
import com.aamir.reelcounter.util.PermissionHelpers

@Composable
fun SetupScreen(
    onFinished: () -> Unit,
    vm: SetupViewModel = viewModel(
        factory = SetupViewModel.factory(LocalContext.current.applicationContext as Application)
    )
) {
    val checklist by vm.checklist.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { vm.refresh() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) vm.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = "Set up ReelCounter",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Finish these steps once so tracking works reliably in the background.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
        )

        Spacer(Modifier.height(24.dp))

        ChecklistItem(
            title = "1. Enable accessibility service",
            done = checklist.accessibility,
            detail = "Find \"ReelCounter\" in Accessibility settings and turn it on."
        ) {
            PermissionHelpers.openAccessibilitySettings(context)
        }

        Spacer(Modifier.height(12.dp))
        ChecklistItem(
            title = "2. Allow notifications",
            done = checklist.notifications,
            detail = "Needed for the daily-limit alert."
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                PermissionHelpers.openAppNotificationSettings(context)
            }
        }

        Spacer(Modifier.height(12.dp))
        ChecklistItem(
            title = "3. Unrestricted battery",
            done = checklist.battery,
            detail = "Stops Android from killing the tracker while Instagram/YouTube are open."
        ) {
            PermissionHelpers.openBatterySettings(context)
        }

        Spacer(Modifier.height(20.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(16.dp)
        ) {
            Text("4. Allow restricted settings (Android 13+)", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "On some phones, Accessibility for sideloaded apps is greyed out. " +
                    "Open Android Settings → Apps → ReelCounter → the ⋮ menu → " +
                    "\"Allow restricted settings\", then go back and enable the accessibility service.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { PermissionHelpers.openAppDetailsSettings(context) }) {
                Text("Open app settings")
            }
        }

        Spacer(Modifier.height(28.dp))
        Button(
            onClick = {
                vm.markSetupDone()
                onFinished()
            },
            enabled = checklist.allReady,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (checklist.allReady) "Continue" else "Complete steps above to continue")
        }

        if (!checklist.allReady) {
            Spacer(Modifier.height(8.dp))
            TextButtonLike(onClick = {
                vm.markSetupDone()
                onFinished()
            })
        }
    }
}

@Composable
private fun TextButtonLike(onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Skip for now")
    }
}

@Composable
private fun ChecklistItem(
    title: String,
    done: Boolean,
    detail: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val bg = if (done) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
            val tint = if (done) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.error
            Column(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(bg),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (done) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
            Text(if (done) "Open settings again" else "Enable")
        }
    }
}
