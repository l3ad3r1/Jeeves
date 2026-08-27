package com.hermes.agent.ui.settings

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
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.SaveAlt
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontFamily
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
            SectionHeader(text = "Backup & Restore")
            JsonBackupSection(
                state = jsonBackupState,
                onBackup = viewModel::exportJson,
                onRestore = viewModel::importJson,
                onDismiss = viewModel::dismissJsonBackupState,
            )

        }
    }
}

/**
 * Portable JSON export/import, sitting alongside the whole-database ZIP above.
 *
 * The two are not interchangeable and the copy says so: the ZIP is an exact
 * image that replaces everything and restarts the app, while this writes a
 * readable file of the user's own content that merges into a live install.
 */

/**
 * The single backup surface: pick what travels, optionally lock it with a
 * password, write it out or read it back.
 *
 * Replaces the old whole-database ZIP, which could only take everything, could
 * only be restored wholesale, and needed an app restart to apply.
 */
@Composable
private fun JsonBackupSection(
    state: BackupUiState,
    onBackup: (android.net.Uri, Set<BackupSection>, String?) -> Unit,
    onRestore: (android.net.Uri, Boolean, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    // Credentials start unticked: a backup is something people put in cloud
    // storage, so carrying keys has to be a deliberate act rather than the
    // thing that happens if you do not read the screen.
    var selected by remember { mutableStateOf(BackupSection.DEFAULT) }
    var password by rememberSaveable { mutableStateOf("") }
    var overwrite by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }

    val keysSelected = BackupSection.CREDENTIALS in selected
    // Enforced in the UI so the button explains itself, and again in encode()
    // so no caller can bypass it.
    val passwordRequired = keysSelected
    val canBackUp = selected.isNotEmpty() && (!passwordRequired || password.isNotBlank())

    val backupLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> if (uri != null) onBackup(uri, selected, password.ifBlank { null }) }

    val restoreLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.OpenDocument(),
    ) { uri -> if (uri != null) onRestore(uri, overwrite, password.ifBlank { null }) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Backup,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                Text("Backup & Restore", style = MaterialTheme.typography.bodyLarge)
            }
            Text(
                "Choose what to include. Restoring merges into what is already " +
                    "here and needs no restart.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when (state) {
                is BackupUiState.InProgress -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Text("Working…", style = MaterialTheme.typography.bodySmall)
                    }
                }
                is BackupUiState.Success -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                        Text("Dismiss")
                    }
                }
                is BackupUiState.Error -> {
                    Text(
                        state.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Unit
            }

            if (state !is BackupUiState.InProgress && state !is BackupUiState.Success) {
                BackupSection.entries.forEach { section ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                selected = if (section in selected) selected - section else selected + section
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Checkbox(
                            checked = section in selected,
                            onCheckedChange = {
                                selected = if (it) selected + section else selected - section
                            },
                        )
                        Text(section.label, style = MaterialTheme.typography.bodyMedium)
                        if (section == BackupSection.CREDENTIALS) {
                            Text(
                                " · needs a password",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (passwordRequired) "Password (required)" else "Password (optional)")
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) {
                        androidx.compose.ui.text.input.VisualTransformation.None
                    } else {
                        androidx.compose.ui.text.input.PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(onClick = { showPassword = !showPassword }) {
                            Text(if (showPassword) "Hide" else "Show")
                        }
                    },
                    supportingText = {
                        Text(
                            "Encrypts the whole file. You will need this same password " +
                                "to restore it — it cannot be recovered.",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Switch(checked = overwrite, onCheckedChange = { overwrite = it })
                    Text(
                        if (overwrite) {
                            "Restoring replaces items that already exist"
                        } else {
                            "Restoring keeps your existing items"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { backupLauncher.launch(defaultBackupFileName(APP_LABEL)) },
                        enabled = canBackUp,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back up")
                    }
                    OutlinedButton(
                        // Some providers hand JSON back as octet-stream or
                        // text/plain, so filtering on application/json alone
                        // would grey out the very file we just wrote.
                        onClick = {
                            restoreLauncher.launch(
                                arrayOf("application/json", "text/plain", "application/octet-stream"),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Restore")
                    }
                }
                if (passwordRequired && password.isBlank()) {
                    Text(
                        "Set a password to include cloud API keys.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private const val APP_LABEL = "jeeves"

private fun defaultBackupFileName(app: String): String {
    val stamp = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        .format(java.util.Date())
    return "${app}-backup-$stamp.json"
}
