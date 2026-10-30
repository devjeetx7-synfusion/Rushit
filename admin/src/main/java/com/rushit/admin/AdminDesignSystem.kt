package com.rushit.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Central palette and layout tokens for the Admin prototype. */
object AppColors {
    val primary = Color(0xFFE7473C)
    val secondary = Color(0xFFF47B6C)
    val accent = Color(0xFFFFB84D)
    val background = Color(0xFFFFFDF7)
    val text = Color(0xFF292524)
    val muted = Color(0xFF78716C)
    val success = Color(0xFF27845B)
    val warning = Color(0xFFB54708)
    val error = Color(0xFFB42318)
    val divider = Color(0xFFEDE8DF)
    val input = Color(0xFFFAF7F1)
    val surface = Color.White
    val disabled = Color(0xFFB8B1A8)
    val info = Color(0xFF356E9A)
}

object AppTypography {
    val material = Typography(
        headlineLarge = TextStyle(fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Bold, color = AppColors.text),
        headlineMedium = TextStyle(fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Bold, color = AppColors.text),
        headlineSmall = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold, color = AppColors.text),
        titleLarge = TextStyle(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, color = AppColors.text),
        titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.SemiBold, color = AppColors.text),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 23.sp, color = AppColors.text),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, color = AppColors.text),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, color = AppColors.muted),
        labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold),
        labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium),
    )
}

object AppShapes {
    val card = RoundedCornerShape(18.dp)
    val input = RoundedCornerShape(14.dp)
    val button = RoundedCornerShape(15.dp)
    val pill = RoundedCornerShape(50)
}

object AppSpacing {
    val screen = 20.dp
    val card = 16.dp
    val compact = 8.dp
    val section = 20.dp
    val buttonHeight = 50.dp
}

@Composable
fun RushItAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = AppColors.primary,
            onPrimary = Color.White,
            secondary = AppColors.accent,
            onSecondary = AppColors.text,
            background = AppColors.background,
            onBackground = AppColors.text,
            surface = AppColors.surface,
            onSurface = AppColors.text,
            surfaceVariant = Color(0xFFF6F1EA),
            onSurfaceVariant = AppColors.muted,
            outline = AppColors.divider,
            error = AppColors.error,
        ),
        typography = AppTypography.material,
        content = content,
    )
}

@Composable
fun AdminSurfaceCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = AppShapes.card,
        color = AppColors.surface,
        border = BorderStroke(1.dp, AppColors.divider),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(AppSpacing.card), content = content)
    }
}
