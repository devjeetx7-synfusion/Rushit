package com.rushit.admin

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Centralized design tokens for RushIt Admin V2.
 * Follows a Premium FinTech + Modern Commerce SaaS aesthetic with warm surfaces and zero default Material purple/blue.
 */
object AdminColorScheme {
    val primary = Color(0xFFE7473C)
    val primaryDark = Color(0xFFC93630)
    val primarySoft = Color(0xFFFFF0ED)
    val secondary = Color(0xFFF47B6C)
    val accent = Color(0xFFFFB84D)
    val accentSoft = Color(0xFFFFF6E5)
    val background = Color(0xFFFFFDF7)
    val surface = Color(0xFFFFFFFF)
    val surfaceSecondary = Color(0xFFFFF7F3)
    val surfaceMuted = Color(0xFFFAF6F0)
    val primaryText = Color(0xFF292524)
    val secondaryText = Color(0xFF78716C)
    val mutedText = Color(0xFF9A928C)
    val success = Color(0xFF27845B)
    val successSoft = Color(0xFFE8F6EF)
    val warning = Color(0xFFD99024)
    val warningSoft = Color(0xFFFFF6E5)
    val error = Color(0xFFC9433D)
    val errorSoft = Color(0xFFFDECEC)
    val info = Color(0xFF2E6B9E)
    val infoSoft = Color(0xFFEAF3FA)
    val divider = Color(0xFFEEE9E3)
    val inputBackground = Color(0xFFFFFCF8)
    val disabled = Color(0xFFC4BDB5)

    // Backward-compatible aliases
    val text = primaryText
    val muted = secondaryText
    val input = inputBackground

    fun brandColorFor(themeName: String): Color = when (themeName) {
        "Sunrise" -> Color(0xFFE07A1F)
        "Forest" -> Color(0xFF27845B)
        "Ocean" -> Color(0xFF2E6B9E)
        "Berry" -> Color(0xFFB8336A)
        else -> primary
    }

    fun brandSoftFor(themeName: String): Color = when (themeName) {
        "Sunrise" -> Color(0xFFFFF4E6)
        "Forest" -> Color(0xFFE8F6EF)
        "Ocean" -> Color(0xFFEAF3FA)
        "Berry" -> Color(0xFFFCEEF4)
        else -> primarySoft
    }

    fun accentColorFor(accentName: String): Color = when (accentName) {
        "Coral Soft" -> secondary
        "Mint" -> Color(0xFF2E9E73)
        "Sky" -> Color(0xFF3B82C4)
        "Gold" -> Color(0xFFE59A23)
        else -> accent
    }
}

/** Alias preserving compatibility with existing references. */
typealias AppColors = AdminColorScheme

@Immutable
data class AdminThemeConfig(
    val themeName: String = "RushIt Coral",
    val accentName: String = "Amber",
    val backgroundStyle: String = "Gradient",
    val sectionRadius: Float = 18f,
    val cardStyle: String = "Elevated",
    val buttonStyle: String = "Filled",
    val typographyScale: Float = 1.0f,
    val previewTextTone: String = "Ink",
    val animationsEnabled: Boolean = true,
) {
    val activePrimary: Color get() = AdminColorScheme.brandColorFor(themeName)
    val activePrimarySoft: Color get() = AdminColorScheme.brandSoftFor(themeName)
    val activeAccent: Color get() = AdminColorScheme.accentColorFor(accentName)
    val cardCornerRadius: Dp get() = sectionRadius.coerceIn(8f, 28f).dp
}

val LocalAdminThemeConfig = staticCompositionLocalOf { AdminThemeConfig() }

object AdminTypography {
    val fontFamily = FontFamily(
        Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
        Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
        Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
        Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold),
        Font(R.font.plus_jakarta_sans_extrabold, FontWeight.ExtraBold),
    )

    fun scaled(scale: Float = 1.0f): Typography {
        val s = scale.coerceIn(0.88f, 1.15f)
        return Typography(
            headlineLarge = TextStyle(
                fontFamily = fontFamily,
                fontSize = (28f * s).sp,
                lineHeight = (34f * s).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = AdminColorScheme.primaryText,
            ),
            headlineMedium = TextStyle(
                fontFamily = fontFamily,
                fontSize = (23f * s).sp,
                lineHeight = (29f * s).sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = AdminColorScheme.primaryText,
            ),
            headlineSmall = TextStyle(
                fontFamily = fontFamily,
                fontSize = (19f * s).sp,
                lineHeight = (25f * s).sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.2).sp,
                color = AdminColorScheme.primaryText,
            ),
            titleLarge = TextStyle(
                fontFamily = fontFamily,
                fontSize = (17f * s).sp,
                lineHeight = (23f * s).sp,
                fontWeight = FontWeight.Bold,
                color = AdminColorScheme.primaryText,
            ),
            titleMedium = TextStyle(
                fontFamily = fontFamily,
                fontSize = (15f * s).sp,
                lineHeight = (21f * s).sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminColorScheme.primaryText,
            ),
            titleSmall = TextStyle(
                fontFamily = fontFamily,
                fontSize = (13f * s).sp,
                lineHeight = (18f * s).sp,
                fontWeight = FontWeight.SemiBold,
                color = AdminColorScheme.primaryText,
            ),
            bodyLarge = TextStyle(
                fontFamily = fontFamily,
                fontSize = (15f * s).sp,
                lineHeight = (22f * s).sp,
                fontWeight = FontWeight.Normal,
                color = AdminColorScheme.primaryText,
            ),
            bodyMedium = TextStyle(
                fontFamily = fontFamily,
                fontSize = (13.5f * s).sp,
                lineHeight = (19.5f * s).sp,
                fontWeight = FontWeight.Normal,
                color = AdminColorScheme.primaryText,
            ),
            bodySmall = TextStyle(
                fontFamily = fontFamily,
                fontSize = (12f * s).sp,
                lineHeight = (17f * s).sp,
                fontWeight = FontWeight.Normal,
                color = AdminColorScheme.secondaryText,
            ),
            labelLarge = TextStyle(
                fontFamily = fontFamily,
                fontSize = (14f * s).sp,
                lineHeight = (19f * s).sp,
                fontWeight = FontWeight.Bold,
            ),
            labelMedium = TextStyle(
                fontFamily = fontFamily,
                fontSize = (12f * s).sp,
                lineHeight = (16f * s).sp,
                fontWeight = FontWeight.SemiBold,
            ),
            labelSmall = TextStyle(
                fontFamily = fontFamily,
                fontSize = (10.5f * s).sp,
                lineHeight = (14f * s).sp,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }

    val material: Typography = scaled(1.0f)
}

