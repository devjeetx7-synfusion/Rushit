package com.rushit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.ShoppingMode
import com.rushit.app.ui.theme.Amber
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Peach

/** The selected tab belongs to the shared shop state, not an individual destination. */
@Composable
fun ShoppingModeSwitcher(selectedMode: ShoppingMode, onModeSelected: (ShoppingMode) -> Unit, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        // Leave the next card peeking on phones so horizontal scrolling is discoverable.
        val textScale = LocalDensity.current.fontScale.coerceIn(1f, 1.6f)
        val cardWidth = (maxWidth * (.40f * textScale)).coerceIn(132.dp * textScale, 160.dp * textScale)
        val imageSize = if (cardWidth < 146.dp) 40.dp else 48.dp
        val listState = rememberLazyListState()
        LaunchedEffect(selectedMode) {
            val layout = listState.layoutInfo
            val item = layout.visibleItemsInfo.firstOrNull { it.index == selectedMode.ordinal }
            if (item == null || item.offset < layout.viewportStartOffset || item.offset + item.size > layout.viewportEndOffset) {
                listState.animateScrollToItem(selectedMode.ordinal)
            }
        }
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 3.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(ShoppingMode.entries, key = { it.name }) { mode ->
                val selected = mode == selectedMode
                val background by animateColorAsState(if (selected) CardWhite else Peach.copy(alpha = .65f), label = "mode background")
                val outline by animateColorAsState(if (selected) Amber else Border, label = "mode outline")
                val image = when (mode) {
                    ShoppingMode.GROCERY -> R.drawable.product_tomatoes
                    ShoppingMode.ECOMMERCE -> R.drawable.ecommerce_headphones
                    ShoppingMode.GIFTS -> R.drawable.gift_bouquet
                }
                Surface(
                    modifier = Modifier.width(cardWidth).heightIn(min = 72.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onModeSelected(mode) }),
                    shape = RoundedCornerShape(20.dp), color = background,
                    border = BorderStroke(if (selected) 1.5.dp else 1.dp, outline),
                ) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(imageSize).clip(RoundedCornerShape(14.dp)).background(Peach), contentAlignment = Alignment.Center) {
                            CatalogImage(null, image, null, Modifier.size(imageSize - 2.dp))
                        }
                        Spacer(Modifier.width(9.dp))
                        Text(mode.label, style = MaterialTheme.typography.labelMedium, color = Ink,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
