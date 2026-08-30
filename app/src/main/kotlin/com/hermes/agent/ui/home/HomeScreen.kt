package com.hermes.agent.ui.home

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.domain.model.Conversation
import com.hermes.agent.ui.bloub.HermesBot
import com.hermes.agent.ui.components.HermesDiamond
import com.jeeves.core.theme.GeistMono
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.Icon
import com.hermes.agent.ui.theme.alt.OutlinedSpaceTile
import com.hermes.agent.ui.theme.alt.SpaceTile
import com.hermes.agent.ui.theme.alt.ThemeStyle
import com.hermes.agent.ui.theme.alt.contrastInkAcross
import com.hermes.agent.ui.theme.alt.tileAccent
import com.jeeves.core.settings.JeevesSettings

/**
 * Home dashboard — the app's landing surface: greeting, the active cloud model,
 * quick actions (new chat, messaging), and the real recent-conversation list.
 */
@Composable
fun HomeScreen(
    onOpenConversations: () -> Unit,
    onNewChat: (conversationId: String) -> Unit,
    onOpenConnections: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val threads by viewModel.recentThreads.collectAsStateWithLifecycle()
    val model by viewModel.modelName.collectAsStateWithLifecycle()
    val presence by viewModel.presence.collectAsStateWithLifecycle()
    val scheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val themeStyleKey by JeevesSettings.themeStyleFlow(context)
        .collectAsStateWithLifecycle(initialValue = JeevesSettings.THEME_STYLE_CLASSIC)
    val themeStyle = ThemeStyle.fromStorageKey(themeStyleKey)
    val themeAccentArgb by JeevesSettings.themeAccentColorFlow(context)
        .collectAsStateWithLifecycle(initialValue = JeevesSettings.themeAccentColor(context))
    val accentSeed = themeAccentArgb?.let { Color(it) }

    Column(
        modifier = Modifier
            .background(scheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
            .padding(top = 8.dp, bottom = 26.dp),
    ) {
        // Header: the bot's face + context-aware greeting.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 86.dp is the whole viewBox, not the ball: the bot itself is about
            // 0.63 of it, and the margin is what the orbit rings need to stay in
            // frame. Colour and shape come from the customiser.
            HermesBot(
                mood = presence.mood,
                size = 86.dp,
                // Poke Jeeves: the body collapses and the particles spiral in.
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = viewModel::poke,
                ),
            )
            Spacer(Modifier.size(8.dp))
            // Settings lives in the bottom navigation, so the header keeps the
            // full width for the status line rather than repeating that route.
            Column(Modifier.weight(1f)) {
                Text(presence.greeting, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
                Text(
                    presence.statusLine,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onBackground,
                )
            }
        }

        // Active-model card. Cortex sweeps two of its accents for a splash of
        // colour; Material You fills flat with one wallpaper accent, matching
        // its tiles; Classic keeps the monochrome surface blend.
        val modelCardStops = when (themeStyle) {
            ThemeStyle.MATERIAL_YOU -> {
                val fill = tileAccent(themeStyle, scheme, 1, accentSeed)
                listOf(fill, fill)
            }
            ThemeStyle.CORTEX -> listOf(
                tileAccent(themeStyle, scheme, 1, accentSeed),
                tileAccent(themeStyle, scheme, 4, accentSeed),
            )
            ThemeStyle.CLASSIC -> listOf(scheme.surfaceVariant, scheme.surfaceContainerHigh)
        }
        val modelCardGradient = Brush.linearGradient(modelCardStops)
        // The label sits on whichever gradient won, and those grounds span
        // Cortex's deep ember, a wallpaper-derived Material You sweep and
        // Classic's near-white light surface. A hardcoded white reads on the
        // first and disappears on the last, so the ink is chosen from the stops
        // the card actually drew — on the worst of them, since the text has to
        // survive both ends of the gradient rather than its average.
        val modelCardInk = contrastInkAcross(modelCardStops)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(modelCardGradient)
                .padding(18.dp),
        ) {
            Text("Active model", color = modelCardInk.copy(alpha = 0.85f), style = MaterialTheme.typography.labelSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                model.ifBlank { "not configured" },
                fontFamily = GeistMono,
                color = modelCardInk,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.height(16.dp))

        // Quick actions
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            QuickAction(
                title = "New chat",
                subtitle = "Ask or delegate",
                icon = Icons.AutoMirrored.Filled.Chat,
                accent = tileAccent(themeStyle, scheme, 2, accentSeed),
                themeStyle = themeStyle,
                modifier = Modifier.weight(1f),
                onClick = { viewModel.createNewConversation(onNewChat) },
            )
            QuickAction(
                title = "Messaging",
                subtitle = "Link a platform",
                icon = Icons.Filled.Forum,
                accent = tileAccent(themeStyle, scheme, 1, accentSeed),
                themeStyle = themeStyle,
                modifier = Modifier.weight(1f),
                onClick = onOpenConnections,
            )
        }

        Spacer(Modifier.height(20.dp))

        // Bundled sub-app feature entries contributed dynamically via AgentFeature.entries()
        if (viewModel.appEntries.isNotEmpty()) {
            SectionLabel("Apps")
            Spacer(Modifier.height(11.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                viewModel.appEntries.forEachIndexed { index, entry ->
                    QuickAction(
                        title = entry.label,
                        subtitle = entry.subtitle,
                        icon = iconForRoute(entry.route),
                        accent = tileAccent(themeStyle, scheme, index, accentSeed),
                        themeStyle = themeStyle,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            entry.targetActivityClassName?.let { className ->
                                val intent = Intent().setClassName(context.packageName, className).apply {
                                    putExtra("EXTRA_EMBEDDED", true)
                                    addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                                }
                                if (context.packageManager.resolveActivity(intent, 0) != null) {
                                    context.startActivity(intent)
                                }
                            }
                        },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        Spacer(Modifier.height(20.dp))

        // Recent threads
        SectionHeader("Recent threads", action = "Open", onAction = onOpenConversations)
        Spacer(Modifier.height(11.dp))
        if (threads.isEmpty()) {
            EmptyHint("No conversations yet — start a new chat.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                threads.forEachIndexed { index, thread ->
                    ThreadRow(thread, themeStyle, index, accentSeed, onClick = { onNewChat(thread.id) })
                }
            }
        }
    }
}


/**
 * Maps an [AgentFeature][com.hermes.agent.domain.agent.AgentFeature] nav-entry
 * route to an icon for the Cortex/Material You tile grid — new modules just
 * fall back to a generic icon rather than requiring a mapping update.
 */
private fun iconForRoute(route: String) = when (route) {
    "butler_alarms" -> Icons.Filled.CalendarMonth
    "jotter_notes" -> Icons.Filled.EditNote
    else -> Icons.Filled.EditNote
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    themeStyle: ThemeStyle,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    // Both variants are the same tile: same squircle, same 1:1 footprint, same
    // title/subtitle above a large icon. Classic drops the fill and the accent
    // for a hairline outline and monochrome contents; the coloured styles keep
    // the accent glow and the tinted icon.
    if (themeStyle == ThemeStyle.CLASSIC) {
        OutlinedSpaceTile(
            title = title,
            subtitle = subtitle,
            icon = icon,
            modifier = modifier.aspectRatio(1f),
            onClick = onClick,
        )
    } else {
        SpaceTile(
            title = title,
            subtitle = subtitle,
            icon = icon,
            accent = accent,
            modifier = modifier.aspectRatio(1f),
            // Material You fills the card with the wallpaper accent outright;
            // Cortex keeps the softer glow its fixed palette was tuned for.
            solid = themeStyle == ThemeStyle.MATERIAL_YOU,
            onClick = onClick,
        )
    }
}
/** Section heading without a trailing action link (cf. [SectionHeader]). */
@Composable
private fun SectionLabel(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.outline,
    )
}

@Composable
private fun SectionHeader(title: String, action: String, onAction: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.sp,
            color = scheme.outline,
        )
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onAction) {
            Text(
                action,
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
            )
        }
    }
}

@Composable
private fun ThreadRow(
    thread: Conversation,
    themeStyle: ThemeStyle,
    index: Int,
    accentSeed: Color?,
    onClick: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    // A different accent per row under Cortex/Material You — a splash of
    // colour down the thread list rather than one repeated primary dot.
    val dotColor = if (themeStyle != ThemeStyle.CLASSIC) {
        tileAccent(themeStyle, scheme, index, accentSeed)
    } else {
        scheme.primary
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = 0.25f), MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(RoundedCornerShape(percent = 50))
                .background(dotColor),
        )
        Spacer(Modifier.size(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                thread.title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (thread.lastMessagePreview.isNotBlank()) {
                Text(
                    thread.lastMessagePreview,
                    fontSize = 12.sp,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(scheme.surface)
            .border(1.dp, scheme.outline.copy(alpha = 0.25f), MaterialTheme.shapes.medium)
            .padding(16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = scheme.onSurfaceVariant)
    }
}
