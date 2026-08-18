package dev.ssactinium.phone3dsgamepad.ui.theme

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

val DisplayFont = FontFamily.SansSerif
val MonoFont = FontFamily.Monospace
val BodyFont = FontFamily.SansSerif

val BrandTitle = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 28.sp,
    letterSpacing = 3.sp,
    color = Ink,
)

val HudMono = TextStyle(
    fontFamily = MonoFont,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    letterSpacing = 0.4.sp,
    color = Ink,
)

val FaceGlyph = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = FontWeight.Bold,
    fontSize = 20.sp,
    letterSpacing = 0.5.sp,
    color = FaceLabel,
)
