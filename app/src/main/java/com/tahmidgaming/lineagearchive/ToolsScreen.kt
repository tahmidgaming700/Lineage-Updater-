package com.tahmidgaming.lineagearchive

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

@Composable
fun ToolsScreen(
    context: android.content.Context,
    capability: FlashManager.Capability,
    downloads: List<DownloadStore.Item>,
    gapps: List<AddonDownload>,
    magisk: List<AddonDownload>,
    gsis: List<AddonDownload>,
    loading: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onCapabilityChange: (FlashManager.Capability) -> Unit,
    onOpenGsi: () -> Unit,
    onOpenHarmonyBuild: () -> Unit,
    onDownloadAddon: (AddonDownload) -> Unit
) {
    var pendingRecovery by remember { mutableStateOf<Pair<DownloadStore.Item, FlashManager.Recovery>?>(null) }
    var pendingDd by remember { mutableStateOf<Pair<DownloadStore.Item, String>?>(null) }
    var actionResult by remember { mutableStateOf<String?>(null) }
    val verified = downloads.filter { it.verified == true && it.status.startsWith("Downloaded") }
    val uriHandler = LocalUriHandler.current

    LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp)) {
        item {
            ToolsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Flashing & tools", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (capability.isSimulation) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                "Safety Simulation Mode",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    if (capability.isSimulation)
                        "Safety Simulation Mode active: Virtual root enabled so you can test recovery handoffs and dd partition flashing on the virtual phone without hardware risk."
                    else if (capability.rooted)
                        "Physical root access detected. Recovery and image flashing controls are active."
                    else
                        "Root is not detected. Download tools are available. Enable Virtual Phone Root Mode below to test all root features in the emulator.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ToolStatus("Root", capability.rooted)
                    ToolStatus("TWRP", capability.twrpDetected)
                    ToolStatus("OrangeFox", capability.orangeFoxDetected)
                }
                Spacer(Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                            Text("Virtual Phone Root Mode", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "Simulate root (uid=0) with TWRP & OrangeFox in emulator",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = capability.isSimulation,
                            onCheckedChange = { enabled ->
                                FlashManager.setRootTestingMode(context, enabled)
                                onCapabilityChange(FlashManager.detect(context))
                            }
                        )
                    }
                }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Recovery flash", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("OpenRecoveryScript", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(8.dp))
                Text("Flash verified ZIP packages through TWRP, OrangeFox, or generic OpenRecoveryScript.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                if (verified.isEmpty()) {
                    Text("Download and SHA-256 verify a ZIP first in Archive or Updates.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    verified.filter { it.filename.endsWith(".zip", true) }.take(12).forEach { item ->
                        Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            FlashButton("TWRP", capability.twrpDetected) { pendingRecovery = item to FlashManager.Recovery.TWRP }
                            FlashButton("OrangeFox", capability.orangeFoxDetected) { pendingRecovery = item to FlashManager.Recovery.ORANGEFOX }
                            FlashButton("OpenRecovery", capability.rooted) { pendingRecovery = item to FlashManager.Recovery.OPEN_RECOVERY }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            ToolsCard {
                Text("Harmony Recovery image build", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Prepare a reproducible Linux build script from a trusted manifest, branch, device codename, and the source tree's exact build target. The phone does not fabricate a recovery image.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenHarmonyBuild, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Build, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Open Harmony Recovery Build Lab")
                }
            }
        }

        item {
            ToolsCard {
                Text("dd image flasher", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Root-only raw image flashing to an explicitly selected by-name partition. In Simulation Mode, partition writing is safely modeled without modifying storage.", color = if (capability.isSimulation) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
                val images = verified.filter { it.filename.endsWith(".img", true) }
                if (images.isEmpty()) {
                    Text("No verified .img downloads available.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    images.take(8).forEach { item ->
                        Text(item.filename, fontWeight = FontWeight.SemiBold, maxLines = 2)
                        Spacer(Modifier.height(6.dp))
                        listOf("boot", "boot_a", "boot_b", "system", "system_a", "system_b", "recovery", "vendor_boot", "dtbo")
                            .chunked(3).forEach { targets ->
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                                    targets.forEach { target ->
                                        FlashButton(target, capability.rooted) { pendingDd = item to target }
                                    }
                                }
                                Spacer(Modifier.height(5.dp))
                            }
                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }

        item {
            ToolsCard {
                Text("GSI Discovery", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Text("Live community Treble/GSI GitHub release assets (AndyYan, ponces) filtered by architecture. Discovery does not guarantee device compatibility.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                Button(onClick = onOpenGsi, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Search, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Browse GSI Releases")
                }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("MindTheGapps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh GApps") }
                }
                Text("Official MindTheGapps release packages from GitHub.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                gapps.take(10).forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        item {
            ToolsCard {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Magisk Downloader", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onRefresh) { Icon(Icons.Default.Download, "Refresh Magisk") }
                }
                Text("Official topjohnwu/Magisk GitHub release assets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(10.dp))
                magisk.take(10).forEach { AddonRow(it, onDownloadAddon) }
            }
        }

        error?.let { item { ToolsCard { Text(it, color = MaterialTheme.colorScheme.error); TextButton(onClick = onRefresh) { Text("Try again") } } } }
    }

    pendingRecovery?.let { (item, recovery) ->
        AlertDialog(
            onDismissRequest = { pendingRecovery = null },
            title = { Text("Flash with " + recoveryLabel(recovery) + "?") },
            text = { Text("Only the already SHA-256 verified ZIP will be installed. The device will reboot to recovery. Confirm the package and make a backup before continuing.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRecovery = null
                    val res = FlashManager.flashVerifiedZip(context, item.filename, recovery)
                    actionResult = if (res.isSuccess) res.getOrNull() else "Flash failed: " + res.exceptionOrNull()?.message
                }) { Text("Flash") }
            },
            dismissButton = { TextButton(onClick = { pendingRecovery = null }) { Text("Cancel") } }
        )
    }

    pendingDd?.let { (item, target) ->
        AlertDialog(
            onDismissRequest = { pendingDd = null },
            title = { Text("dd → " + target) },
            text = { Text("This writes " + item.filename + " directly to /dev/block/by-name/" + target + ". A wrong image or target can make the device unbootable. Continue only if you have confirmed the partition layout.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDd = null
                    val res = FlashManager.flashImageWithDd(context, item.filename, target)
                    actionResult = if (res.isSuccess) res.getOrNull() else "dd write failed: " + res.exceptionOrNull()?.message
                }) { Text("Write image") }
            },
            dismissButton = { TextButton(onClick = { pendingDd = null }) { Text("Cancel") } }
        )
    }

    actionResult?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionResult = null },
            title = { Text("Flashing Execution Result") },
            text = { Text(msg) },
            confirmButton = { TextButton(onClick = { actionResult = null }) { Text("OK") } }
        )
    }
}

