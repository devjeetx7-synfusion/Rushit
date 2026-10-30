package com.rushit.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Window-size driven layout tokens. Breakpoints follow Material window size classes
 * (compact < 600dp, medium 600–839dp, expanded ≥ 840dp) using the actual window
 * width rather than a device or orientation flag.
 */
enum class AdminNavLayout {
    BottomBar,
    Rail,
    Sidebar,
}

@Immutable
data class AdminWindow(
    val widthDp: Int,
    val heightDp: Int,
    val navLayout: AdminNavLayout,
    val kpiColumns: Int,
    val formColumns: Int,
    val listIsTable: Boolean,
    val isLandscape: Boolean,
    val isCompactHeight: Boolean,
    val contentMaxWidth: Dp,
    val pagePadding: Dp,
    val sectionSpacing: Dp,
) {
    val isWide: Boolean get() = widthDp >= 600
    val isExpanded: Boolean get() = navLayout == AdminNavLayout.Sidebar
}

val LocalAdminWindow = staticCompositionLocalOf {
    AdminWindow(
        widthDp = 360,
        heightDp = 800,
        navLayout = AdminNavLayout.BottomBar,
        kpiColumns = 2,
        formColumns = 1,
        listIsTable = false,
        isLandscape = false,
        isCompactHeight = false,
        contentMaxWidth = Dp.Unspecified,
        pagePadding = 18.dp,
        sectionSpacing = 14.dp,
    )
}

fun adminNavLayoutFor(widthDp: Int): AdminNavLayout = when {
    widthDp >= 840 -> AdminNavLayout.Sidebar
    widthDp >= 600 -> AdminNavLayout.Rail
    else -> AdminNavLayout.BottomBar
}

fun adminKpiColumnsFor(widthDp: Int): Int = when {
    widthDp >= 1100 -> 3
    widthDp >= 360 -> 2
    else -> 1
}

fun adminFormColumnsFor(widthDp: Int): Int = if (widthDp >= 700) 2 else 1

@Composable
fun rememberAdminWindow(): AdminWindow {
    val configuration = LocalConfiguration.current
    val width = configuration.screenWidthDp
    val height = configuration.screenHeightDp
    return remember(width, height) {
        val layout = adminNavLayoutFor(width)
        AdminWindow(
            widthDp = width,
            heightDp = height,
            navLayout = layout,
            kpiColumns = adminKpiColumnsFor(width),
            formColumns = adminFormColumnsFor(width),
            listIsTable = width >= 840,
            isLandscape = width > height,
            isCompactHeight = height < 500,
            contentMaxWidth = when {
                width >= 1200 -> 1120.dp
                layout == AdminNavLayout.Sidebar -> 1000.dp
                else -> Dp.Unspecified
            },
            pagePadding = if (width >= 840) 24.dp else 18.dp,
            sectionSpacing = if (height < 500) 10.dp else 14.dp,
        )
    }
}

@Composable
fun AdaptiveContent(
    modifier: Modifier = Modifier,
    maxWidth: Dp = LocalAdminWindow.current.contentMaxWidth,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = if (maxWidth == Dp.Unspecified) Modifier.fillMaxWidth()
            else Modifier.widthIn(max = maxWidth).fillMaxWidth(),
        ) {
            content()
        }
    }
}

/**
 * Places [items] into an even-weight row grid with [columns] cells per row.
 * Remainder cells are filled with spacers so the last row stays aligned.
 */
@Composable
fun AdaptiveGrid(
    columns: Int,
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 10.dp,
    verticalSpacing: Dp = 10.dp,
    content: List<@Composable RowScope.() -> Unit>,
) {
    val cols = columns.coerceAtLeast(1)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(verticalSpacing)) {
        content.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
            ) {
                rowItems.forEach { item -> item() }
                repeat(cols - rowItems.size) { androidx.compose.foundation.layout.Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun AdaptiveFormRow(
    modifier: Modifier = Modifier,
    columns: Int = LocalAdminWindow.current.formColumns,
    spacing: Dp = 10.dp,
    first: @Composable () -> Unit,
    second: @Composable (() -> Unit)? = null,
) {
    if (second == null || columns <= 1) {
        Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(spacing)) {
            first()
            second?.invoke()
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.weight(1f)) { first() }
            Box(Modifier.weight(1f)) { second() }
        }
    }
}

@Composable
fun MeasureWidth(content: @Composable (Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        content(maxWidth)
    }
}

fun compactPagePadding(window: AdminWindow): Dp =
    if (window.isCompactHeight) 12.dp else window.pagePadding

@Composable
fun Modifier.adminPageInsets(): Modifier {
    val pad = LocalAdminWindow.current.pagePadding
    return this.padding(horizontal = pad)
}