typealias AppTypography = AdminTypography

object AdminShapes {
    val card = RoundedCornerShape(18.dp)
    val cardLarge = RoundedCornerShape(22.dp)
    val cardCompact = RoundedCornerShape(14.dp)
    val input = RoundedCornerShape(14.dp)
    val button = RoundedCornerShape(14.dp)
    val pill = CircleShape
    val dialog = RoundedCornerShape(24.dp)
    val bottomBar = RoundedCornerShape(26.dp)

    fun materialShapes(cardRadiusDp: Float = 18f): Shapes = Shapes(
        extraSmall = RoundedCornerShape(8.dp),
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(cardRadiusDp.coerceIn(8f, 28f).dp),
        large = RoundedCornerShape(22.dp),
        extraLarge = RoundedCornerShape(28.dp),
    )
}

typealias AppShapes = AdminShapes

object AdminSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 20.dp
    val xxl = 28.dp

    val screen = 18.dp
    val card = 16.dp
    val compact = 8.dp
    val section = 18.dp
    val buttonHeight = 50.dp
    val bottomBarClearance = 96.dp
}

typealias AppSpacing = AdminSpacing

@Composable
fun AdminTheme(
    config: AdminThemeConfig = AdminThemeConfig(),
    content: @Composable () -> Unit,
) {
    val activePrimary = config.activePrimary
    val activePrimarySoft = config.activePrimarySoft
    val activeAccent = config.activeAccent

    val colorScheme = lightColorScheme(
        primary = activePrimary,
        onPrimary = Color.White,
        primaryContainer = activePrimarySoft,
        onPrimaryContainer = AdminColorScheme.primaryText,
        secondary = AdminColorScheme.secondary,
        onSecondary = Color.White,
        secondaryContainer = AdminColorScheme.surfaceSecondary,
        onSecondaryContainer = AdminColorScheme.primaryText,
        tertiary = activeAccent,
        onTertiary = AdminColorScheme.primaryText,
        tertiaryContainer = AdminColorScheme.accentSoft,
        onTertiaryContainer = AdminColorScheme.primaryText,
        background = AdminColorScheme.background,
        onBackground = AdminColorScheme.primaryText,
        surface = AdminColorScheme.surface,
        onSurface = AdminColorScheme.primaryText,
        surfaceVariant = AdminColorScheme.surfaceSecondary,
        onSurfaceVariant = AdminColorScheme.secondaryText,
        surfaceTint = Color.Transparent,
        outline = AdminColorScheme.divider,
        outlineVariant = AdminColorScheme.divider,
        error = AdminColorScheme.error,
        onError = Color.White,
        errorContainer = AdminColorScheme.errorSoft,
        onErrorContainer = AdminColorScheme.error,
    )

    CompositionLocalProvider(LocalAdminThemeConfig provides config) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AdminTypography.scaled(config.typographyScale),
            shapes = AdminShapes.materialShapes(config.sectionRadius),
            content = content,
        )
    }
}

@Composable
fun RushItAdminTheme(
    config: AdminThemeConfig = AdminThemeConfig(),
    content: @Composable () -> Unit,
) {
    AdminTheme(config = config, content = content)
}

@Composable
fun adminBackgroundBrush(style: String = LocalAdminThemeConfig.current.backgroundStyle): Brush {
    val themeConfig = LocalAdminThemeConfig.current
    return when (style) {
        "Gradient" -> Brush.verticalGradient(
            colors = listOf(
                themeConfig.activePrimarySoft.copy(alpha = 0.65f),
                AdminColorScheme.background,
                AdminColorScheme.background,
            )
        )
        "Warm" -> Brush.verticalGradient(
            colors = listOf(
                AdminColorScheme.surfaceSecondary,
                AdminColorScheme.background,
            )
        )
        else -> Brush.verticalGradient(
            colors = listOf(
                AdminColorScheme.background,
                AdminColorScheme.background,
            )
        )
    }
}
