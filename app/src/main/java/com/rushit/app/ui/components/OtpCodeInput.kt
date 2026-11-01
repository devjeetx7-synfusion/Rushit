package com.rushit.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.data.auth.phone.VerificationCode
import com.rushit.app.ui.theme.Border
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.DeepRed
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Peach
import com.rushit.app.ui.theme.SoftRed

/**
 * A six-box one-time-code field built on a single text input.
 *
 * One text field, not six: a single field is what makes pasting a whole code, backspace across
 * boxes, Android's SMS-code suggestion and accessibility services work the way they do in every
 * other messaging app. The boxes are decorations drawn behind an invisible input, so the digits stay
 * visually aligned and the active box is highlighted with the RushIt accent.
 *
 * @param value the raw digits typed so far; non-digits are filtered out and the length is capped
 * @param onValueChange receives the normalized digits, never the raw text
 * @param isError draws the rejection state — also announced through [stateDescription] so the error
 * is not communicated by colour alone
 * @param autoFocus requests focus on first composition, which is what raises the keyboard and lets
 * the IME offer the incoming SMS code
 */
@Composable
fun OtpCodeInput(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    length: Int = VerificationCode.LENGTH,
    enabled: Boolean = true,
    isError: Boolean = false,
    autoFocus: Boolean = true,
) {
    val digits = VerificationCode.normalize(value).take(length)
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val activeIndex = digits.length.coerceIn(0, (length - 1).coerceAtLeast(0))

    // Keyed on `enabled` as well as the first composition: a rejected code disables the field while
    // Firebase checks it, so restoring focus when it is enabled again puts the cursor back where the
    // person was typing instead of leaving them with a closed keyboard.
    LaunchedEffect(autoFocus, enabled) {
        if (autoFocus && enabled) {
            // A failure here only means the field was not attached yet; the person can still tap it.
            runCatching { focusRequester.requestFocus() }
        }
    }

    Box(modifier.fillMaxWidth()) {
        // Decoration only: the field below owns the value, the focus and the accessibility node.
        Row(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { },
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(length) { index ->
                val digit = digits.getOrNull(index)
                val isActive = enabled && index == activeIndex
                val borderColor by animateColorAsState(
                    when {
                        isError -> DeepRed
                        isActive -> Coral
                        digit != null -> Coral.copy(alpha = .45f)
                        else -> Border
                    },
                    label = "otp box border",
                )
                val container by animateColorAsState(
                    when {
                        isError -> SoftRed
                        isActive -> Peach.copy(alpha = .55f)
                        else -> CardWhite
                    },
                    label = "otp box fill",
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = container,
                    border = BorderStroke(if (isActive || isError) 2.dp else 1.dp, borderColor),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 56.dp),
                ) {
                    Box(Modifier.padding(vertical = 12.dp, horizontal = 2.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = digit?.toString().orEmpty(),
                            color = if (isError) DeepRed else Ink,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            maxLines = 1,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        // The real input. Its text and cursor are transparent so only the boxes are visible, while
        // the field still reports (and edits) the actual value for TalkBack and IME autofill.
        BasicTextField(
            value = digits,
            onValueChange = { onValueChange(VerificationCode.normalize(it).take(length)) },
            enabled = enabled,
            singleLine = true,
            textStyle = TextStyle(color = Color.Transparent, fontSize = 1.sp),
            cursorBrush = SolidColor(Color.Transparent),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxSize()
                .focusRequester(focusRequester)
                .semantics {
                    contentDescription = "$length-digit verification code"
                    stateDescription = if (isError) {
                        "Rejected. ${digits.length} of $length digits entered"
                    } else {
                        "${digits.length} of $length digits entered"
                    }
                },
        )
    }
}
