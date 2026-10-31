package com.rushit.app.ui.screens.store

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rushit.app.ui.components.ScreenHeader
import com.rushit.app.ui.theme.Cream
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Muted
import com.rushit.app.ui.viewmodel.ShopUiState

@Composable
fun FullProductDetailsScreen(state: ShopUiState, productId: String, onBack: () -> Unit) {
    val product = remember(state.products, productId) { state.products.firstOrNull { it.id == productId } }
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("Full Details", onBack)
        if (product == null) {
            Text("Product not found", Modifier.padding(16.dp))
            return
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleLarge, color = Ink, modifier = Modifier.padding(vertical = 12.dp))
            Text(product.description, style = MaterialTheme.typography.bodyMedium, color = Muted)
            
            if (product.specifications.isNotEmpty()) {
                ExpandableSection("Specifications") {
                    product.specifications.forEach { (key, value) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                            Text(key, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(0.4f))
                            Text(value, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(0.6f))
                        }
                    }
                }
            }
            if (product.keyFeatures.isNotEmpty()) {
                ExpandableSection("Key Features") {
                    product.keyFeatures.forEach { feature ->
                        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.Top) {
                            Text("•", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(end = 8.dp))
                            Text(feature, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (product.whatsInTheBox != null) ExpandableSection("What's in the Box") { Text(product.whatsInTheBox, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            if (product.careInstructions != null) ExpandableSection("Care Instructions") { Text(product.careInstructions, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            if (product.warranty != null) ExpandableSection("Warranty") { Text(product.warranty, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            if (product.returnPolicy != null) ExpandableSection("Return Policy") { Text(product.returnPolicy, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            if (product.sellerInformation != null) ExpandableSection("Seller Information") { Text(product.sellerInformation, style = MaterialTheme.typography.bodyMedium, color = Muted) }
            
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ProductReviewsScreen(state: ShopUiState, productId: String, onBack: () -> Unit, onHelpfulReview: (String) -> Unit, onSubmitReview: (Review) -> Unit) {
    val reviews = state.reviews(productId)
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("All Reviews", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ProductReviewsSection(reviews, onHelpful = onHelpfulReview, onSubmitReview = { r -> onSubmitReview(r.copy(productId = productId)) })
        }
    }
}

@Composable
fun ProductCommentsScreen(state: ShopUiState, productId: String, onBack: () -> Unit, onHelpfulComment: (String) -> Unit, onHelpfulQuestion: (String) -> Unit, onSubmitComment: (Comment) -> Unit, onSubmitQuestion: (Question) -> Unit) {
    val comments = state.comments(productId)
    Column(Modifier.fillMaxSize().background(Cream)) {
        ScreenHeader("All Comments", onBack)
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            ProductQASection(comments, state.questions(productId), onHelpfulComment = onHelpfulComment, onHelpfulQuestion = onHelpfulQuestion, onSubmitComment = { c -> onSubmitComment(c.copy(productId = productId)) }, onSubmitQuestion = { q -> onSubmitQuestion(q.copy(productId = productId)) })
        }
    }
}
