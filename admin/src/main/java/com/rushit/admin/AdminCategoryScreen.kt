package com.rushit.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

fun categoryEmojiBadge(name: String): String = when {
    name.contains("fruit", true) -> "🍎"
    name.contains("veget", true) -> "🥬"
    name.contains("dairy", true) || name.contains("milk", true) -> "🥛"
    name.contains("baker", true) || name.contains("bread", true) -> "🥐"
    name.contains("grocer", true) || name.contains("staple", true) -> "🧺"
    name.contains("snack", true) -> "🍿"
    name.contains("drink", true) || name.contains("bever", true) -> "🧃"
    name.contains("care", true) || name.contains("beauty", true) -> "🧴"
    else -> "📦"
}

@Composable
fun CategoryManagementScreen(
    categories: MutableList<String>,
    products: MutableList<String>,
    onSave: (String) -> Unit,
    onOpenRoles: () -> Unit = {},
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageCategories
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary

    var showEditor by remember { mutableStateOf(false) }
    var editIndex by remember { mutableStateOf<Int?>(null) }
    var draft by remember { mutableStateOf("") }
    var editorError by remember { mutableStateOf("") }
    var deleteIndex by remember { mutableStateOf<Int?>(null) }
    var search by rememberSaveable { mutableStateOf("") }
    var visibilityFilter by rememberSaveable { mutableStateOf("All") }
    var alphabetical by rememberSaveable { mutableStateOf(false) }

    // Precompute product counts per category name efficiently
    val productCountsByCategory by remember(products.toList()) {
        derivedStateOf {
            val counts = mutableMapOf<String, Int>()
            products.forEach { raw ->
                val clean = raw.substringBefore(" ⟐ ")
                val cat = clean.split(" · ").getOrNull(3)?.trim().orEmpty()
                if (cat.isNotBlank()) {
                    counts[cat] = (counts[cat] ?: 0) + 1
                }
            }
            counts
        }
    }

    val visibleCategories by remember(categories.toList(), search, visibilityFilter, alphabetical) {
        derivedStateOf {
            categories.withIndex()
                .filter { (_, value) ->
                    val name = value.substringBefore('|')
                    val isVis = value.endsWith("Visible")
                    name.contains(search, ignoreCase = true) && when (visibilityFilter) {
                        "Visible" -> isVis
                        "Hidden" -> !isVis
                        else -> true
                    }
                }
                .let { list ->
                    if (alphabetical) {
                        list.sortedBy { indexed -> indexed.value.substringBefore('|').lowercase() }
                    } else {
                        list
                    }
                }
        }
    }

    val visibleCount = remember(categories.toList()) { categories.count { it.endsWith("Visible") } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .adminPageInsets(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            ScreenHeader(
                title = "Categories",
                subtitle = "${categories.size} categories · $visibleCount visible in local preview",
                onAction = if (canManage) ({
                    draft = ""
                    editIndex = null
                    editorError = ""
                    showEditor = true
                }) else null,
                action = "+ Add",
            )
        }
        if (!canManage) {
            item {
                RoleAccessBanner(
                    roleName = permissions.roleName,
                    requiredCapability = "Manage Categories",
                    onSwitchRole = onOpenRoles,
                )
            }
        }
        item { DemoNotice() }
        item {
            AdminSearchField(
                value = search,
                onValueChange = { search = it },
                placeholder = "Search categories…",
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                item {
                    AdminFilterChip(
                        selected = visibilityFilter == "All",
                        onClick = { visibilityFilter = "All" },
                        label = "All",
                        countBadge = categories.size,
                    )
                }
                item {
                    AdminFilterChip(
                        selected = visibilityFilter == "Visible",
                        onClick = { visibilityFilter = "Visible" },
                        label = "Visible",
                        countBadge = visibleCount,
                    )
                }
                item {
                    AdminFilterChip(
                        selected = visibilityFilter == "Hidden",
                        onClick = { visibilityFilter = "Hidden" },
                        label = "Hidden",
                        countBadge = categories.size - visibleCount,
                    )
                }
                item {
                    AdminFilterChip(
                        selected = alphabetical,
                        onClick = { alphabetical = true },
                        label = "Sort: A–Z",
                    )
                }
                item {
                    AdminFilterChip(
                        selected = !alphabetical,
                        onClick = { alphabetical = false },
                        label = "Manual Order",
                    )
                }
            }
        }

        if (visibleCategories.isEmpty()) {
            item {
                EmptyState(
                    title = "No matching categories",
                    message = "Adjust your search filter or add a new category.",
                    actionLabel = if (canManage) "Add Category" else null,
                    onAction = {
                        draft = ""
                        editIndex = null
                        editorError = ""
                        showEditor = true
                    },
                )
            }
        }

        itemsIndexed(
            items = visibleCategories,
            key = { _, indexed -> "${indexed.index}:${indexed.value}" },
        ) { _, indexed ->
            val index = indexed.index
            val item = indexed.value
            val name = item.substringBefore('|')
            val visible = item.endsWith("Visible")
            val count = productCountsByCategory[name] ?: 0

            AdminSurfaceCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFFFF2EC)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(categoryEmojiBadge(name), fontSize = 22.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = AdminColorScheme.primaryText,
                            )
                            StatusChip(if (visible) "Visible" else "Hidden")
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "$count ${if (count == 1) "product" else "products"} linked",
                            color = AdminColorScheme.secondaryText,
                            fontSize = 12.sp,
                        )
                        if (count > 0) {
                            Text(
                                text = "Reassign linked products before deleting this category.",
                                color = AdminColorScheme.warning,
                                fontSize = 11.sp,
                            )
                        }
                    }
                    AdminSwitch(
                        checked = visible,
                        enabled = canManage,
                        onCheckedChange = { checked ->
                            if (canManage && index in categories.indices) {
                                categories[index] = "$name|${if (checked) "Visible" else "Hidden"}"
                                onSave("Category '$name' ${if (checked) "shown" else "hidden"} locally")
                            }
                        },
                    )
                }

                if (canManage) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(
                            onClick = {
                                draft = name
                                editIndex = index
                                editorError = ""
                                showEditor = true
                            },
                        ) {
                            Icon(AdminIcons.Edit, contentDescription = null, tint = brand, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Rename", color = brand, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                        }
                        TextButton(
                            onClick = {
                                if (index > 0) {
                                    val moved = categories.removeAt(index)
                                    categories.add(index - 1, moved)
                                    onSave("Category '$name' moved up")
                                }
                            },
                            enabled = index > 0 && !alphabetical,
                        ) {
                            Text("↑ Up", color = if (index > 0 && !alphabetical) AdminColorScheme.primaryText else AdminColorScheme.disabled, fontSize = 12.sp)
                        }
                        TextButton(
                            onClick = {
                                if (index < categories.lastIndex) {
                                    val moved = categories.removeAt(index)
                                    categories.add(index + 1, moved)
                                    onSave("Category '$name' moved down")
                                }
                            },
                            enabled = index < categories.lastIndex && !alphabetical,
                        ) {
                            Text("↓ Down", color = if (index < categories.lastIndex && !alphabetical) AdminColorScheme.primaryText else AdminColorScheme.disabled, fontSize = 12.sp)
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = { deleteIndex = index },
                            enabled = count == 0,
                        ) {
                            Text(
                                text = if (count == 0) "Delete" else "In use ($count)",
                                color = if (count == 0) AdminColorScheme.error else AdminColorScheme.disabled,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                            )
                        }
                    }
                }
            }
        }

        if (canManage) {
            item {
                PrimaryButton(
                    label = "+ Add Category",
                    modifier = Modifier.fillMaxWidth(),
                    onClick = {
                        draft = ""
                        editIndex = null
                        editorError = ""
                        showEditor = true
                    },
                )
            }
        }
    }

    if (showEditor) {
        AdminDialog(
            title = if (editIndex == null) "Add Category" else "Edit Category",
            message = "Category names are used across local Catalog filters and Home Preview sections.",
            confirmLabel = "Save Category",
            onConfirm = {
                val clean = draft.trim().replace('|', ' ').replace("##", " ")
                editorError = when {
                    clean.isBlank() -> "Enter a category name."
                    categories.any {
                        it.substringBefore('|').equals(clean, true) &&
                            (editIndex == null || categories.indexOf(it) != editIndex)
                    } -> "A category with that name already exists."
                    else -> ""
                }
                if (editorError.isBlank()) {
                    val index = editIndex
                    if (index == null) {
                        categories.add("$clean|Visible")
                        onSave("Category '$clean' added locally")
                    } else if (index in categories.indices) {
                        val oldName = categories[index].substringBefore('|')
                        val status = categories[index].substringAfter('|', "Visible")
                        categories[index] = "$clean|$status"
                        products.indices.forEach { productIndex ->
                            val rawProduct = products[productIndex]
                            val visiblePart = rawProduct.substringBefore(" ⟐ ")
                            val metaPart = if (rawProduct.contains(" ⟐ ")) " ⟐ " + rawProduct.substringAfter(" ⟐ ") else ""
                            val parts = visiblePart.split(" · ").toMutableList()
                            if (parts.getOrNull(3) == oldName) {
                                parts[3] = clean
                                products[productIndex] = parts.joinToString(" · ") + metaPart
                            }
                        }
                        onSave("Category '$oldName' renamed to '$clean'")
                    }
                    showEditor = false
                }
            },
            onDismiss = { showEditor = false },
        ) {
            AdminField(
                label = "Category name",
                value = draft,
                onValue = { draft = it.take(40) },
            )
            if (editorError.isNotBlank()) {
                Text(editorError, color = AdminColorScheme.error, fontSize = 12.sp)
            }
        }
    }

    deleteIndex?.let { index ->
        val categoryName = categories.getOrNull(index)?.substringBefore('|').orEmpty()
        AdminDialog(
            title = "Delete category?",
            message = "'$categoryName' will be removed from this local demo catalog.",
            confirmLabel = "Delete",
            isDestructive = true,
            onConfirm = {
                if (index in categories.indices && (productCountsByCategory[categoryName] ?: 0) == 0) {
                    categories.removeAt(index)
                    onSave("Category '$categoryName' deleted locally")
                }
                deleteIndex = null
            },
            onDismiss = { deleteIndex = null },
        )
    }
}
