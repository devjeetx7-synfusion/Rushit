package com.rushit.admin

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Custom vector icon set for RushIt Admin V2.
 * Uses clean 24dp geometry with rounded caps and joins — no Unicode glyph fallbacks for navigation or actions.
 */
object AdminIcons {

    private fun strokedIcon(
        name: String,
        strokeWidth: Float = 1.85f,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply(block).build()

    val Home: ImageVector = strokedIcon("AdminHome") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3.5f, 10.8f)
            lineTo(12f, 3.5f)
            lineTo(20.5f, 10.8f)
            moveTo(5.5f, 9.5f)
            verticalLineTo(19.2f)
            curveTo(5.5f, 19.9f, 6.1f, 20.5f, 6.8f, 20.5f)
            horizontalLineTo(17.2f)
            curveTo(17.9f, 20.5f, 18.5f, 19.9f, 18.5f, 19.2f)
            verticalLineTo(9.5f)
            moveTo(9.8f, 20.5f)
            verticalLineTo(14.5f)
            horizontalLineTo(14.2f)
            verticalLineTo(20.5f)
        }
    }

    val Catalog: ImageVector = strokedIcon("AdminCatalog") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            // Top-left box
            moveTo(4.5f, 4f)
            horizontalLineTo(9.5f)
            curveTo(10.05f, 4f, 10.5f, 4.45f, 10.5f, 5f)
            verticalLineTo(9.5f)
            curveTo(10.5f, 10.05f, 10.05f, 10.5f, 9.5f, 10.5f)
            horizontalLineTo(4.5f)
            curveTo(3.95f, 10.5f, 3.5f, 10.05f, 3.5f, 9.5f)
            verticalLineTo(5f)
            curveTo(3.5f, 4.45f, 3.95f, 4f, 4.5f, 4f)
            close()
            // Top-right box
            moveTo(14.5f, 4f)
            horizontalLineTo(19.5f)
            curveTo(20.05f, 4f, 20.5f, 4.45f, 20.5f, 5f)
            verticalLineTo(9.5f)
            curveTo(20.5f, 10.05f, 20.05f, 10.5f, 19.5f, 10.5f)
            horizontalLineTo(14.5f)
            curveTo(13.95f, 10.5f, 13.5f, 10.05f, 13.5f, 9.5f)
            verticalLineTo(5f)
            curveTo(13.5f, 4.45f, 13.95f, 4f, 14.5f, 4f)
            close()
            // Bottom-left box
            moveTo(4.5f, 13.5f)
            horizontalLineTo(9.5f)
            curveTo(10.05f, 13.5f, 10.5f, 13.95f, 10.5f, 14.5f)
            verticalLineTo(19f)
            curveTo(10.5f, 19.55f, 10.05f, 20f, 9.5f, 20f)
            horizontalLineTo(4.5f)
            curveTo(3.95f, 20f, 3.5f, 19.55f, 3.5f, 19f)
            verticalLineTo(14.5f)
            curveTo(3.5f, 13.95f, 3.95f, 13.5f, 4.5f, 13.5f)
            close()
            // Bottom-right box
            moveTo(14.5f, 13.5f)
            horizontalLineTo(19.5f)
            curveTo(20.05f, 13.5f, 20.5f, 13.95f, 20.5f, 14.5f)
            verticalLineTo(19f)
            curveTo(20.5f, 19.55f, 20.05f, 20f, 19.5f, 20f)
            horizontalLineTo(14.5f)
            curveTo(13.95f, 20f, 13.5f, 19.55f, 13.5f, 19f)
            verticalLineTo(14.5f)
            curveTo(13.5f, 13.95f, 13.95f, 13.5f, 14.5f, 13.5f)
            close()
        }
    }

    val Orders: ImageVector = strokedIcon("AdminOrders") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(6f, 4f)
            horizontalLineTo(18f)
            curveTo(19.1f, 4f, 20f, 4.9f, 20f, 6f)
            verticalLineTo(18.5f)
            curveTo(20f, 19.6f, 19.1f, 20.5f, 18f, 20.5f)
            horizontalLineTo(6f)
            curveTo(4.9f, 20.5f, 4f, 19.6f, 4f, 18.5f)
            verticalLineTo(6f)
            curveTo(4f, 4.9f, 4.9f, 4f, 6f, 4f)
            close()
            moveTo(8f, 9f)
            horizontalLineTo(16f)
            moveTo(8f, 13f)
            horizontalLineTo(16f)
            moveTo(8f, 16.8f)
            horizontalLineTo(12.5f)
        }
    }

    val Themes: ImageVector = strokedIcon("AdminThemes") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3.2f)
            curveTo(7.1f, 3.2f, 3.2f, 7.1f, 3.2f, 12f)
            curveTo(3.2f, 16.9f, 7.1f, 20.8f, 11.5f, 20.8f)
            curveTo(13.1f, 20.8f, 14.2f, 19.7f, 14.2f, 18.3f)
            curveTo(14.2f, 17.6f, 13.9f, 17f, 13.5f, 16.5f)
            curveTo(13.1f, 16f, 13.4f, 15.2f, 14.2f, 15.2f)
            horizontalLineTo(16f)
            curveTo(18.7f, 15.2f, 20.8f, 13.1f, 20.8f, 10.4f)
            curveTo(20.8f, 6.4f, 16.9f, 3.2f, 12f, 3.2f)
            close()
            moveTo(8f, 10f)
            lineTo(8.05f, 10f)
            moveTo(11f, 7.5f)
            lineTo(11.05f, 7.5f)
            moveTo(15f, 8.5f)
            lineTo(15.05f, 8.5f)
            moveTo(7.5f, 13.8f)
            lineTo(7.55f, 13.8f)
        }
    }

    val More: ImageVector = strokedIcon("AdminMore") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.1f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 7f)
            horizontalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(19f)
            moveTo(5f, 17f)
            horizontalLineTo(14f)
        }
    }

    val BrandCart: ImageVector = strokedIcon("BrandCart") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(2.8f, 4.5f)
            horizontalLineTo(5.6f)
            lineTo(7.8f, 15.2f)
            horizontalLineTo(18.2f)
            lineTo(20.6f, 7.8f)
            horizontalLineTo(6.5f)
        }
        path(
            fill = SolidColor(Color.Black),
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.2f, 18.2f)
            curveTo(10.1f, 18.2f, 10.8f, 18.9f, 10.8f, 19.8f)
            curveTo(10.8f, 20.7f, 10.1f, 21.4f, 9.2f, 21.4f)
            curveTo(8.3f, 21.4f, 7.6f, 20.7f, 7.6f, 19.8f)
            curveTo(7.6f, 18.9f, 8.3f, 18.2f, 9.2f, 18.2f)
            close()
            moveTo(17.0f, 18.2f)
            curveTo(17.9f, 18.2f, 18.6f, 18.9f, 18.6f, 19.8f)
            curveTo(18.6f, 20.7f, 17.9f, 21.4f, 17.0f, 21.4f)
            curveTo(16.1f, 21.4f, 15.4f, 20.7f, 15.4f, 19.8f)
            curveTo(15.4f, 18.9f, 16.1f, 18.2f, 17.0f, 18.2f)
            close()
        }
    }

    val Bell: ImageVector = strokedIcon("Bell") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(18f, 9.5f)
            curveTo(18f, 6.2f, 15.3f, 3.5f, 12f, 3.5f)
            curveTo(8.7f, 3.5f, 6f, 6.2f, 6f, 9.5f)
            verticalLineTo(13.2f)
            lineTo(4.3f, 15.6f)
            curveTo(3.9f, 16.2f, 4.3f, 17f, 5.1f, 17f)
            horizontalLineTo(18.9f)
            curveTo(19.7f, 17f, 20.1f, 16.2f, 19.7f, 15.6f)
            lineTo(18f, 13.2f)
            close()
            moveTo(10f, 19.5f)
            curveTo(10.5f, 20.3f, 11.2f, 20.7f, 12f, 20.7f)
            curveTo(12.8f, 20.7f, 13.5f, 20.3f, 14f, 19.5f)
        }
    }

    val Search: ImageVector = strokedIcon("Search") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(11f, 4f)
            curveTo(14.87f, 4f, 18f, 7.13f, 18f, 11f)
            curveTo(18f, 14.87f, 14.87f, 18f, 11f, 18f)
            curveTo(7.13f, 18f, 4f, 14.87f, 4f, 11f)
            curveTo(4f, 7.13f, 7.13f, 4f, 11f, 4f)
            close()
            moveTo(16.2f, 16.2f)
            lineTo(20.2f, 20.2f)
        }
    }

    val Back: ImageVector = strokedIcon("Back") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(14.8f, 5.5f)
            lineTo(8.3f, 12f)
            lineTo(14.8f, 18.5f)
        }
    }

    val ChevronRight: ImageVector = strokedIcon("ChevronRight") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(9.5f, 6f)
            lineTo(15.5f, 12f)
            lineTo(9.5f, 18f)
        }
    }

    val Plus: ImageVector = strokedIcon("Plus") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 5f)
            verticalLineTo(19f)
            moveTo(5f, 12f)
            horizontalLineTo(19f)
        }
    }

    val Minus: ImageVector = strokedIcon("Minus") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.0f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5.5f, 12f)
            horizontalLineTo(18.5f)
        }
    }

    val Check: ImageVector = strokedIcon("Check") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2.2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 12.5f)
            lineTo(9.8f, 17.2f)
            lineTo(19.2f, 7.2f)
        }
    }

    val Edit: ImageVector = strokedIcon("Edit") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(15.5f, 4.5f)
            lineTo(19.5f, 8.5f)
            lineTo(8.5f, 19.5f)
            horizontalLineTo(4.5f)
            verticalLineTo(15.5f)
            close()
        }
    }

    val Trash: ImageVector = strokedIcon("Trash") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(4.5f, 7f)
            horizontalLineTo(19.5f)
            moveTo(9.5f, 7f)
            verticalLineTo(4.5f)
            horizontalLineTo(14.5f)
            verticalLineTo(7f)
            moveTo(6.5f, 7f)
            lineTo(7.5f, 19.5f)
            horizontalLineTo(16.5f)
            lineTo(17.5f, 7f)
        }
    }

    val Duplicate: ImageVector = strokedIcon("Duplicate") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(9f, 9f)
            horizontalLineTo(19f)
            verticalLineTo(19f)
            horizontalLineTo(9f)
            close()
            moveTo(15f, 9f)
            verticalLineTo(5f)
            horizontalLineTo(5f)
            verticalLineTo(15f)
            horizontalLineTo(9f)
        }
    }

    val Package: ImageVector = strokedIcon("Package") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            lineTo(20f, 7.5f)
            verticalLineTo(16.5f)
            lineTo(12f, 21f)
            lineTo(4f, 16.5f)
            verticalLineTo(7.5f)
            close()
            moveTo(4f, 7.5f)
            lineTo(12f, 12f)
            lineTo(20f, 7.5f)
            moveTo(12f, 12f)
            verticalLineTo(21f)
        }
    }

    val Users: ImageVector = strokedIcon("Users") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(9.5f, 6.5f)
            curveTo(11.16f, 6.5f, 12.5f, 7.84f, 12.5f, 9.5f)
            curveTo(12.5f, 11.16f, 11.16f, 12.5f, 9.5f, 12.5f)
            curveTo(7.84f, 12.5f, 6.5f, 11.16f, 6.5f, 9.5f)
            curveTo(6.5f, 7.84f, 7.84f, 6.5f, 9.5f, 6.5f)
            close()
            moveTo(4f, 19.5f)
            curveTo(4f, 16.5f, 6.5f, 15f, 9.5f, 15f)
            curveTo(12.5f, 15f, 15f, 16.5f, 15f, 19.5f)
            moveTo(16f, 7.2f)
            curveTo(17.5f, 7.6f, 18.5f, 8.8f, 18.5f, 10.3f)
            curveTo(18.5f, 11.8f, 17.5f, 13f, 16f, 13.4f)
            moveTo(17f, 15.5f)
            curveTo(18.8f, 16.1f, 20f, 17.4f, 20f, 19.5f)
        }
    }

    val Tag: ImageVector = strokedIcon("Tag") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3.5f, 11.5f)
            verticalLineTo(4.5f)
            horizontalLineTo(10.5f)
            lineTo(20f, 14f)
            lineTo(13f, 21f)
            close()
            moveTo(7.5f, 7.5f)
            lineTo(7.55f, 7.5f)
        }
    }

    val Sparkles: ImageVector = strokedIcon("Sparkles") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            lineTo(14.2f, 9.8f)
            lineTo(21f, 12f)
            lineTo(14.2f, 14.2f)
            lineTo(12f, 21f)
            lineTo(9.8f, 14.2f)
            lineTo(3f, 12f)
            lineTo(9.8f, 9.8f)
            close()
        }
    }

    val Calendar: ImageVector = strokedIcon("Calendar") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 5.5f)
            horizontalLineTo(19f)
            curveTo(19.8f, 5.5f, 20.5f, 6.2f, 20.5f, 7f)
            verticalLineTo(19f)
            curveTo(20.5f, 19.8f, 19.8f, 20.5f, 19f, 20.5f)
            horizontalLineTo(5f)
            curveTo(4.2f, 20.5f, 3.5f, 19.8f, 3.5f, 19f)
            verticalLineTo(7f)
            curveTo(3.5f, 6.2f, 4.2f, 5.5f, 5f, 5.5f)
            close()
            moveTo(8f, 3.5f)
            verticalLineTo(7f)
            moveTo(16f, 3.5f)
            verticalLineTo(7f)
            moveTo(3.5f, 10f)
            horizontalLineTo(20.5f)
        }
    }

    val Layout: ImageVector = strokedIcon("Layout") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(4f, 4f)
            horizontalLineTo(20f)
            verticalLineTo(9f)
            horizontalLineTo(4f)
            close()
            moveTo(4f, 12.5f)
            horizontalLineTo(11f)
            verticalLineTo(20f)
            horizontalLineTo(4f)
            close()
            moveTo(14f, 12.5f)
            horizontalLineTo(20f)
            verticalLineTo(20f)
            horizontalLineTo(14f)
            close()
        }
    }

    val PhonePreview: ImageVector = strokedIcon("PhonePreview") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7.5f, 3f)
            horizontalLineTo(16.5f)
            curveTo(17.6f, 3f, 18.5f, 3.9f, 18.5f, 5f)
            verticalLineTo(19f)
            curveTo(18.5f, 20.1f, 17.6f, 21f, 16.5f, 21f)
            horizontalLineTo(7.5f)
            curveTo(6.4f, 21f, 5.5f, 20.1f, 5.5f, 19f)
            verticalLineTo(5f)
            curveTo(5.5f, 3.9f, 6.4f, 3f, 7.5f, 3f)
            close()
            moveTo(10.5f, 18f)
            horizontalLineTo(13.5f)
        }
    }

    val Shield: ImageVector = strokedIcon("Shield") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3f)
            lineTo(19.5f, 6f)
            verticalLineTo(11.5f)
            curveTo(19.5f, 16.2f, 16.3f, 19.6f, 12f, 21f)
            curveTo(7.7f, 19.6f, 4.5f, 16.2f, 4.5f, 11.5f)
            verticalLineTo(6f)
            close()
            moveTo(9.2f, 12f)
            lineTo(11.2f, 14f)
            lineTo(15.2f, 9.8f)
        }
    }

    val Activity: ImageVector = strokedIcon("Activity") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3f, 12f)
            horizontalLineTo(7f)
            lineTo(10f, 5f)
            lineTo(14f, 19f)
            lineTo(17f, 12f)
            horizontalLineTo(21f)
        }
    }

    val Settings: ImageVector = strokedIcon("Settings") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 8.8f)
            curveTo(13.77f, 8.8f, 15.2f, 10.23f, 15.2f, 12f)
            curveTo(15.2f, 13.77f, 13.77f, 15.2f, 12f, 15.2f)
            curveTo(10.23f, 15.2f, 8.8f, 13.77f, 8.8f, 12f)
            curveTo(8.8f, 10.23f, 10.23f, 8.8f, 12f, 8.8f)
            close()
            moveTo(12f, 3.5f)
            verticalLineTo(5.5f)
            moveTo(12f, 18.5f)
            verticalLineTo(20.5f)
            moveTo(3.5f, 12f)
            horizontalLineTo(5.5f)
            moveTo(18.5f, 12f)
            horizontalLineTo(20.5f)
            moveTo(6f, 6f)
            lineTo(7.5f, 7.5f)
            moveTo(16.5f, 16.5f)
            lineTo(18f, 18f)
            moveTo(18f, 6f)
            lineTo(16.5f, 7.5f)
            moveTo(7.5f, 16.5f)
            lineTo(6f, 18f)
        }
    }

    val AlertTriangle: ImageVector = strokedIcon("AlertTriangle") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 4f)
            lineTo(21f, 19.5f)
            horizontalLineTo(3f)
            close()
            moveTo(12f, 10f)
            verticalLineTo(14f)
            moveTo(12f, 17f)
            lineTo(12.05f, 17f)
        }
    }

    val TrendingUp: ImageVector = strokedIcon("TrendingUp") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.95f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(3.5f, 16.5f)
            lineTo(9.5f, 10.5f)
            lineTo(13.5f, 14.5f)
            lineTo(20.5f, 7.5f)
            moveTo(15f, 7.5f)
            horizontalLineTo(20.5f)
            verticalLineTo(13f)
        }
    }

    val Clock: ImageVector = strokedIcon("Clock") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 3.5f)
            curveTo(16.69f, 3.5f, 20.5f, 7.31f, 20.5f, 12f)
            curveTo(20.5f, 16.69f, 16.69f, 20.5f, 12f, 20.5f)
            curveTo(7.31f, 20.5f, 3.5f, 16.69f, 3.5f, 12f)
            curveTo(3.5f, 7.31f, 7.31f, 3.5f, 12f, 3.5f)
            close()
            moveTo(12f, 7.5f)
            verticalLineTo(12.2f)
            lineTo(15.2f, 14.2f)
        }
    }

    val Logout: ImageVector = strokedIcon("Logout") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(10f, 4.5f)
            horizontalLineTo(5.5f)
            verticalLineTo(19.5f)
            horizontalLineTo(10f)
            moveTo(15.5f, 8.5f)
            lineTo(19.5f, 12f)
            lineTo(15.5f, 15.5f)
            moveTo(9.5f, 12f)
            horizontalLineTo(19.5f)
        }
    }

    val ArrowUp: ImageVector = strokedIcon("ArrowUp") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 18.5f)
            verticalLineTo(5.5f)
            moveTo(7.5f, 10f)
            lineTo(12f, 5.5f)
            lineTo(16.5f, 10f)
        }
    }

    val ArrowDown: ImageVector = strokedIcon("ArrowDown") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.9f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(12f, 5.5f)
            verticalLineTo(18.5f)
            moveTo(7.5f, 14f)
            lineTo(12f, 18.5f)
            lineTo(16.5f, 14f)
        }
    }

    val Lock: ImageVector = strokedIcon("Lock") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 10.5f)
            horizontalLineTo(17f)
            curveTo(18.1f, 10.5f, 19f, 11.4f, 19f, 12.5f)
            verticalLineTo(19f)
            curveTo(19f, 20.1f, 18.1f, 21f, 17f, 21f)
            horizontalLineTo(7f)
            curveTo(5.9f, 21f, 5f, 20.1f, 5f, 19f)
            verticalLineTo(12.5f)
            curveTo(5f, 11.4f, 5.9f, 10.5f, 7f, 10.5f)
            close()
            moveTo(8f, 10.5f)
            verticalLineTo(7.5f)
            curveTo(8f, 5.29f, 9.79f, 3.5f, 12f, 3.5f)
            curveTo(14.21f, 3.5f, 16f, 5.29f, 16f, 7.5f)
            verticalLineTo(10.5f)
        }
    }

    val Eye: ImageVector = strokedIcon("Eye") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(2.5f, 12f)
            curveTo(4.5f, 7.5f, 8f, 5f, 12f, 5f)
            curveTo(16f, 5f, 19.5f, 7.5f, 21.5f, 12f)
            curveTo(19.5f, 16.5f, 16f, 19f, 12f, 19f)
            curveTo(8f, 19f, 4.5f, 16.5f, 2.5f, 12f)
            close()
            moveTo(12f, 9.5f)
            curveTo(13.38f, 9.5f, 14.5f, 10.62f, 14.5f, 12f)
            curveTo(14.5f, 13.38f, 13.38f, 14.5f, 12f, 14.5f)
            curveTo(10.62f, 14.5f, 9.5f, 13.38f, 9.5f, 12f)
            curveTo(9.5f, 10.62f, 10.62f, 9.5f, 12f, 9.5f)
            close()
        }
    }

    val ImagePlaceholder: ImageVector = strokedIcon("ImagePlaceholder") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(5f, 5f)
            horizontalLineTo(19f)
            curveTo(20.1f, 5f, 21f, 5.9f, 21f, 7f)
            verticalLineTo(17f)
            curveTo(21f, 18.1f, 20.1f, 19f, 19f, 19f)
            horizontalLineTo(5f)
            curveTo(3.9f, 19f, 3f, 18.1f, 3f, 17f)
            verticalLineTo(7f)
            curveTo(3f, 5.9f, 3.9f, 5f, 5f, 5f)
            close()
            moveTo(8f, 10f)
            curveTo(8.83f, 10f, 9.5f, 9.33f, 9.5f, 8.5f)
            curveTo(9.5f, 7.67f, 8.83f, 7f, 8f, 7f)
            curveTo(7.17f, 7f, 6.5f, 7.67f, 6.5f, 8.5f)
            curveTo(6.5f, 9.33f, 7.17f, 10f, 8f, 10f)
            close()
            moveTo(4f, 17.5f)
            lineTo(9.5f, 12f)
            lineTo(12.5f, 15f)
            lineTo(15f, 12.5f)
            lineTo(20f, 17.5f)
        }
    }

    val Note: ImageVector = strokedIcon("Note") {
        path(
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 1.85f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ) {
            moveTo(7f, 3.5f)
            horizontalLineTo(15.5f)
            lineTo(20.5f, 8.5f)
            verticalLineTo(19f)
            curveTo(20.5f, 20.1f, 19.6f, 21f, 18.5f, 21f)
            horizontalLineTo(7f)
            curveTo(5.9f, 21f, 5f, 20.1f, 5f, 19f)
            verticalLineTo(5.5f)
            curveTo(5f, 4.4f, 5.9f, 3.5f, 7f, 3.5f)
            close()
            moveTo(15f, 3.5f)
            verticalLineTo(8.5f)
            horizontalLineTo(20f)
            moveTo(8.5f, 13f)
            horizontalLineTo(15.5f)
            moveTo(8.5f, 16.5f)
            horizontalLineTo(13f)
        }
    }
}
