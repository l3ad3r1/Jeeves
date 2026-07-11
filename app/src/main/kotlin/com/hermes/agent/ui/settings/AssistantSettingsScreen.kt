package com.hermes.agent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.R
import com.hermes.agent.ui.theme.hermesFieldColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant") },
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
            SectionHeader(text = stringResource(R.string.settings_section_cloud))
            CloudSection(settings = settings, viewModel = viewModel)

            SectionHeader(text = "Chat")
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    ToggleRow(
                        title = "Show tool call details",
                        subtitle = "See what the agent does mid-reply (web search, calendar, etc.) " +
                            "instead of just the final answer",
                        checked = settings.showToolCalls,
                        onCheckedChange = viewModel::setShowToolCalls,
                    )
                }
            }
        }
    }
}

@Composable
private fun CloudSection(
    settings: com.hermes.agent.data.settings.UserSettings,
    viewModel: SettingsViewModel,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ToggleRow(
                title = stringResource(R.string.settings_cloud_enabled),
                subtitle = stringResource(R.string.settings_cloud_enabled_subtitle),
                checked = settings.cloudEnabled,
                onCheckedChange = viewModel::setCloudEnabled,
            )
            HorizontalDivider()

            Text(
                "Primary provider — general tasks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var apiKey by remember(settings.cloudApiKey) { mutableStateOf(settings.cloudApiKey) }
            OutlinedTextField(
                value = apiKey,
                onValueChange = {
                    apiKey = it
                    viewModel.setCloudApiKey(it)
                },
                label = { Text(stringResource(R.string.settings_cloud_api_key)) },
                supportingText = {
                    Text(stringResource(R.string.settings_cloud_api_key_subtitle))
                },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            var baseUrl by remember(settings.cloudBaseUrl) { mutableStateOf(settings.cloudBaseUrl) }
            OutlinedTextField(
                value = baseUrl,
                onValueChange = {
                    baseUrl = it
                    viewModel.setCloudBaseUrl(it)
                },
                label = { Text(stringResource(R.string.settings_cloud_base_url)) },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            var model by remember(settings.cloudModel) { mutableStateOf(settings.cloudModel) }
            OutlinedTextField(
                value = model,
                onValueChange = {
                    model = it
                    viewModel.setCloudModel(it)
                },
                label = { Text(stringResource(R.string.settings_cloud_model)) },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            HorizontalDivider()

            Text(
                "Specialist provider — specialised tasks",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            var specialisedModel by remember(settings.auxModel) { mutableStateOf(settings.auxModel) }
            OutlinedTextField(
                value = specialisedModel,
                onValueChange = {
                    specialisedModel = it
                    viewModel.setAuxModel(it)
                },
                label = { Text(stringResource(R.string.settings_specialised_model)) },
                supportingText = { Text(stringResource(R.string.settings_specialised_model_hint)) },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            var auxBaseUrl by remember(settings.auxBaseUrl) { mutableStateOf(settings.auxBaseUrl) }
            OutlinedTextField(
                value = auxBaseUrl,
                onValueChange = {
                    auxBaseUrl = it
                    viewModel.setAuxBaseUrl(it)
                },
                label = { Text("Specialist base URL (optional)") },
                supportingText = { Text("Leave blank to use the primary provider's endpoint.") },
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )

            var auxApiKey by remember(settings.auxApiKey) { mutableStateOf(settings.auxApiKey) }
            OutlinedTextField(
                value = auxApiKey,
                onValueChange = {
                    auxApiKey = it
                    viewModel.setAuxApiKey(it)
                },
                label = { Text("Specialist API key (optional)") },
                supportingText = { Text("Leave blank to use the primary provider's key.") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                colors = hermesFieldColors(),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
