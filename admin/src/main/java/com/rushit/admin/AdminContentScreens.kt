package com.rushit.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ThemeScreen(
    config: AdminThemeConfig,
    onSave: (AdminThemeConfig) -> Unit,
    onPreview: () -> Unit,
    onOpenRoles: () -> Unit,
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageContent
    val paletteNames = listOf("RushIt Coral", "Sunrise", "Forest", "Ocean", "Berry")
    val accentNames = listOf("Amber", "Coral Soft", "Mint", "Sky", "Gold")

    var draftTheme by rememberSaveable(config.themeName) { mutableStateOf(config.themeName) }
    var draftAccent by rememberSaveable(config.accentName) { mutableStateOf(config.accentName) }
    var draftBackground by rememberSaveable(config.backgroundStyle) { mutableStateOf(config.backgroundStyle) }
    var draftRadius by rememberSaveable(config.sectionRadius) { mutableFloatStateOf(config.sectionRadius) }
    var draftButtonStyle by rememberSaveable(config.buttonStyle) { mutableStateOf(config.buttonStyle) }
    var draftCardStyle by rememberSaveable(config.cardStyle) { mutableStateOf(config.cardStyle) }
    var draftTextTone by rememberSaveable(config.previewTextTone) { mutableStateOf(config.previewTextTone) }
    var draftTypeScale by rememberSaveable(config.typographyScale) { mutableFloatStateOf(config.typographyScale) }

    val draft = AdminThemeConfig(
        themeName = draftTheme,
        accentName = draftAccent,
        backgroundStyle = draftBackground,
        sectionRadius = draftRadius,
        cardStyle = draftCardStyle,
        buttonStyle = draftButtonStyle,
        typographyScale = draftTypeScale,
        previewTextTone = draftTextTone,
        animationsEnabled = config.animationsEnabled,
    )
    val hasChanges = draftTheme != config.themeName ||
        draftAccent != config.accentName ||
        draftBackground != config.backgroundStyle ||
        draftRadius != config.sectionRadius ||
        draftButtonStyle != config.buttonStyle ||
        draftCardStyle != config.cardStyle ||
        draftTextTone != config.previewTextTone ||
        draftTypeScale != config.typographyScale

    fun resetDraft() {
        draftTheme = config.themeName
        draftAccent = config.accentName
        draftBackground = config.backgroundStyle
        draftRadius = config.sectionRadius
        draftButtonStyle = config.buttonStyle
        draftCardStyle = config.cardStyle
        draftTextTone = config.previewTextTone
        draftTypeScale = config.typographyScale
    }

    AdaptiveContent(Modifier.fillMaxSize()) {
    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 28.dp),
    ) {
        item { ScreenHeader("Theme & Colors", "Customize Admin appearance · local device only") }
        if (!canManage) item { RoleAccessBanner(permissions.roleName, "Manage Content & Themes", onOpenRoles) }
        item { DemoNotice("Theme changes restyle this Admin app on this device. They do not publish to the RushIt User app.") }

        item {
            AdminSurfaceCard {
                Text("Brand palette", fontWeight = FontWeight.Bold)
                Text("Primary actions, navigation pill and KPI accents follow this palette.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    paletteNames.forEach { name ->
                        val color = AdminColorScheme.brandColorFor(name)
                        Column(
                            Modifier.clickable(enabled = canManage) { draftTheme = name }.padding(3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Box(
                                Modifier.size(40.dp).clip(CircleShape).background(color)
                                    .border(if (draftTheme == name) 2.dp else 0.dp, AdminColorScheme.primaryText, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (draftTheme == name) {
                                    Icon(AdminIcons.Check, null, Modifier.size(16.dp), Color.White)
                                }
                            }
                            Text(name.replace("RushIt ", ""), fontSize = 10.sp, color = if (draftTheme == name) color else AdminColorScheme.secondaryText)
                        }
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Accent color", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    accentNames.forEach { name ->
                        AdminFilterChip(draftAccent == name, { if (canManage) draftAccent = name }, name)
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Background tone", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 8.dp)) {
                    listOf("Default", "Gradient", "Warm").forEach {
                        AdminFilterChip(draftBackground == it, { if (canManage) draftBackground = it }, it)
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Card radius · ${draftRadius.toInt()}dp", fontWeight = FontWeight.Bold)
                AdminSlider(draftRadius, { if (canManage) draftRadius = it }, 8f..28f)
                Text("Card style", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Elevated", "Soft", "Glass").forEach {
                        AdminFilterChip(draftCardStyle == it, { if (canManage) draftCardStyle = it }, it)
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Buttons & type", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Filled", "Soft", "Pill").forEach {
                        AdminFilterChip(draftButtonStyle == it, { if (canManage) draftButtonStyle = it }, it)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Ink", "Soft").forEach {
                        AdminFilterChip(draftTextTone == it, { if (canManage) draftTextTone = it }, if (it == "Ink") "Charcoal text" else "Warm muted text")
                    }
                }
                Text("Typography scale · ${"%.2f".format(draftTypeScale)}×", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                AdminSlider(draftTypeScale, { if (canManage) draftTypeScale = it }, 0.88f..1.15f)
            }
        }
        item {
            AdminSurfaceCard {
                Text("Live preview", fontWeight = FontWeight.Bold)
                val selectedColor = AdminColorScheme.brandColorFor(draftTheme)
                val accent = AdminColorScheme.accentColorFor(draftAccent)
                Surface(
                    shape = RoundedCornerShape(draftRadius.dp),
                    color = when (draftBackground) {
                        "Gradient" -> AdminColorScheme.brandSoftFor(draftTheme)
                        "Warm" -> AdminColorScheme.surfaceSecondary
                        else -> AdminColorScheme.background
                    },
                ) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("RushIt Home", color = if (draftTextTone == "Soft") AdminColorScheme.secondaryText else AdminColorScheme.primaryText, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Fresh picks · local preview", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                        Surface(
                            shape = if (draftButtonStyle == "Pill") CircleShape else RoundedCornerShape(12.dp),
                            color = if (draftButtonStyle == "Soft") selectedColor.copy(alpha = .13f) else selectedColor,
                        ) {
                            Text(
                                "Browse products",
                                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                color = if (draftButtonStyle == "Soft") selectedColor else Color.White,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Text("Accent chip", Modifier.clip(CircleShape).background(accent.copy(alpha = 0.18f)).padding(horizontal = 10.dp, vertical = 4.dp), color = accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        if (canManage) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton("Cancel", Modifier.weight(1f), enabled = hasChanges, onClick = ::resetDraft)
                    AdminSecondaryButton("Reset defaults", Modifier.weight(1f), onClick = {
                        draftTheme = "RushIt Coral"
                        draftAccent = "Amber"
                        draftBackground = "Default"
                        draftRadius = 16f
                        draftButtonStyle = "Filled"
                        draftCardStyle = "Elevated"
                        draftTextTone = "Ink"
                        draftTypeScale = 1f
                    })
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton("Save & preview", Modifier.weight(1f), onClick = {
                        if (hasChanges) onSave(draft)
                        onPreview()
                    })
                    PrimaryButton("Save theme", Modifier.weight(1f), enabled = hasChanges) { onSave(draft) }
                }
            }
        }
    }
    }
}

@Composable
fun MoreScreen(onOpen: (String) -> Unit, currentRole: String) {
    val permissions = LocalAdminPermissions.current
    val destinations = remember(permissions) { moreDestinations() }
    val window = LocalAdminWindow.current
    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader("More", "Content, customers, roles & local tools") }
        item {
            AdminSurfaceCard {
                Text("Signed in as", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                Text(currentRole, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Client-side role preview · not server authorization", color = AdminColorScheme.secondaryText, fontSize = 11.5.sp)
            }
        }
        items(
            destinations.chunked(window.listColumns.coerceAtLeast(1)),
            key = { row -> row.joinToString("-") { it.route } },
        ) { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { dest ->
                    val allowed = dest.required(permissions)
                    DataRow(
                        title = dest.label,
                        detail = if (allowed) dest.detail else "Restricted for $currentRole",
                        onClick = { onOpen(if (allowed) dest.route else "roles") },
                        icon = dest.icon,
                        trailingBadge = if (allowed) null else "Restricted",
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(window.listColumns - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun DesignerScreen(
    sections: MutableList<String>,
    onEdit: (Int) -> Unit,
    onSave: (String) -> Unit,
    onDirtyChanged: (Boolean) -> Unit,
    onLeave: () -> Unit,
    onPreview: () -> Unit,
    onOpenRoles: () -> Unit,
) {
    val permissions = LocalAdminPermissions.current
    val canManage = permissions.canManageContent
    val brand = LocalAdminThemeConfig.current.activePrimary
    val baseline = remember { mutableStateListOf<String>().apply { addAll(sections) } }
    var dirty by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Pair<Int, String>?>(null) }
    androidx.activity.compose.BackHandler(enabled = dirty) { onLeave() }

    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item { ScreenHeader("Home Designer", "Configure a local customer Home preview", backAction = onLeave) }
        if (!canManage) item { RoleAccessBanner(permissions.roleName, "Manage Home Content", onOpenRoles) }
        item { DemoNotice() }
        if (sections.isEmpty()) {
            item { EmptyState("No Home sections", "Add a supported section to start composing your preview.", if (canManage) "Add section" else null) { showAdd = true } }
        }
        itemsIndexed(sections.toList(), key = { index, row -> "$index:${row.hashCode()}" }) { index, row ->
            val config = parseHomeSection(row)
            AdminSurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(AdminIcons.Layout, null, Modifier.size(20.dp), brand)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(config.title.ifBlank { "Untitled section" }, fontWeight = FontWeight.Bold)
                        Text("${config.type} · ${config.subtitle.ifBlank { config.layout }}", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                    }
                    AdminSwitch(config.visibility == "Visible", enabled = canManage, onCheckedChange = { checked ->
                        if (index in sections.indices) {
                            sections[index] = config.copy(visibility = if (checked) "Visible" else "Hidden").encode()
                            dirty = true
                            onDirtyChanged(true)
                        }
                    })
                }
                if (canManage) {
                    Row {
                        TextButton(onClick = { onEdit(index) }) { Text("Edit", color = brand, fontWeight = FontWeight.Bold) }
                        TextButton(onClick = {
                            if (index > 0 && index in sections.indices) {
                                val moved = sections.removeAt(index)
                                sections.add(index - 1, moved)
                                dirty = true
                                onDirtyChanged(true)
                            }
                        }) {
                            Icon(AdminIcons.ArrowUp, contentDescription = "Move section up", tint = AdminColorScheme.primaryText, modifier = Modifier.size(18.dp))
                        }
                        TextButton(onClick = {
                            if (index in 0 until sections.lastIndex) {
                                val moved = sections.removeAt(index)
                                sections.add(index + 1, moved)
                                dirty = true
                                onDirtyChanged(true)
                            }
                        }) {
                            Icon(AdminIcons.ArrowDown, contentDescription = "Move section down", tint = AdminColorScheme.primaryText, modifier = Modifier.size(18.dp))
                        }
                        TextButton(onClick = { deleteTarget = index to config.title }) { Text("Delete", color = AdminColorScheme.error) }
                    }
                }
            }
        }
        if (canManage) {
            item { AdminSecondaryButton("+ Add Section", Modifier.fillMaxWidth()) { showAdd = true } }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminSecondaryButton("Preview", Modifier.weight(1f), onClick = onPreview)
                    PrimaryButton("Save Draft", Modifier.weight(1f)) {
                        onSave("Draft saved on this device")
                        baseline.clear()
                        baseline.addAll(sections)
                        dirty = false
                        onDirtyChanged(false)
                    }
                }
            }
            item {
                TextButton(onClick = { confirmDiscard = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Discard unsaved changes", color = AdminColorScheme.secondaryText)
                }
            }
        }
    }
    if (showAdd) {
        val choices = listOf("Category Icons", "Product Grid", "Bestsellers", "Recommended", "Promotional Banner", "Festival Collection")
        AdminDialog("Add a Home section", confirmLabel = "Close", onConfirm = { showAdd = false }, onDismiss = { showAdd = false }) {
            choices.forEach { type ->
                TextButton(
                    onClick = {
                        sections.add("$type|$type|Visible")
                        dirty = true
                        onDirtyChanged(true)
                        showAdd = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(type, modifier = Modifier.fillMaxWidth(), color = AdminColorScheme.primaryText) }
            }
        }
    }
    if (confirmDiscard) {
        AdminDialog(
            title = "Discard Home changes?",
            message = "Unsaved edits will be replaced with the last saved local draft.",
            confirmLabel = "Discard",
            isDestructive = true,
            onConfirm = {
                sections.clear(); sections.addAll(baseline); dirty = false; onDirtyChanged(false); confirmDiscard = false
            },
            onDismiss = { confirmDiscard = false },
        )
    }
    deleteTarget?.let { (index, title) ->
        AdminDialog(
            title = "Remove Home section?",
            message = "Remove $title from the local draft? Save the draft to keep this change.",
            confirmLabel = "Remove",
            isDestructive = true,
            onConfirm = {
                if (index in sections.indices) sections.removeAt(index)
                dirty = true
                onDirtyChanged(true)
                deleteTarget = null
            },
            onDismiss = { deleteTarget = null },
        )
    }
}

@Composable
fun SectionEditorScreen(section: String, onSave: (String) -> Unit, onCancel: () -> Unit) {
    val initial = remember(section) { parseHomeSection(section) }
    var title by rememberSaveable(section) { mutableStateOf(initial.title) }
    var type by rememberSaveable(section) { mutableStateOf(initial.type) }
    var subtitle by rememberSaveable(section) { mutableStateOf(initial.subtitle) }
    var layout by rememberSaveable(section) { mutableStateOf(initial.layout) }
    var surface by rememberSaveable(section) { mutableStateOf(initial.surface) }
    var animation by rememberSaveable(section) { mutableStateOf(initial.motion) }
    var visible by rememberSaveable(section) { mutableStateOf(initial.visibility == "Visible") }
    var error by rememberSaveable(section) { mutableStateOf("") }
    var confirmDiscard by remember { mutableStateOf(false) }
    val changed = title != initial.title || type != initial.type || subtitle != initial.subtitle ||
        layout != initial.layout || surface != initial.surface || animation != initial.motion ||
        visible != (initial.visibility == "Visible")
    val cancel = { if (changed) confirmDiscard = true else onCancel() }
    androidx.activity.compose.BackHandler(enabled = changed) { confirmDiscard = true }
    val usesCollectionLayout = type in setOf("Featured products", "Product grid", "Categories")

    LazyColumn(
        Modifier.fillMaxSize().adminPageInsets(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Edit Section", type, backAction = cancel) }
        item {
            AdminSurfaceCard {
                Text("Section content", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                AdminField("Section title", title, { title = it.take(48) })
                Text("Section type", color = AdminColorScheme.secondaryText, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(vertical = 6.dp)) {
                    items(homeSectionTypes, key = { it }) { option ->
                        AdminFilterChip(type == option, { type = option }, option)
                    }
                }
                AdminField(if (type in setOf("Hero / banner", "Promotion", "Festival campaigns", "Offers", "Custom promotion")) "Subtitle or content note" else "Supporting label", subtitle, { subtitle = it.take(100) })
                ToggleRow("Visible in local preview", visible) { visible = it }
            }
        }
        if (usesCollectionLayout) {
            item {
                AdminSurfaceCard {
                    Text("Layout", fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Carousel", "Grid").forEach { AdminFilterChip(layout == it, { layout = it }, it) }
                    }
                    Text("Card surface", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Soft", "Glass", "Elevated").forEach { AdminFilterChip(surface == it, { surface = it }, it) }
                    }
                }
            }
        }
        item {
            AdminSurfaceCard {
                Text("Motion preset", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("None", "Fade", "Slide").forEach { AdminFilterChip(animation == it, { animation = it }, it) }
                }
                Text("Short, lightweight transitions run in the local preview only.", color = AdminColorScheme.secondaryText, fontSize = 12.sp)
            }
        }
        item {
            if (error.isNotBlank()) Text(error, color = AdminColorScheme.error)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminSecondaryButton("Cancel", Modifier.weight(1f), onClick = cancel)
                PrimaryButton("Save Changes", Modifier.weight(1f)) {
                    error = if (title.isBlank()) "Add a title for this section." else ""
                    if (error.isBlank()) onSave(HomeSectionConfig(title.trim(), type, subtitle.trim(), layout, surface, animation, if (visible) "Visible" else "Hidden").encode())
                }
            }
        }
    }
    if (confirmDiscard) {
        AdminDialog(
            title = "Discard section edits?",
            message = "Your unsaved changes to this section will be lost.",
            confirmLabel = "Discard",
            isDestructive = true,
            onConfirm = { confirmDiscard = false; onCancel() },
            onDismiss = { confirmDiscard = false },
        )
    }
}

@Composable
fun PreviewScreen(
    sections: List<String>,
    products: List<String>,
    categories: List<String>,
    campaigns: List<String>,
    offers: List<String>,
    publishedSections: List<String>,
    themeConfig: AdminThemeConfig,
    publishedTheme: String,
    publishedBackground: String,
    publishedRadius: Float,
    publishedButtonStyle: String,
    publishedTextTone: String,
    storeName: String,
    maintenancePreview: Boolean,
    onBack: () -> Unit,
    onPublish: () -> Unit,
) {
    val visibleSections = sections.filter { parseHomeSection(it).visibility == "Visible" }
    val visibleCategories = categories.filter { it.endsWith("|Visible") }.map { it.substringBefore('|') }
    val previewProducts = products.filter {
        val rec = visibleProductRecord(it)
        rec.contains("Visible", true) && !rec.contains("Out of stock", true)
    }
    val hasDraftChanges = sections != publishedSections ||
        themeConfig.themeName != publishedTheme ||
        themeConfig.backgroundStyle != publishedBackground ||
        themeConfig.sectionRadius != publishedRadius ||
        themeConfig.buttonStyle != publishedButtonStyle ||
        themeConfig.previewTextTone != publishedTextTone
    val previewTextColor = if (themeConfig.previewTextTone == "Soft") AdminColorScheme.secondaryText else AdminColorScheme.primaryText
    var confirmPublish by remember { mutableStateOf(false) }
    var validationMessage by remember { mutableStateOf("") }
    val previewColor = themeConfig.activePrimary
    val previewBackground = if (themeConfig.backgroundStyle == "Gradient") {
        Modifier.background(Brush.verticalGradient(listOf(themeConfig.activePrimarySoft, AdminColorScheme.background)))
    } else {
        Modifier.background(AdminColorScheme.background)
    }

    LazyColumn(
        Modifier.fillMaxSize().then(previewBackground).adminPageInsets(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenHeader("Home Preview", "Local preview · not published to the User app") }
        item { DemoNotice() }
        if (maintenancePreview) {
            item {
                Text(
                    "Maintenance preview · this local banner does not affect the User app or customer traffic.",
                    Modifier.fillMaxWidth().background(AdminColorScheme.warningSoft, RoundedCornerShape(12.dp)).padding(12.dp),
                    color = AdminColorScheme.primaryText,
                    fontSize = 12.sp,
                )
            }
        }
        item { StatusChip(if (hasDraftChanges) "Draft · local only" else "Matches saved snapshot") }
        item {
            AdminSurfaceCard {
                Text(storeName, color = previewColor, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
                Text("Good morning · Patna", color = previewTextColor)
                Spacer(Modifier.height(12.dp))
                Text("Search groceries and products…", Modifier.fillMaxWidth().background(Color(0xFFF6F1EA), RoundedCornerShape(14.dp)).padding(14.dp), color = AdminColorScheme.secondaryText)
            }
        }
        if (visibleSections.isEmpty()) item { EmptyState("No visible sections", "Enable or add a section in Home Designer to see it here.") }
        itemsIndexed(visibleSections, key = { index, section -> "$index:${section.hashCode()}" }) { _, section ->
            val config = parseHomeSection(section)
            val cardSurface = when (config.surface) {
                "Glass" -> Color.White.copy(alpha = .82f)
                "Elevated" -> Color.White
                else -> Color(0xFFFAF7F1)
            }
            AnimatedVisibility(
                visible = true,
                enter = if (!themeConfig.animationsEnabled) EnterTransition.None else when (config.motion) {
                    "Slide" -> slideInVertically { it / 8 }
                    "Fade" -> fadeIn()
                    else -> EnterTransition.None
                },
            ) {
                when (config.type) {
                    "Hero / banner", "Promotion", "Festival campaigns", "Offers", "Custom promotion" -> Surface(shape = RoundedCornerShape(themeConfig.cardCornerRadius), color = previewColor) {
                        Column(Modifier.fillMaxWidth().padding(18.dp)) {
                            Text(config.type, color = Color.White.copy(alpha = .82f), fontSize = 12.sp)
                            Text(config.title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                            val contentLabel = when (config.type) {
                                "Festival campaigns" -> campaigns.firstOrNull()?.split('|')?.firstOrNull()?.let { "Festival preview · $it" } ?: "No local festivals configured"
                                "Offers" -> offers.firstOrNull()?.split('|')?.getOrNull(1)?.let { "Coupon preview · $it" } ?: "No local offers configured"
                                else -> config.subtitle
                            }
                            if (contentLabel.isNotBlank()) Text(contentLabel, color = Color.White.copy(alpha = .9f), fontSize = 12.sp)
                            Text("Local preview only · not published to the User app", color = Color.White.copy(alpha = .84f), fontSize = 11.sp)
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = if (themeConfig.buttonStyle == "Soft") Color.White.copy(alpha = .2f) else Color.White,
                                modifier = Modifier.padding(top = 10.dp),
                            ) {
                                Text(
                                    "Explore",
                                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    color = if (themeConfig.buttonStyle == "Soft") Color.White else previewColor,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                    "Categories" -> AdminSurfaceCard(containerColor = cardSurface, radius = themeConfig.cardCornerRadius) {
                        Text(config.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))
                        if (visibleCategories.isEmpty()) EmptyState("No visible categories", "Enable a category in Category Management to preview it.")
                        else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            visibleCategories.take(4).forEach { category ->
                                Text(
                                    "${categoryGlyph(category)}\n$category",
                                    Modifier.weight(1f).background(Color(0xFFFFF4EE), RoundedCornerShape(12.dp)).padding(8.dp),
                                    color = previewTextColor,
                                    fontSize = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    else -> AdminSurfaceCard(containerColor = cardSurface, radius = themeConfig.cardCornerRadius) {
                        if (config.title.contains("best", true)) {
                            EmptyState("Sales ranking unavailable", "No order line quantities are stored in this local demo, so best sellers cannot be ranked.")
                        } else if (previewProducts.isEmpty()) {
                            EmptyState("No visible products", "Add or show products in Product Management to preview this section.")
                        } else {
                            Text(config.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(config.subtitle.ifBlank { "Local demo catalog · ${config.layout}" }, color = AdminColorScheme.secondaryText, fontSize = 12.sp)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                previewProducts.take(3).forEach { stored ->
                                    val record = visibleProductRecord(stored)
                                    val fields = record.split(" · ")
                                    Column(Modifier.weight(1f).background(Color.White, RoundedCornerShape(12.dp)).padding(10.dp)) {
                                        Box(Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFFFF4EE)), contentAlignment = Alignment.Center) {
                                            Icon(AdminIcons.Package, null, Modifier.size(20.dp), previewColor)
                                        }
                                        Text(fields.firstOrNull().orEmpty(), color = previewTextColor, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(fields.getOrNull(1).orEmpty(), color = previewTextColor, fontSize = 11.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            if (validationMessage.isNotBlank()) Text(validationMessage, color = AdminColorScheme.error, fontSize = 13.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AdminSecondaryButton("Return to editor", Modifier.weight(1f), onClick = onBack)
                PrimaryButton("Save local snapshot", Modifier.weight(1f), enabled = hasDraftChanges && visibleSections.isNotEmpty()) {
                    validationMessage = when {
                        visibleSections.isEmpty() -> "Enable at least one section before saving a snapshot."
                        visibleSections.any { it.substringBefore('|').isBlank() } -> "Every visible section needs a title."
                        else -> ""
                    }
                    if (validationMessage.isBlank()) confirmPublish = true
                }
            }
        }
    }
    if (confirmPublish) {
        AdminDialog(
            title = "Save this preview snapshot?",
            message = "This stores the current preview on this device only. It does not publish to the RushIt User app or any server.",
            confirmLabel = "Save snapshot",
            onConfirm = { confirmPublish = false; onPublish() },
            onDismiss = { confirmPublish = false },
        )
    }
}
