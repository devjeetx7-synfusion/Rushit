package com.rushit.admin

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController

val LocalAdminNavigation = staticCompositionLocalOf<NavHostController?> { null }

data class AdminNavTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

val adminBottomTabs = listOf(
    AdminNavTab("home", "Home", AdminIcons.Home),
    AdminNavTab("catalog", "Catalog", AdminIcons.Catalog),
    AdminNavTab("orders", "Orders", AdminIcons.Orders),
    AdminNavTab("themes", "Themes", AdminIcons.Themes),
    AdminNavTab("more", "More", AdminIcons.More),
)

/**
 * Custom floating rounded bottom navigation bar for RushIt Admin V2.
 * Replaces the generic Material NavigationBar with a sleek floating dock,
 * custom coral pill indicator, vector icons, and proper system bar inset handling.
 */
@Composable
fun FloatingAdminBottomBar(
    selectedRoute: String,
    onSelectTab: (String) -> Unit,
    modifier: Modifier = Modifier,
    badgeCounts: Map<String, Int> = emptyMap(),
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brandColor = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft
    val animate = themeConfig.animationsEnabled

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        AdminColorScheme.background.copy(alpha = 0.88f),
                        AdminColorScheme.background,
                    )
                )
            )
            .navigationBarsPadding()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 10.dp,
                    shape = AdminShapes.bottomBar,
                    ambientColor = Color(0x22292524),
                    spotColor = Color(0x26E7473C),
                ),
            shape = AdminShapes.bottomBar,
            color = AdminColorScheme.surface.copy(alpha = 0.98f),
             tonalElevation = 0.dp,
            border = BorderStroke(1.dp, AdminColorScheme.divider),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                adminBottomTabs.forEach { tab ->
                    val selected = selectedRoute == tab.route
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.06f else 1.0f,
                        animationSpec = if (animate) {
                            spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMediumLow,
                            )
                        } else {
                            snap()
                        },
                        label = "nav_scale_${tab.route}",
                    )
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = if (animate) tween(200) else snap(),
                        label = "nav_pill_${tab.route}",
                    )
                    val contentColor by animateColorAsState(
                        targetValue = if (selected) brandColor else AdminColorScheme.secondaryText,
                        animationSpec = if (animate) tween(200) else snap(),
                        label = "nav_color_${tab.route}",
                    )
                    val badge = badgeCounts[tab.route] ?: 0

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .selectable(
                                selected = selected,
                                role = Role.Tab,
                                onClick = { onSelectTab(tab.route) },
                            )
                            .semantics { contentDescription = "${tab.label} tab" }
                            .padding(vertical = 5.dp, horizontal = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .height(32.dp)
                                .width(52.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = pillAlpha
                                        scaleX = scale
                                        scaleY = scale
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(brandSoft)
                                    .border(
                                        width = 1.dp,
                                        color = brandColor.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(16.dp),
                                    ),
                            )
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = null,
                                tint = contentColor,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer {
                                        scaleX = scale
                                        scaleY = scale
                                    },
                            )
                            if (badge > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 2.dp, end = 6.dp)
                                        .size(15.dp)
                                        .clip(CircleShape)
                                        .background(brandColor),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = if (badge > 9) "9+" else badge.toString(),
                                        color = Color.White,
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(
                            text = tab.label,
                            color = contentColor,
                            fontSize = 11.sp,
                            lineHeight = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Custom RushIt Admin Brand Logo artwork rendered with Compose Canvas.
 * Replaces generic shopping cart emojis with a crisp, branded vector badge.
 */
@Composable
fun RushItBrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 76.dp,
    inverted: Boolean = false,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val primary = if (inverted) Color.White else themeConfig.activePrimary
    val containerBrush = if (inverted) {
        Brush.linearGradient(listOf(Color.White.copy(alpha = 0.22f), Color.White.copy(alpha = 0.10f)))
    } else {
        Brush.linearGradient(listOf(themeConfig.activePrimary, AdminColorScheme.primaryDark))
    }

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (inverted) 0.dp else 10.dp,
                shape = RoundedCornerShape(size * 0.30f),
                ambientColor = themeConfig.activePrimary.copy(alpha = 0.35f),
                spotColor = themeConfig.activePrimary.copy(alpha = 0.35f),
            )
            .clip(RoundedCornerShape(size * 0.30f))
            .background(containerBrush)
            .border(
                width = 1.5.dp,
                color = Color.White.copy(alpha = if (inverted) 0.35f else 0.22f),
                shape = RoundedCornerShape(size * 0.30f),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize(0.62f)
                .semantics { contentDescription = "RushIt Admin brand emblem" },
        ) {
            val w = this.size.width
            val h = this.size.height
            val strokeColor = if (inverted) primary else Color.White

            // Fast speed lines on the left
            drawLine(
                color = AdminColorScheme.accent,
                start = Offset(w * 0.04f, h * 0.36f),
                end = Offset(w * 0.22f, h * 0.36f),
                strokeWidth = w * 0.085f,
                cap = StrokeCap.Round,
            )
            drawLine(
                color = AdminColorScheme.accent.copy(alpha = 0.85f),
                start = Offset(w * 0.08f, h * 0.54f),
                end = Offset(w * 0.24f, h * 0.54f),
                strokeWidth = w * 0.075f,
                cap = StrokeCap.Round,
            )

            // Express Cart Path
            val cartPath = Path().apply {
                moveTo(w * 0.14f, h * 0.18f)
                lineTo(w * 0.28f, h * 0.18f)
                lineTo(w * 0.38f, h * 0.66f)
                lineTo(w * 0.82f, h * 0.66f)
                lineTo(w * 0.93f, h * 0.30f)
                lineTo(w * 0.33f, h * 0.30f)
            }
            drawPath(
                path = cartPath,
                color = strokeColor,
                style = Stroke(width = w * 0.095f, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )

            // Wheels
            drawCircle(
                color = strokeColor,
                radius = w * 0.075f,
                center = Offset(w * 0.44f, h * 0.84f),
            )
            drawCircle(
                color = strokeColor,
                radius = w * 0.075f,
                center = Offset(w * 0.76f, h * 0.84f),
            )

            // Fresh leaf badge at top-right of basket
            drawRoundRect(
                color = AdminColorScheme.accent,
                topLeft = Offset(w * 0.48f, h * 0.42f),
                size = Size(w * 0.28f, h * 0.11f),
                cornerRadius = CornerRadius(w * 0.05f, w * 0.05f),
            )
        }
    }
}

