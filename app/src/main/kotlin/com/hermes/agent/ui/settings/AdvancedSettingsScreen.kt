package com.hermes.agent.ui.settings
import com.hermes.agent.domain.settings.*

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.SaveAlt
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.runtime.saveable.rememberSaveable
import com.hermes.agent.data.export.BackupSection
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.ui.theme.hermesFieldColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val jsonBackupState by viewModel.jsonBackupState.collectAsStateWithLifecycle()
    val privilegedStatus by viewModel.privilegedStatus.collectAsStateWithLifecycle()
    val retryGateStatus by viewModel.privilegedRetryGateStatus.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Advanced") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader(text = "Privileged Shell (Shizuku)")
            PrivilegedShellSection(
                enabled = settings.privilegedShellEnabled,
                status = privilegedStatus,
                gateStatus = retryGateStatus,
                onToggleEnabled = viewModel::setPrivilegedShellEnabled,
                onRequestPermission = viewModel::requestPrivilegedPermission,
                onRefresh = viewModel::refreshPrivilegedStatus,
                onResetGate = viewModel::resetPrivilegedGate,
            )

            SectionHeader(text = "Backup & Restore")
            BackupRestoreSection(
                jsonState = jsonBackupState,
                onJsonBackup = { uri, sections, password, bots -> viewModel.exportJson(uri, sections, password, bots) },
                onJsonRestore = viewModel::importJson,
                onJsonDismiss = viewModel::dismissJsonBackupState,
            )
            AutoBackupSection()

            SectionHeader(text = "Import chats")
            ChatImportSection()

            SectionHeader(text = "Files & Workspace Access")
            FilesWorkspaceSection(
                rootUri = settings.filesRootUri,
                onUpdateRoot = viewModel::setFilesRootUri,
            )

        }
    }
}

@Composable
private fun FilesWorkspaceSection(
    rootUri: String,
    onUpdateRoot: (String) -> Unit,
) {
    val launcher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree(),
    ) { uri ->
        if (uri != null) {
            onUpdateRoot(uri.toString())
        }
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Workspace Root Directory",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (rootUri.isBlank()) {
                    "Default: App Sandbox Internal Workspace"
                } else {
                    "Granted Root: $rootUri"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = { launcher.launch(null) },
                    modifier = Modifier.weight(1f),
                    contentPadding = SettingsButtonPadding,
                ) {
                    ButtonLabel(if (rootUri.isBlank()) "Grant Directory" else "Change Directory")
                }
                if (rootUri.isNotBlank()) {
                    OutlinedButton(
                        onClick = { onUpdateRoot("") },
                        contentPadding = SettingsButtonPadding,
                    ) {
                        ButtonLabel("Revoke / Reset")
                    }
                }
            }
        }
    }
}

@Composable
private fun PrivilegedShellSection(
    enabled: Boolean,
    status: com.hermes.agent.domain.device.PrivilegedShellBackend.PrivilegedStatus,
    gateStatus: com.hermes.agent.data.device.PrivilegedShellRetryGate.GateStatus,
    onToggleEnabled: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onResetGate: () -> Unit,
) {
    val context = LocalContext.current
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DescribedTitle(
                    title = "Enable Privileged Shell",
                    description = "Allows the shell tool to run with ADB privileges (UID 2000) via Shizuku.",
                    modifier = Modifier.weight(1f),
                )
                androidx.compose.material3.Switch(
                    checked = enabled,
                    onCheckedChange = onToggleEnabled,
                )
            }

            androidx.compose.material3.HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Shizuku Status",
                    style = MaterialTheme.typography.titleSmall,
                )
                androidx.compose.material3.TextButton(onClick = onRefresh) {
                    Text("Check Status")
                }
            }

            when (status.status) {
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.READY -> {
                    Text(
                        text = "● Connected (UID ${status.uid} · Version ${status.version})",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.PERMISSION_REQUIRED -> {
                    Text(
                        text = "⚠️ Permission Required: Jeeves needs Shizuku access.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Grant Shizuku Permission")
                    }
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.DEAD -> {
                    Text(
                        text = "⚠️ Shizuku service is not running.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                    Text(
                        text = "Start it via ADB:\n${com.hermes.agent.data.device.PrivilegedShellGateway.ADB_START_COMMAND}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("ADB Command", com.hermes.agent.data.device.PrivilegedShellGateway.ADB_START_COMMAND)
                            clipboard.setPrimaryClip(clip)
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Copy ADB Command")
                    }
                }
                com.hermes.agent.domain.device.PrivilegedShellBackend.Status.NOT_INSTALLED -> {
                    Text(
                        text = "Shizuku app is not installed on this device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (gateStatus.state == com.hermes.agent.data.device.PrivilegedShellRetryGate.State.DIRTY_UNWIND) {
                androidx.compose.material3.HorizontalDivider()
                Text(
                    text = "⚠️ Execution Gate Locked: ${gateStatus.reason}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                OutlinedButton(
                    onClick = onResetGate,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Reset Execution Gate")
                }
            }
        }
    }
}
