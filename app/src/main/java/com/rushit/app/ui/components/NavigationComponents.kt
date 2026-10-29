package com.rushit.app.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.domain.CartTotals
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.CtaRed
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink

data class MainDestination(val route: String, val label: String, @DrawableRes val icon: Int)

val mainDestinations = listOf(
    MainDestination("home", "Home", R.drawable.ic_home_01),
    MainDestination("categories", "Categories", R.drawable.ic_grid_view),
    MainDestination("orders", "Orders", R.drawable.ic_shopping_bag_01),
    MainDestination("account", "Account", R.drawable.ic_user_02),
)

@Composable
fun CartSummary(totals: CartTotals, onClick: () -> Unit, modifier: Modifier = Modifier) {
    if (totals.itemCount <= 0) return
    val items = "${totals.itemCount} ${if (totals.itemCount == 1) "item" else "items"}"
    Row(
        modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(26.dp)).background(CtaRed)
            .clickable(role = Role.Button, onClickLabel = "View cart", onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = "View cart, $items, total ${formatPrice(totals.totalPaise)}" }
            .heightIn(min = 64.dp).padding(start = 12.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(DeepRed), contentAlignment = Alignment.Center) {
            RushIcon(R.drawable.ic_shopping_cart_01, null, Modifier.size(25.dp), CardWhite)
        }
        Spacer(Modifier.width(10.dp))
        Text("$items · ${formatPrice(totals.totalPaise)}", Modifier.weight(1f), color = CardWhite, style = MaterialTheme.typography.labelLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(4.dp))
        Row(Modifier.clip(CircleShape).background(DeepRed).padding(horizontal = 12.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("View Cart", color = CardWhite, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.width(6.dp))
            RushIcon(R.drawable.ic_arrow_right_01, null, Modifier.size(16.dp), CardWhite)
        }
    }
}

@Composable
fun RushBottomBar(currentRoute: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().background(Cream)) {
        HorizontalDivider(color = Border.copy(alpha = .65f))
        Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.SpaceEvenly) {
            mainDestinations.forEach { destination ->
                val selected = destination.route == currentRoute
                Column(
                    Modifier.weight(1f).heightIn(min = 64.dp)
                        .selectable(selected = selected, role = Role.Tab, onClick = { onSelect(destination.route) })
                        .padding(top = 8.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    RushIcon(destination.icon, null, Modifier.size(23.dp), if (selected) Coral else Ink)
                    Spacer(Modifier.height(4.dp))
                    Text(destination.label, color = if (selected) Coral else Ink, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp, maxLines = 1)
                }
            }
        }
    }
}