@Composable
fun AdminSurfaceCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    radius: Dp? = null,
    contentPadding: Dp = AdminSpacing.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val resolvedRadius = radius ?: themeConfig.cardCornerRadius
    val resolvedColor = containerColor ?: when (themeConfig.cardStyle) {
        "Glass" -> AdminColorScheme.surface.copy(alpha = 0.88f)
        "Soft" -> AdminColorScheme.surfaceSecondary
        else -> AdminColorScheme.surface
    }
    val resolvedElevation = when (themeConfig.cardStyle) {
        "Elevated" -> 2.dp
        "Glass" -> 1.dp
        else -> 0.dp
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(resolvedRadius),
        color = resolvedColor,
        tonalElevation = 0.dp,
        shadowElevation = resolvedElevation,
        border = BorderStroke(1.dp, AdminColorScheme.divider),
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
fun AdminCard(
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    radius: Dp? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    AdminSurfaceCard(
        modifier = modifier,
        containerColor = containerColor,
        radius = radius,
        content = content,
    )
}

@Composable
fun PrimaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val isSoft = themeConfig.buttonStyle == "Soft"
    val shape = when (themeConfig.buttonStyle) {
        "Pill" -> CircleShape
        else -> AdminShapes.button
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSoft) themeConfig.activePrimarySoft else brand,
            contentColor = if (isSoft) brand else Color.White,
            disabledContainerColor = AdminColorScheme.divider,
            disabledContentColor = AdminColorScheme.secondaryText,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (isSoft) 0.dp else 1.dp,
            pressedElevation = 0.dp,
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AdminSecondaryButton(
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val shape = if (themeConfig.buttonStyle == "Pill") CircleShape else AdminShapes.button

    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 48.dp),
        shape = shape,
        border = BorderStroke(1.dp, if (enabled) brand.copy(alpha = 0.35f) else AdminColorScheme.divider),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = AdminColorScheme.surface,
            contentColor = brand,
            disabledContentColor = AdminColorScheme.disabled,
        ),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(6.dp))
        }
        Text(
            text = label,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AdminField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    keyboard: KeyboardType = KeyboardType.Text,
    readOnly: Boolean = false,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    placeholder: String? = null,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it, color = AdminColorScheme.mutedText) } },
        singleLine = singleLine,
        readOnly = readOnly,
        visualTransformation = visualTransformation,
        trailingIcon = trailingIcon,
        leadingIcon = leadingIcon,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        shape = AdminShapes.input,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = brand,
            unfocusedBorderColor = AdminColorScheme.divider,
            disabledBorderColor = AdminColorScheme.divider,
            focusedLabelColor = brand,
            unfocusedLabelColor = AdminColorScheme.secondaryText,
            cursorColor = brand,
            focusedContainerColor = AdminColorScheme.surface,
            unfocusedContainerColor = AdminColorScheme.inputBackground,
            focusedTextColor = AdminColorScheme.primaryText,
            unfocusedTextColor = AdminColorScheme.primaryText,
        ),
    )
}

