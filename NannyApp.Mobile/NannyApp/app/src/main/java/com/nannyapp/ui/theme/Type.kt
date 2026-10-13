package com.nannyapp.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.nannyapp.R

// The website's two typefaces (--font-body and --font-display in style.css), bundled as
// variable fonts so the app looks the same offline. Licences: NannyApp/licenses/.
@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight) =
    Font(resId, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))

val Manrope = FontFamily(
    variableFont(R.font.manrope, FontWeight.Normal),
    variableFont(R.font.manrope, FontWeight.Medium),
    variableFont(R.font.manrope, FontWeight.SemiBold),
    variableFont(R.font.manrope, FontWeight.Bold),
    variableFont(R.font.manrope, FontWeight.ExtraBold),
)

val Sora = FontFamily(
    variableFont(R.font.sora, FontWeight.Medium),
    variableFont(R.font.sora, FontWeight.SemiBold),
    variableFont(R.font.sora, FontWeight.Bold),
    variableFont(R.font.sora, FontWeight.ExtraBold),
)

// Headings use the display face with the website's tight tracking; everything else is the body face.
private fun display(weight: FontWeight, size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Sora, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp, letterSpacing = (-0.02).em)

private fun body(weight: FontWeight, size: Int, lineHeight: Int) =
    TextStyle(fontFamily = Manrope, fontWeight = weight, fontSize = size.sp, lineHeight = lineHeight.sp)

val NannyTypography = Typography(
    displayLarge = display(FontWeight.ExtraBold, 44, 50),
    displayMedium = display(FontWeight.ExtraBold, 38, 44),
    displaySmall = display(FontWeight.ExtraBold, 33, 39),
    headlineLarge = display(FontWeight.Bold, 29, 34),
    headlineMedium = display(FontWeight.Bold, 24, 30),
    headlineSmall = display(FontWeight.Bold, 21, 27),
    titleLarge = display(FontWeight.Bold, 20, 26),
    titleMedium = body(FontWeight.Bold, 16, 22),
    titleSmall = body(FontWeight.Bold, 14, 20),
    bodyLarge = body(FontWeight.Normal, 16, 26),
    bodyMedium = body(FontWeight.Normal, 14, 22),
    bodySmall = body(FontWeight.Normal, 13, 19),
    labelLarge = body(FontWeight.Bold, 14, 20),
    labelMedium = body(FontWeight.SemiBold, 12, 16),
    labelSmall = body(FontWeight.SemiBold, 12, 16),
)
