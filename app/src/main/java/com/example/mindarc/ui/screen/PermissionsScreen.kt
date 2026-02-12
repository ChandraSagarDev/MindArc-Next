package com.example.mindarc.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController

@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    var hasAccessibilityPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAccessibilityPermission = isAccessibilityServiceEnabled(context)
                hasOverlayPermission = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissions = listOf(
        Permission(
            "Accessibility Service",
            "Detects which app you're using to block distractions.",
            hasAccessibilityPermission,
            "android.settings.ACCESSIBILITY_SETTINGS"
        ),
        Permission(
            "Display Over Apps",
            "Shows block screens when you try to open blocked apps.",
            hasOverlayPermission,
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            true
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Permissions Required", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(permissions) { permission ->
                PermissionItem(permission = permission) {
                    if (permission.isUri) {
                        val intent = Intent(permission.action, Uri.parse("package:${context.packageName}"))
                        context.startActivity(intent)
                    } else {
                        val intent = Intent(permission.action)
                        context.startActivity(intent)
                    }
                }
            }
        }
        Button(
            onClick = {
                navController.navigate("home") {
                    popUpTo("permissions") { inclusive = true }
                }
            },
            enabled = hasAccessibilityPermission && hasOverlayPermission,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Text("Let's Go!")
        }
    }
}

@Composable
fun PermissionItem(permission: Permission, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = !permission.isGranted),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(permission.name, fontWeight = FontWeight.Bold)
                Text(permission.description, style = MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = permission.isGranted,
                onCheckedChange = null,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}

data class Permission(
    val name: String,
    val description: String,
    val isGranted: Boolean,
    val action: String,
    val isUri: Boolean = false
)

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val enabledServices = Settings.Secure.getString(
        context.contentResolver,
        Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
    ) ?: return false

    val pkg = context.packageName
    val fullComponent = "$pkg/$pkg.service.AppBlockingService"
    val shortComponent = "$pkg/.service.AppBlockingService"

    return enabledServices.contains(fullComponent) || enabledServices.contains(shortComponent)
}