@Composable
fun AdminSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = "Search…",
    modifier: Modifier = Modifier,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AdminColorScheme.secondaryText, fontSize = 13.5.sp) },
        leadingIcon = {
            Icon(
                imageVector = AdminIcons.Search,
                contentDescription = "Search",
                tint = AdminColorScheme.secondaryText,
                modifier = Modifier.size(19.dp),
            )
        },
        trailingIcon = if (value.isNotEmpty()) {
            {
                TextButton(onClick = { onValueChange("") }) {
                    Text("Clear", color = brand, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = brand,
            unfocusedBorderColor = AdminColorScheme.divider,
            cursorColor = brand,
            focusedContainerColor = AdminColorScheme.surface,
            unfocusedContainerColor = AdminColorScheme.surface,
            focusedTextColor = AdminColorScheme.primaryText,
            unfocusedTextColor = AdminColorScheme.primaryText,
        ),
    )
}

@Composable
fun AdminSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = brand,
            checkedBorderColor = brand,
            uncheckedThumbColor = Color.White,
            uncheckedTrackColor = Color(0xFFE2DCD4),
            uncheckedBorderColor = Color(0xFFD5CEC5),
            disabledCheckedTrackColor = brand.copy(alpha = 0.4f),
            disabledUncheckedTrackColor = AdminColorScheme.divider,
        ),
    )
}

@Composable
fun ToggleRow(
    label: String,
    value: Boolean,
    subtitle: String? = null,
    enabled: Boolean = true,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onChange(!value) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = label,
                color = AdminColorScheme.primaryText,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    color = AdminColorScheme.secondaryText,
                    fontSize = 11.5.sp,
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        AdminSwitch(checked = value, onCheckedChange = onChange, enabled = enabled)
    }
}

@Composable
fun AdminSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    modifier: Modifier = Modifier,
) {
    val themeConfig = LocalAdminThemeConfig.current
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = themeConfig.activePrimary,
            activeTrackColor = themeConfig.activePrimary,
            inactiveTrackColor = themeConfig.activePrimarySoft,
        ),
    )
}

@Composable
fun AdminCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    Checkbox(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = CheckboxDefaults.colors(
            checkedColor = brand,
            uncheckedColor = AdminColorScheme.secondaryText,
            checkmarkColor = Color.White,
        ),
    )
}

