package com.l3ad3r1.octojotter.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class OctoStatusColors(
  val syncOk: Color,
  val syncPending: Color,
  val localOnly: Color,
  val wikiLink: Color,
  val hashtag: Color,
  val code: Color,
  val codeBackground: Color,
)

val LightStatusColors = OctoStatusColors(
  syncOk = Color.Black,
  syncPending = Color(0xFF333333),
  localOnly = Color(0xFF666666),
  wikiLink = Color.Black,
  hashtag = Color(0xFF444444),
  code = Color.Black,
  codeBackground = Color(0xFFE8E8E8),
)

val DarkStatusColors = OctoStatusColors(
  syncOk = Color.White,
  syncPending = Color(0xFFD0D0D0),
  localOnly = Color(0xFF9A9A9A),
  wikiLink = Color.White,
  hashtag = Color(0xFFB8B8B8),
  code = Color.White,
  codeBackground = Color(0xFF171717),
)

val LocalOctoStatusColors = staticCompositionLocalOf { DarkStatusColors }

val MaterialTheme.octoStatus: OctoStatusColors
  @Composable
  @ReadOnlyComposable
  get() = LocalOctoStatusColors.current
