package com.hermes.agent.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.ui.bloub.BloubBot
import com.hermes.agent.ui.bloub.COLORS
import com.hermes.agent.ui.bloub.ColorId
import com.hermes.agent.ui.bloub.DEFAULT_COLOR
import com.hermes.agent.ui.bloub.DEFAULT_EXPRESSION
import com.hermes.agent.ui.bloub.DEFAULT_SHAPE
import com.hermes.agent.ui.bloub.EXPRESSIONS
import com.hermes.agent.ui.bloub.ExpressionId
import com.hermes.agent.ui.bloub.POSES
import com.hermes.agent.ui.bloub.SEQUENCE
import com.hermes.agent.ui.bloub.SHAPES
import com.hermes.agent.ui.bloub.ShapeId
import com.hermes.agent.ui.bloub.StateId

/**
 * The bot customiser, the same three choices the reference offers: eight body
 * shapes, twelve colours and sixteen resting expressions, all previewed live.
 *
 * The state board at the bottom is the reference's `#planche`: the fourteen
 * measured states side by side, each frozen at the date it reads most clearly.
 * Tapping one plays it in the big preview, which is the only way to see the
 * states the persona does not otherwise reach.
 *
 * This screen is what makes Jeeves the full build: the standalone Hermes app
 * shares the whole `ui/bloub` engine but ships no customiser, so its face is
 * always the measured circle following the app theme.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BotFaceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val shapeId by viewModel.botShape.collectAsStateWithLifecycle()
    val colorId by viewModel.botColor.collectAsStateWithLifecycle()
    val expressionId by viewModel.botExpression.collectAsStateWithLifecycle()
    val themeColor by viewModel.botThemeColor.collectAsStateWithLifecycle()

    val shape = ShapeId.fromId(shapeId) ?: DEFAULT_SHAPE
    val expression = ExpressionId.fromId(expressionId) ?: DEFAULT_EXPRESSION
    val color = ColorId.fromId(colorId) ?: DEFAULT_COLOR

    val scheme = MaterialTheme.colorScheme
    val paper = scheme.background
    val ink = if (themeColor) scheme.onBackground else Color(color.argb)

    // What the big preview is showing. Idle unless the board asked for another.
    var preview by remember { mutableStateOf(StateId.IDLE) }
    var aim by remember { mutableStateOf<Offset?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assistant face") },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        modifier = Modifier.padding(start = 8.dp).size(44.dp),
                        shape = CircleShape,
                        color = scheme.surfaceVariant,
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // ── live preview ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(paper)
                    .border(1.dp, scheme.outline, RoundedCornerShape(22.dp))
                    // The bot follows a fingertip, as the reference follows the
                    // cursor: the aim is normalised to -1..1 about the centre, and
                    // released on lift so the head goes back to its resting drift.
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { aim = null },
                            onDragCancel = { aim = null },
                        ) { change, _ ->
                            aim = Offset(
                                (change.position.x / size.width) * 2f - 1f,
                                (change.position.y / size.height) * 2f - 1f,
                            )
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { pos ->
                            aim = Offset(
                                (pos.x / size.width) * 2f - 1f,
                                (pos.y / size.height) * 2f - 1f,
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                BloubBot(
                    state = preview,
                    size = 220.dp,
                    shape = shape,
                    expression = expression,
                    ink = ink,
                    paper = paper,
                    aim = aim,
                    label = "Assistant face preview",
                )
            }

            // ── body shape ──────────────────────────────────────────────────
            FaceCard {
                CardTitle("Body", "The silhouette the resting states morph into")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    for (s in SHAPES) {
                        SwatchTile(
                            selected = s.id == shape,
                            label = s.id.label,
                            onClick = { viewModel.setBotShape(s.id.id) },
                        ) {
                            BloubBot(
                                state = StateId.IDLE,
                                size = 54.dp,
                                shape = s.id,
                                expression = expression,
                                ink = ink,
                                paper = paper,
                                frozenAt = POSES[StateId.IDLE],
                                label = null,
                            )
                        }
                    }
                }
            }

            // ── colour ──────────────────────────────────────────────────────
            FaceCard {
                CardTitle("Colour", "The body's fill. The eyes are holes, so they always show the background.")
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Follow app theme", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "Ink on paper in light mode, the reverse in dark",
                            style = MaterialTheme.typography.bodySmall,
                            color = scheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = themeColor, onCheckedChange = viewModel::setBotThemeColor)
                }
                if (!themeColor) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (c in COLORS) {
                            val chosen = c == color
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(c.argb))
                                    .border(
                                        width = if (chosen) 3.dp else 1.dp,
                                        color = if (chosen) scheme.primary else scheme.outline,
                                        shape = CircleShape,
                                    )
                                    .clickable { viewModel.setBotColor(c.id) },
                                contentAlignment = Alignment.Center,
                            ) {
                                if (chosen) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = c.label,
                                        tint = if (c == ColorId.CREME || c == ColorId.AMBRE) Color.Black else Color.White,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── resting expression ──────────────────────────────────────────
            FaceCard {
                CardTitle(
                    "Resting expression",
                    "Only the idle state carries it — the others keep the expression measured off the reference.",
                )
                Grid(items = EXPRESSIONS.map { it.id!! }, columns = 4) { e ->
                    SwatchTile(
                        selected = e == expression,
                        label = e.label,
                        onClick = { viewModel.setBotExpression(e.id) },
                    ) {
                        BloubBot(
                            state = StateId.IDLE,
                            size = 54.dp,
                            shape = shape,
                            expression = e,
                            ink = ink,
                            paper = paper,
                            frozenAt = POSES[StateId.IDLE],
                            label = null,
                        )
                    }
                }
            }

            // ── the state board ─────────────────────────────────────────────
            FaceCard {
                CardTitle("States", "The fourteen measured states. Tap one to play it above.")
                Grid(items = SEQUENCE, columns = 4) { s ->
                    SwatchTile(
                        selected = s == preview,
                        label = s.label,
                        onClick = { preview = s },
                    ) {
                        BloubBot(
                            state = s,
                            size = 54.dp,
                            shape = shape,
                            expression = expression,
                            ink = ink,
                            paper = paper,
                            frozenAt = POSES[s],
                            label = null,
                        )
                    }
                }
            }

            Text(
                "The face is a recreation of the x.ai bot avatar from Bloub " +
                    "(github.com/jeremy-prt/bloub, MIT). Its constants are measurements, not design choices.",
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FaceCard(content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun CardTitle(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    Text(
        subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 10.dp),
    )
}

/**
 * A fixed-column grid built out of rows.
 *
 * Deliberately not a LazyVerticalGrid: this screen is one vertical scroll, and a
 * lazy grid nested in it needs a bounded height, which would either clip the last
 * row or introduce a second scroll region.
 */
@Composable
private fun <T> Grid(items: List<T>, columns: Int, item: @Composable (T) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in items.chunked(columns)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                for (cell in row) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { item(cell) }
                }
                // keep the last row's cells the same width as the full ones
                repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SwatchTile(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) scheme.primary else scheme.outlineVariant,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        preview()
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) scheme.primary else scheme.onSurfaceVariant,
            maxLines = 1,
            textAlign = TextAlign.Center,
        )
    }
}