@Composable
fun AdminRadioButton(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    RadioButton(
        selected = selected,
        onClick = onClick,
        modifier = modifier,
        colors = RadioButtonDefaults.colors(
            selectedColor = brand,
            unselectedColor = AdminColorScheme.secondaryText,
        ),
    )
}

/**
 * Branded pill filter chip replacing the default Material 3 FilterChip look.
 */
@Composable
fun AdminFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    countBadge: Int? = null,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val bgColor = if (selected) brand else AdminColorScheme.surface
    val contentColor = if (selected) Color.White else AdminColorScheme.secondaryText
    val borderColor = if (selected) brand else AdminColorScheme.divider

    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Tab, onClick = onClick),
        shape = CircleShape,
        color = bgColor,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = label,
                color = contentColor,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1,
            )
            if (countBadge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) Color.White.copy(alpha = 0.22f)
                            else AdminColorScheme.surfaceSecondary
                        )
                        .padding(horizontal = 6.dp, vertical = 1.dp),
                ) {
                    Text(
                        text = countBadge.toString(),
                        color = if (selected) Color.White else AdminColorScheme.primaryText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Semantic status pill chip with consistent contrast and color coding.
 */
@Composable
fun StatusChip(
    status: String,
    modifier: Modifier = Modifier,
) {
    val normalized = status.trim().lowercase()
    val (fg, bg) = when {
        normalized.contains("delivered") ||
            normalized.contains("active") ||
            normalized.contains("in stock") ||
            normalized.contains("healthy") ||
            normalized.contains("enabled") ||
            normalized.contains("visible") ||
            normalized.contains("matches") -> AdminColorScheme.success to AdminColorScheme.successSoft

        normalized.contains("cancelled") ||
            normalized.contains("out of stock") ||
            normalized.contains("error") ||
            normalized.contains("denied") ||
            normalized.contains("restricted") -> AdminColorScheme.error to AdminColorScheme.errorSoft

        normalized.contains("scheduled") ||
            normalized.contains("upcoming") ||
            normalized.contains("on the way") ||
            normalized.contains("almost there") ||
            normalized.contains("picked up") ||
            normalized.contains("super admin") -> AdminColorScheme.info to AdminColorScheme.infoSoft

        normalized.contains("preparing") ||
            normalized.contains("confirmed") ||
            normalized.contains("pending") ||
            normalized.contains("rider ready") ||
            normalized.contains("low stock") ||
            normalized.contains("draft") -> AdminColorScheme.warning to AdminColorScheme.warningSoft

        else -> AdminColorScheme.secondaryText to Color(0xFFF3EFEA)
    }

    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, fg.copy(alpha = 0.22f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg),
            )
            Text(
                text = status,
                color = fg,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun ScreenHeader(
    title: String,
    subtitle: String = "",
    onAction: (() -> Unit)? = null,
    action: String = "+ Add",
    backAction: (() -> Unit)? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    val nav = LocalAdminNavigation.current
    val route = nav?.currentBackStackEntry?.destination?.route
    val showBack = backAction != null || (route != null && route !in setOf("home", "catalog", "orders", "themes", "more", "login", "splash"))
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBack) {
            Surface(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = { if (backAction != null) backAction() else nav?.popBackStack() },
                    )
                    .semantics { contentDescription = "Navigate back" },
                shape = RoundedCornerShape(13.dp),
                color = AdminColorScheme.surface,
                border = BorderStroke(1.dp, AdminColorScheme.divider),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = AdminIcons.Back,
                        contentDescription = "Back",
                        tint = AdminColorScheme.primaryText,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AdminColorScheme.primaryText,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminColorScheme.secondaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (trailingContent != null) {
            Spacer(Modifier.width(8.dp))
            trailingContent()
        } else if (onAction != null) {
            Spacer(Modifier.width(8.dp))
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(13.dp))
                    .clickable(role = Role.Button, onClick = onAction),
                shape = RoundedCornerShape(13.dp),
                color = brand,
            ) {
                Text(
                    text = action,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                )
            }
        }
    }
}

