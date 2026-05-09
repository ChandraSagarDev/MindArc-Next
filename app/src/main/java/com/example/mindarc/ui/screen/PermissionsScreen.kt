package com.example.mindarc.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import com.example.mindarc.R
import com.example.mindarc.ui.components.GlassCard
import com.example.mindarc.ui.components.MindArcPrimaryButton
import kotlinx.coroutines.launch

@Composable
fun PermissionsScreen(navController: NavController) {
    val context = LocalContext.current
    var hasAccessibilityPermission by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasHealthConnectPermission by remember { mutableStateOf(false) }

    val healthPermissions = remember {
        setOf(
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        )
    }

    val coroutineScope = rememberCoroutineScope()

    var healthConnectClient by remember { mutableStateOf<HealthConnectClient?>(null) }
    var isHealthConnectAvailable by remember { mutableStateOf(false) }
    var isHealthConnectUpdateRequired by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        // Health Connect provider package name as per official docs
        val providerPackageName = "com.google.android.apps.healthdata"
        when (HealthConnectClient.getSdkStatus(context, providerPackageName)) {
            HealthConnectClient.SDK_AVAILABLE -> {
                isHealthConnectAvailable = true
                val client = HealthConnectClient.getOrCreate(context)
                healthConnectClient = client
                val granted = client.permissionController.getGrantedPermissions()
                hasHealthConnectPermission = granted.containsAll(healthPermissions)
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                isHealthConnectAvailable = false
                isHealthConnectUpdateRequired = true
            }
            else -> {
                isHealthConnectAvailable = false
                isHealthConnectUpdateRequired = false
            }
        }
    }

    val healthConnectPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        hasHealthConnectPermission = granted.containsAll(healthPermissions)
    }

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

            item {
                Spacer(modifier = Modifier.height(8.dp))
                GlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    containerAlpha = 0.45f,
                    borderAlpha = 0.25f,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                contentDescription = "Health Connect logo",
                                tint = Color.Unspecified,
                                modifier = Modifier
                                    .size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Health Connect (optional)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "During push-up and squat workouts, MindArc can show your live heart rate, calories burned, and heart rate variability by reading them from Health Connect. " +
                                    "This is optional and uses read-only access: MindArc does not write health data or share it with other apps.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val client = healthConnectClient

                                    if (client == null || !isHealthConnectAvailable) {
                                        // Health Connect not available; if an update is required,
                                        // send the user to the Play Store, otherwise do nothing.
                                        if (isHealthConnectUpdateRequired) {
                                            val uriString =
                                                "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                `package` = "com.android.vending"
                                                data = Uri.parse(uriString)
                                                putExtra("overlay", true)
                                                putExtra("callerId", context.packageName)
                                            }
                                            context.startActivity(intent)
                                        }
                                    } else {
                                        val granted = client.permissionController.getGrantedPermissions()
                                        if (!granted.containsAll(healthPermissions)) {
                                            healthConnectPermissionLauncher.launch(healthPermissions)
                                        }
                                    }
                                }
                            },
                            enabled = !hasHealthConnectPermission
                        ) {
                            Text(if (hasHealthConnectPermission) "Health Connect Connected" else "Connect Health Connect")
                        }
                    }
                }
            }
        }

        MindArcPrimaryButton(
            hero = true,
            text = "Let's Go!",
            enabled = hasAccessibilityPermission && hasOverlayPermission,
            modifier = Modifier
                .fillMaxWidth(),
            height = 56.dp,
            onClick = {
                navController.navigate("home") {
                    popUpTo("permissions") { inclusive = true }
                }
            }
        )
    }
}

@Composable
fun PermissionItem(permission: Permission, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick, enabled = !permission.isGranted),
        shape = RoundedCornerShape(16.dp),
        containerAlpha = 0.50f,
        borderAlpha = 0.25f,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
    ) {
        Row(
            modifier = Modifier
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
