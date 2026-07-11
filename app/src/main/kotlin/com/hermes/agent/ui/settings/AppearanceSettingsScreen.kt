package com.hermes.agent.ui.settings

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jeeves.core.settings.JeevesSettings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance") },
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
            ThemePicker(
                currentTheme = settings.appTheme,
                onThemeSelected = viewModel::setAppTheme,
            )
            DarkModePicker(
                current = themeMode,
                onSelected = viewModel::setThemeMode,
            )
        }
    }
}

private data class ThemeOption(
    val name: String,
    val label: String,
    val bg: Color,
    val fg: Color,
    val accent: Color,
)

private val themeOptions = listOf(
    ThemeOption("MIDNIGHT",    "Monochrome",   Color(0xFF0A0A0F), Color(0xFFF3F3F6), Color(0xFF5B73FF)),
    ThemeOption("HERMES_BLUE", "Blue",         Color(0xFF3300FF), Color(0xFFFFFFFF), Color(0xFF2200CC)),
)

@Composable
private fun ThemePicker(
    currentTheme: String,
    onThemeSelected: (String) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "App Theme", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                themeOptions.forEach { option ->
                    ThemeSwatch(
                        option = option,
                        selected = currentTheme == option.name,
                        onClick = { onThemeSelected(option.name) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatch(
    option: ThemeOption,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(2.dp, borderColor, RoundedCornerShape(8.dp))
                .clickable(onClick = onClick)
                .semantics {
                    role = Role.RadioButton
                    this.selected = selected
                    this.contentDescription = "Theme: ${option.label}"
                },
        ) {
            Surface(
                modifier = Modifier.matchParentSize(),
                color = option.bg,
                shape = RoundedCornerShape(8.dp),
            ) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.7f).height(8.dp),
                        color = option.fg,
                        shape = RoundedCornerShape(4.dp),
                        content = {},
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.5f).height(6.dp),
                        color = option.fg.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(4.dp),
                        content = {},
                    )
                    Surface(
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        color = option.accent,
                        shape = RoundedCornerShape(4.dp),
                        content = {},
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = option.fg,
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(16.dp),
                )
            }
        }
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DarkModePicker(
    current: String,
    onSelected: (String) -> Unit,
) {
    val options = listOf(
        JeevesSettings.THEME_SYSTEM to "System",
        JeevesSettings.THEME_LIGHT to "Light",
        JeevesSettings.THEME_DARK to "Dark",
    )
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Dark mode", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Applies across the app, including Notes.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(12.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = current == value,
                        onClick = { onSelected(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, options.size),
                    ) { Text(label) }
                }
            }
        }
    }
}
