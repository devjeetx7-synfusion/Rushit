package com.rushit.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.rushit.app.data.model.Order
import com.rushit.app.data.model.OrderStatus
import com.rushit.app.ui.screens.order.OrderJourneyScreen
import com.rushit.app.ui.theme.LocalReducedMotion
import com.rushit.app.ui.theme.RushItTheme
import com.rushit.app.ui.viewmodel.OrderUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Focused tracking-screen UI tests. Reduced motion is enabled so assertions do not wait on
 * decorative infinite animations; the motion geometry itself is covered by TravelMotionTest.
 */
class OrderJourneyScreenTest {
    @get:Rule val rule = createComposeRule()

    private fun order(status: OrderStatus, isDemo: Boolean = true) = Order(
        id = "RI172805456",
        status = status,
        subtotalPaise = 11_200,
        totalPaise = 11_700,
        createdAt = "2026-10-30T10:15:00+0530",
        isDemo = isDemo,
    )

    private fun show(
        initial: OrderUiState,
        width: Int = 400,
        onBack: () -> Unit = {},
        onAdvance: () -> Unit = {},
        onCancel: () -> Unit = {},
        onViewOrder: () -> Unit = {},
        onHome: () -> Unit = {},
    ) {
        var state by mutableStateOf(initial)
        rule.setContent {
            RushItTheme {
                CompositionLocalProvider(LocalReducedMotion provides true) {
                    Box(Modifier.size(width.dp, 820.dp)) {
                        OrderJourneyScreen(
                            state = state,
                            onBack = onBack,
                            onAdvance = onAdvance,
                            onCancel = onCancel,
                            onViewOrder = onViewOrder,
                            onContinueShopping = onHome,
                            onCelebrationShown = {},
                            onRetry = {},
                        )
                    }
                }
            }
        }
    }

    @Test fun confirmedStageShowsOriginalActionsAndBackWorks() {
        var backs = 0
        show(OrderUiState(order = order(OrderStatus.CONFIRMED)), onBack = { backs++ })
        rule.onNodeWithText("Order confirmed!").assertIsDisplayed()
        rule.onNodeWithText("Preview next stage").performScrollTo().assertIsDisplayed()
        rule.onNodeWithText("Cancel order").performScrollTo().assertIsDisplayed()
        rule.onNodeWithContentDescription("Go back").performClick()
        assertEquals(1, backs)
    }

    @Test fun previewButtonInvokesCallbackOncePerTap() {
        var advances = 0
        show(OrderUiState(order = order(OrderStatus.PREPARING)), onAdvance = { advances++ })
        rule.onNodeWithText("Preview next stage").performScrollTo().performClick()
        rule.onNodeWithText("Preview next stage").performScrollTo().performClick()
        assertEquals(2, advances)
    }

    @Test fun cancelIsHiddenAfterPickup() {
        show(OrderUiState(order = order(OrderStatus.PICKED_UP)))
        rule.onNodeWithText("Order picked up!").assertIsDisplayed()
        rule.onNodeWithText("Cancel order").assertDoesNotExist()
    }

    @Test fun deliveredStageShowsCompletionActions() {
        var views = 0
        var homes = 0
        show(OrderUiState(order = order(OrderStatus.DELIVERED)), onViewOrder = { views++ }, onHome = { homes++ })
        rule.onNodeWithText("Delivered!").assertIsDisplayed()
        rule.onNodeWithText("View order details").performScrollTo().performClick()
        rule.onNodeWithText("Back to home").performScrollTo().performClick()
        assertEquals(1, views)
        assertEquals(1, homes)
    }

    @Test fun realOrderDoesNotOfferDemoPreview() {
        show(OrderUiState(order = order(OrderStatus.OUT_FOR_DELIVERY, isDemo = false)))
        rule.onNodeWithText("Preview next stage").assertDoesNotExist()
        rule.onNodeWithText("View order details").performScrollTo().assertIsDisplayed()
    }

    @Test fun smallScreenKeepsActionsReachable() {
        show(OrderUiState(order = order(OrderStatus.ARRIVING)), width = 320)
        rule.onNodeWithText("Almost there!").assertIsDisplayed()
        rule.onNodeWithText("Preview next stage").performScrollTo().assertIsDisplayed()
    }

    @Test fun largeScreenKeepsActionsReachable() {
        show(OrderUiState(order = order(OrderStatus.OUT_FOR_DELIVERY)), width = 600)
        rule.onNodeWithText("On the way!").assertIsDisplayed()
        rule.onNodeWithText("Preview next stage").performScrollTo().assertIsDisplayed()
    }
}