@Composable
fun AdminPageTitle(
    title: String,
    subtitle: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    ScreenHeader(title = title, subtitle = subtitle, trailingContent = trailing)
}

@Composable
fun SectionTitle(
    title: String,
    action: String = "",
    onAction: (() -> Unit)? = null,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = AdminColorScheme.primaryText,
        )
        if (action.isNotBlank()) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .then(if (onAction != null) Modifier.clickable(role = Role.Button, onClick = onAction) else Modifier)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = action,
                    color = if (onAction != null) brand else AdminColorScheme.secondaryText,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.Bold,
                )
                if (onAction != null) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        imageVector = AdminIcons.ChevronRight,
                        contentDescription = null,
                        tint = brand,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DataRow(
    glyph: String,
    title: String,
    detail: String,
    onClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    trailingBadge: String? = null,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(themeConfig.cardCornerRadius))
                        .clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(themeConfig.cardCornerRadius),
        color = AdminColorScheme.surface,
        border = BorderStroke(1.dp, AdminColorScheme.divider),
        shadowElevation = 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = brand,
                        modifier = Modifier.size(20.dp),
                    )
                } else {
                    Text(
                        text = glyph,
                        color = brand,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    color = AdminColorScheme.primaryText,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        color = AdminColorScheme.secondaryText,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (!trailingBadge.isNullOrBlank()) {
                Spacer(Modifier.width(8.dp))
                StatusChip(trailingBadge)
            }
            if (onClick != null) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = AdminIcons.ChevronRight,
                    contentDescription = null,
                    tint = AdminColorScheme.secondaryText,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val themeConfig = LocalAdminThemeConfig.current
    val brand = themeConfig.activePrimary
    val brandSoft = themeConfig.activePrimarySoft

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(themeConfig.cardCornerRadius),
        color = AdminColorScheme.surface,
        border = BorderStroke(1.dp, AdminColorScheme.divider),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(brandSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AdminIcons.Search,
                    contentDescription = null,
                    tint = brand,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = title,
                color = AdminColorScheme.primaryText,
                fontWeight = FontWeight.Bold,
                fontSize = 15.5.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = message,
                color = AdminColorScheme.secondaryText,
                fontSize = 12.5.sp,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null && onAction != null) {
                Spacer(Modifier.height(4.dp))
                PrimaryButton(
                    label = actionLabel,
                    onClick = onAction,
                )
            }
        }
    }
}

@Composable
fun DemoNotice(
    message: String = "LOCAL DEMO · Changes stay inside RushIt Admin on this device and do not modify the live customer app or production backend.",
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = AdminColorScheme.warningSoft,
        border = BorderStroke(1.dp, AdminColorScheme.warning.copy(alpha = 0.28f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(AdminColorScheme.warning.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = AdminIcons.AlertTriangle,
                    contentDescription = null,
                    tint = AdminColorScheme.warning,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = message,
                color = AdminColorScheme.primaryText,
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
fun AdminDialog(
    title: String,
    message: String? = null,
    confirmLabel: String,
    dismissLabel: String = "Cancel",
    isDestructive: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val brand = LocalAdminThemeConfig.current.activePrimary
    val actionColor = if (isDestructive) AdminColorScheme.error else brand

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = AdminShapes.dialog,
        containerColor = AdminColorScheme.surface,
        tonalElevation = 0.dp,
        titleContentColor = AdminColorScheme.primaryText,
        textContentColor = AdminColorScheme.secondaryText,
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = AdminColorScheme.primaryText,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (!message.isNullOrBlank()) {
                    Text(
                        text = message,
                        color = AdminColorScheme.secondaryText,
                        fontSize = 13.5.sp,
                        lineHeight = 19.sp,
                    )
                }
                if (content != null) {
                    content()
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = actionColor,
                    contentColor = Color.White,
                ),
            ) {
                Text(confirmLabel, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissLabel, color = AdminColorScheme.secondaryText, fontWeight = FontWeight.SemiBold)
            }
        },
    )
}
