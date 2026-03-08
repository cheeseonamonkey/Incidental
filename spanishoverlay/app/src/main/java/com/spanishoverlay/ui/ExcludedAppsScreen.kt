package com.spanishoverlay.ui

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExcludedAppsScreen(
    initialPackages: List<String>,
    packageManager: PackageManager,
    onPackagesChanged: (List<String>) -> Unit
) {
    var packages by remember { mutableStateOf(initialPackages) }
    var showPicker by remember { mutableStateOf(false) }

    val labels by produceState<Map<String, String>>(emptyMap(), packages) {
        value = withContext(Dispatchers.IO) {
            packages.associateWith { pkg ->
                runCatching {
                    packageManager.getApplicationLabel(
                        packageManager.getApplicationInfo(pkg, 0)
                    ).toString()
                }.getOrDefault(pkg)
            }
        }
    }

    Column {
        TextButton(onClick = { showPicker = true }) {
            Icon(Icons.Default.Add, null); Spacer(Modifier.width(4.dp)); Text("Exclude an app")
        }
        LazyColumn(Modifier.fillMaxWidth()) {
            items(packages, key = { it }) { pkg ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(start = 4.dp)) {
                        Text(labels[pkg] ?: pkg, style = MaterialTheme.typography.bodyMedium)
                        Text(pkg, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = { packages = packages - pkg; onPackagesChanged(packages) }) {
                        Icon(Icons.Default.Close, "Remove", tint = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider()
            }
        }
    }

    if (showPicker) {
        InstalledAppPickerDialog(packageManager, packages.toSet(),
            onSelect = { pkg -> packages = packages + pkg; onPackagesChanged(packages); showPicker = false },
            onDismiss = { showPicker = false })
    }
}

@Composable
fun InstalledAppPickerDialog(
    packageManager: PackageManager, excluded: Set<String>,
    onSelect: (String) -> Unit, onDismiss: () -> Unit
) {
    val apps by produceState<List<Pair<String, String>>>(emptyList()) {
        value = withContext(Dispatchers.IO) {
            packageManager.getInstalledApplications(0)
                .filter { it.packageName !in excluded }
                .map { packageManager.getApplicationLabel(it).toString() to it.packageName }
                .sortedBy { it.first }
        }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text("Select app to exclude") },
        text = {
            if (apps.isEmpty()) Box(Modifier.fillMaxWidth(), Alignment.Center) { CircularProgressIndicator() }
            else LazyColumn(Modifier.fillMaxWidth().heightIn(max = 400.dp)) {
                items(apps, key = { it.second }) { (label, pkg) ->
                    TextButton(onClick = { onSelect(pkg) }, Modifier.fillMaxWidth()) {
                        Text(label, Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
