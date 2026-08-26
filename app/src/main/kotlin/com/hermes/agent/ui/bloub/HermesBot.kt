package com.hermes.agent.ui.bloub

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hermes.agent.ui.home.HermesPersona.Mood
import com.jeeves.core.settings.JeevesSettings

/**
 * Jeeves' own face: the bot engine wired to the persona and to the user's saved
 * customisation.
 *
 * Screens use this rather than [BloubBot] directly, so the face is the same
 * everywhere and the customiser only has to write to one place.
 *
 * Everything under `ui/bloub` except this file is shared verbatim with the
 * standalone Hermes app, which ships the same face with the customiser left out.
 * Keep the divergence here: if a change belongs in the engine, it belongs in
 * both.
 */

/** What the customiser saved. [color] null = follow the app's theme. */
data class BotAppearance(
    val shape: ShapeId = DEFAULT_SHAPE,
    val expression: ExpressionId = DEFAULT_EXPRESSION,
    val color: ColorId? = null,
)

@Composable
fun rememberBotAppearance(): BotAppearance {
    val context = LocalContext.current
    val shape by JeevesSettings.botShapeFlow(context).collectAsStateWithLifecycle(JeevesSettings.botShape(context))
    val expression by JeevesSettings.botExpressionFlow(context)
        .collectAsStateWithLifecycle(JeevesSettings.botExpression(context))
    val color by JeevesSettings.botColorFlow(context).collectAsStateWithLifecycle(JeevesSettings.botColor(context))
    val themeColor by JeevesSettings.botThemeColorFlow(context)
        .collectAsStateWithLifecycle(JeevesSettings.botThemeColor(context))

    return BotAppearance(
        // An unknown id — a stale preference, a hand-edited file — falls back to
        // the default rather than throwing.
        shape = ShapeId.fromId(shape) ?: DEFAULT_SHAPE,
        expression = ExpressionId.fromId(expression) ?: DEFAULT_EXPRESSION,
        color = if (themeColor) null else ColorId.fromId(color) ?: DEFAULT_COLOR,
    )
}

/**
 * The body's colour, and the colour that shows through the eye holes.
 *
 * Following the theme gives ink-on-paper in light mode and the reverse in dark,
 * which is what the app's monochrome scheme wants. [botPaper] is always the real
 * background: the eyes are holes, so what they show has to be what is behind.
 */
@Composable
fun botInk(appearance: BotAppearance): Color =
    appearance.color?.let { Color(it.argb) } ?: MaterialTheme.colorScheme.onBackground

@Composable
fun botPaper(): Color = MaterialTheme.colorScheme.background

/**
 * The state each persona mood is drawn as.
 *
 * The mapping favours keeping a FACE on screen: the states that dissolve it
 * (`sleep` is a bouncing dot, `exclaim` a glyph) are reserved for the customiser's
 * board, and the everyday moods stay on `idle` — the only state that carries the
 * resting face, so the user's chosen expression and body shape both show through.
 * The event moods borrow the measured states that read at a glance.
 */
fun moodState(mood: Mood): StateId = when (mood) {
    Mood.NEUTRAL, Mood.HAPPY, Mood.FOCUSED, Mood.SLEEPY -> StateId.IDLE
    // the three dots, measured off the video: the clearest "working on it"
    Mood.THINKING -> StateId.THINKING
    // eyes wide open, the attentive state of the reference
    Mood.LISTENING -> StateId.WIDE
    // the body collapses and the particles spiral in — a real startle
    Mood.SURPRISED -> StateId.BURST
    Mood.CELEBRATE -> StateId.COMET
}

/**
 * The expression a mood carries.
 *
 * Only `idle` accepts one — the other states have an expression measured off the
 * video, and that is precisely what is being reproduced — so this returns the
 * user's own choice for a neutral mood and overrides it only where the mood
 * genuinely differs from resting.
 */
fun moodExpression(mood: Mood, resting: ExpressionId): ExpressionId = when (mood) {
    Mood.HAPPY -> ExpressionId.HEUREUX
    Mood.FOCUSED -> ExpressionId.ATTENTIF
    Mood.SLEEPY -> ExpressionId.SOMNOLENT
    else -> resting
}

/**
 * Jeeves' face at a given mood, drawn with the saved customisation.
 *
 * [size] is the side of the whole viewBox; the ball itself is about 0.63 of it,
 * the rest is the margin the orbit rings need.
 */
@Composable
fun HermesBot(
    mood: Mood,
    modifier: Modifier = Modifier,
    size: Dp = 84.dp,
    aim: Offset? = null,
    /** overrides the mood's own expression; the greeting uses it to arrive excited */
    expression: ExpressionId? = null,
    /** play the arrival turn once on first appearance — see [BloubLook.tourLook] */
    arrival: Boolean = false,
    appearance: BotAppearance = rememberBotAppearance(),
    label: String? = "Jeeves",
) {
    BloubBot(
        modifier = modifier,
        state = moodState(mood),
        size = size,
        shape = appearance.shape,
        expression = expression ?: moodExpression(mood, appearance.expression),
        arrival = arrival,
        ink = botInk(appearance),
        paper = botPaper(),
        aim = aim,
        label = label,
    )
}