@Composable
private fun ToolStatus(label: String, available: Boolean) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(if (available) label + " ✓" else label + " —") },
        leadingIcon = { Icon(if (available) Icons.Default.CheckCircle else Icons.Default.Security, null) }
    )
}

@Composable
private fun RowScope.FlashButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 4.dp)) {
        Icon(Icons.Default.Build, null, Modifier.size(16.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, maxLines = 1)
    }
}

@Composable
private fun AddonRow(item: AddonDownload, onDownload: (AddonDownload) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(Icons.Default.Storage, null, Modifier.padding(top = 2.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(item.version + " • " + formatAddonBytes(item.size), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Text(item.source, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelSmall)
            if (item.sha256 != null) Text("SHA-256 available", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalButton(onClick = { onDownload(item) }, shape = RoundedCornerShape(12.dp)) {
            Icon(Icons.Default.Download, null, Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Download")
        }
    }
}

private fun recoveryLabel(recovery: FlashManager.Recovery) = when (recovery) {
    FlashManager.Recovery.TWRP -> "TWRP"
    FlashManager.Recovery.ORANGEFOX -> "OrangeFox"
    FlashManager.Recovery.OPEN_RECOVERY -> "OpenRecovery"
}

private fun formatAddonBytes(size: Long): String = when {
    size < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", size / 1024.0)
    size < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", size / (1024.0 * 1024.0))
    else -> String.format(Locale.US, "%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
}

@Composable
private fun ToolsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(Modifier.padding(18.dp), content = content)
    }
}
