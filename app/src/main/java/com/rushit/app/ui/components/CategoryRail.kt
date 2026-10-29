package com.rushit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.Category
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.Motion
import com.rushit.app.ui.theme.Peach

/**
 * Horizontally scrolling category cards with an animated selected state.
 *
 * The first entry is always "All" (null id). Selection is owned by the view model, so this rail
 * never keeps its own copy and stays in sync when the mode changes underneath it.
 */
@Composable
fun CategoryRail(
    categories: List<Category>,
    selectedCategoryId: String?,
    onCategorySelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (categories.isEmpty()) return
    val reduced = LocalReducedMotion.current
    val listState = rememberLazyListState()
    // null is the synthetic "All" entry; categories follow in their configured order.
    val entries = remember(categories) { listOf<Category?>(null) + categories }
    val selectedIndex = remember(entries, selectedCategoryId) {
        entries.indexOfFirst { it?.id == selectedCategoryId }.coerceAtLeast(0)
    }

    LaunchedEffect(selectedIndex, entries.size) {
        val layout = listState.layoutInfo
        val visible = layout.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
        val outsideViewport = visible == null ||
            visible.offset < layout.viewportStartOffset ||
            visible.offset + visible.size > layout.viewportEndOffset
        if (outsideViewport) {
            if (reduced) listState.scrollToItem(selectedIndex) else listState.animateScrollToItem(selectedIndex)
        }
    }

    LazyRow(
        state = listState,
        modifier = modifier.fillMaxWidth().selectableGroup(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(entries, key = { _, entry -> entry?.id ?: "all" }) { _, entry ->
            CategoryCard(
                category = entry,
                selected = if (entry == null) selectedCategoryId == null else entry.id == selectedCategoryId,
                onClick = { onCategorySelected(entry?.id) },
            )
        }
    }
}

@Composable
private fun CategoryCard(category: Category?, selected: Boolean, onClick: () -> Unit) {
    val reduced = LocalReducedMotion.current
    val colorSpec = if (reduced) snap<Color>() else tween<Color>(Motion.CATEGORY_SELECTION, easing = FastOutSlowInEasing)
    val background by animateColorAsState(if (selected) Peach.copy(alpha = 0.9f) else CardWhite, animationSpec = colorSpec, label = "category background")
    val borderColor by animateColorAsState(if (selected) Coral else Border, animationSpec = colorSpec, label = "category border")
    val borderWidth by animateDpAsState(
        targetValue = if (selected) 1.5.dp else 1.dp,
        animationSpec = if (reduced) snap() else tween(Motion.CATEGORY_SELECTION),
        label = "category border width",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.08f else 1f,
        animationSpec = if (reduced) snap() else tween(Motion.CATEGORY_SELECTION),
        label = "category icon scale",
    )
    val elevation by animateDpAsState(
        targetValue = if (selected) 2.dp else 0.dp,
        animationSpec = if (reduced) snap() else tween(Motion.CATEGORY_SELECTION),
        label = "category elevation",
    )
    val label = category?.name ?: "All"

    Surface(
        modifier = Modifier
            .widthIn(min = 96.dp, max = 186.dp)
            .heightIn(min = 62.dp)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = background,
        border = BorderStroke(borderWidth, borderColor),
        shadowElevation = elevation,
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(34.dp)
                    .graphicsLayer { scaleX = iconScale; scaleY = iconScale }
                    .clip(CircleShape)
                    .background(if (selected) CardWhite else Peach),
                contentAlignment = Alignment.Center,
            ) {
                if (category == null) {
                    RushIcon(R.drawable.ic_grid_view, null, Modifier.size(19.dp), Coral)
                } else {
                    CatalogImage(category.imageUrl, category.imageRes, null, Modifier.size(30.dp))
                }
            }
            Spacer(Modifier.width(9.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight(if (selected) 700 else 500),
                ),
                color = Ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
