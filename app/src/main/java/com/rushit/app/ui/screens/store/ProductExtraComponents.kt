package com.rushit.app.ui.screens.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rushit.app.R
import com.rushit.app.data.model.*
import com.rushit.app.domain.formatPrice
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.components.RushButton
import com.rushit.app.ui.theme.*

@Composable
fun ExpandableSection(title: String, content: @Composable () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            Text(if (expanded) "−" else "+", style = MaterialTheme.typography.titleMedium, color = Coral)
        }
        androidx.compose.animation.AnimatedVisibility(visible = expanded) {
            Box(Modifier.padding(vertical = 8.dp)) {
                content()
            }
        }
        HorizontalDivider(color = Border)
    }
}

@Composable
fun ProductOffers(offers: List<String>) {
    if (offers.isEmpty()) return
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Available Offers", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        offers.forEach { offer ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                RushIcon(R.drawable.ic_package_01, null, Modifier.size(16.dp), Coral)
                Spacer(Modifier.width(8.dp))
                Text(offer, style = MaterialTheme.typography.bodyMedium, color = Ink)
            }
        }
    }
}

@Composable
fun PincodeChecker() {
    var pincode by remember { mutableStateOf("") }
    var result by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Check Delivery Options", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pincode,
                onValueChange = { if (it.length <= 6 && it.all { char -> char.isDigit() }) pincode = it },
                placeholder = { Text("Enter 6-digit Pincode", color = Muted) },
                modifier = Modifier.weight(1f).height(54.dp),
                textStyle = MaterialTheme.typography.bodyMedium,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Coral,
                    unfocusedBorderColor = Border,
                    focusedContainerColor = CardWhite,
                    unfocusedContainerColor = CardWhite
                ),
                trailingIcon = {
                    if (pincode.isNotEmpty()) {
                        RushIcon(R.drawable.ic_cancel_01, "Clear", Modifier.size(16.dp).clickable { pincode = ""; result = null }, Muted)
                    }
                }
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { 
                    if (pincode.length == 6) {
                        isLoading = true
                        scope.launch {
                            kotlinx.coroutines.delay(600)
                            isLoading = false
                            result = if (pincode.startsWith("1")) "Delivery not available for this area (Demo)." else "Delivery available in 3 days. COD eligible (Demo)."
                        }
                    } else {
                        result = "Please enter a valid 6-digit pincode."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
                modifier = Modifier.height(54.dp),
                shape = RoundedCornerShape(24.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Check", fontWeight = FontWeight.Bold)
                }
            }
        }
        if (result != null) {
            Text(result!!, style = MaterialTheme.typography.bodySmall, color = if (result!!.contains("available in")) Success else Coral, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
        }
    }
}

@Composable
fun ProductReviewsSection(reviews: List<Review>, onHelpful: (String) -> Unit = {}, onSubmitReview: (Review) -> Unit = {}) {
    var filter by remember { mutableStateOf("All") }
    var sort by remember { mutableStateOf("Most Recent") }
    val filters = listOf("All", "5 Star", "4 Star", "With Photos", "With Videos")
    val sorts = listOf("Most Recent", "Highest Rated", "Lowest Rated", "Most Helpful")
    
    val filtered = reviews.filter {
        when (filter) {
            "5 Star" -> it.rating == 5
            "4 Star" -> it.rating == 4
            "With Photos" -> it.hasPhotos
            "With Videos" -> it.hasVideos
            else -> true
        }
    }
    
    val sorted = when (sort) {
        "Highest Rated" -> filtered.sortedByDescending { it.rating }
        "Lowest Rated" -> filtered.sortedBy { it.rating }
        "Most Helpful" -> filtered.sortedByDescending { it.helpfulCount }
        else -> filtered.sortedByDescending { it.date }
    }
    
    Column(Modifier.padding(vertical = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ratings & Reviews", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.weight(1f))
            Text(sort, style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { 
                val nextIndex = (sorts.indexOf(sort) + 1) % sorts.size
                sort = sorts[nextIndex]
            })
        }
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(filters) { f ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (f == filter) Coral else CardWhite,
                    border = BorderStroke(1.dp, if (f == filter) Coral else Border),
                    modifier = Modifier.clickable { filter = f }
                ) {
                    Text(f, color = if (f == filter) Color.White else Ink, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (sorted.isEmpty()) {
            Text("No reviews found for this filter.", style = MaterialTheme.typography.bodyMedium, color = Muted)
        }
        sorted.forEach { review ->
            ReviewCard(review, onHelpful)
            Spacer(Modifier.height(12.dp))
        }
        Button(
            onClick = { /* Demo only */ },
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Coral),
            border = BorderStroke(1.dp, Coral),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Write a Review")
        }
    }
}

@Composable
fun ReviewCard(review: Review, onHelpful: (String) -> Unit = {}) {
    Surface(shape = RoundedCornerShape(12.dp), color = CardWhite, border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Peach), contentAlignment = Alignment.Center) {
                    Text(review.customerName.take(1).uppercase(), color = Coral, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(review.customerName, style = MaterialTheme.typography.labelMedium, color = Ink)
                    Text(review.date, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(8.dp), color = Success.copy(alpha = 0.1f)) {
                    Text("⭐ ${review.rating}", color = Success, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(review.title, style = MaterialTheme.typography.titleSmall, color = Ink)
            Text(review.body, style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(top = 4.dp))
            if (review.hasPhotos || review.hasVideos) {
                Spacer(Modifier.height(8.dp))
                Text("Includes Media (Demo)", style = MaterialTheme.typography.labelSmall, color = Coral)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Text("Helpful (${review.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpful(review.id) }.padding(end = 12.dp))
                Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.clickable { /* Demo */ })
            }
        }
    }
}

@Composable
fun ProductQASection(comments: List<Comment>, questions: List<Question>, onHelpfulComment: (String) -> Unit = {}, onHelpfulQuestion: (String) -> Unit = {}, onSubmitComment: (Comment) -> Unit = {}, onSubmitQuestion: (Question) -> Unit = {}) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.padding(vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Comments", style = MaterialTheme.typography.titleMedium, color = if (tab == 0) Coral else Muted, modifier = Modifier.clickable { tab = 0 })
            Text("Q&A", style = MaterialTheme.typography.titleMedium, color = if (tab == 1) Coral else Muted, modifier = Modifier.clickable { tab = 1 })
        }
        Spacer(Modifier.height(16.dp))
        if (tab == 0) {
            if (comments.isEmpty()) Text("No comments yet.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            comments.forEach { c ->
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("${c.author}: ${c.text}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Helpful (${c.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpfulComment(c.id) })
                        Text("Reply", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    c.replies.forEach { r ->
                        Text("↳ ${r.author}: ${r.text}", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                }
            }
            var showWriteComment by remember { mutableStateOf(false) }
            Text("+ Add Comment", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { showWriteComment = true })
            if (showWriteComment) {
                var text by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showWriteComment = false },
                    title = { Text("Add Comment") },
                    text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Write your comment") }) },
                    confirmButton = { TextButton(onClick = { onSubmitComment(Comment(java.util.UUID.randomUUID().toString(), "", "Demo User", text, "Just now")); showWriteComment = false }) { Text("Submit") } }
                )
            }
        } else {
            if (questions.isEmpty()) Text("No questions yet.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            questions.forEach { q ->
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("Q: ${q.text}", style = MaterialTheme.typography.labelMedium, color = Ink)
                    if (q.answer != null) {
                        Text("A: ${q.answer}", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                    Row(Modifier.padding(top = 4.dp, start = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Helpful (${q.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpfulQuestion(q.id) })
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            var showWriteQuestion by remember { mutableStateOf(false) }
            Text("+ Ask a Question", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { showWriteQuestion = true })
            if (showWriteQuestion) {
                var text by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showWriteQuestion = false },
                    title = { Text("Ask a Question") },
                    text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("What do you want to know?") }) },
                    confirmButton = { TextButton(onClick = { onSubmitQuestion(Question(java.util.UUID.randomUUID().toString(), "", "Demo User", text, "Just now")); showWriteQuestion = false }) { Text("Submit") } }
                )
            }
        }
    }
}

@Composable
fun ProductRecommendations(title: String, products: List<Product>, onProduct: (String) -> Unit) {
    if (products.isEmpty()) return
    Column(Modifier.padding(vertical = 16.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = Ink, )
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(products) { p ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CardWhite,
                    border = BorderStroke(1.dp, Border),
                    modifier = Modifier.width(140.dp).clickable { onProduct(p.id) }
                ) {
                    Column(Modifier.padding(8.dp)) {
                        Box(Modifier.fillMaxWidth().height(100.dp).background(Peach, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                            CatalogImage(p.imageUrl, p.imageRes, p.name, Modifier.size(80.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(p.name, style = MaterialTheme.typography.labelMedium, color = Ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(formatPrice(p.pricePaise), style = MaterialTheme.typography.labelLarge, color = Ink)
                    }
                }
            }
        }
    }
}

@Composable
fun FrequentlyBoughtTogether(
    mainProduct: Product,
    recommendations: List<Product>,
    onAddToCart: (String) -> Unit
) {
    if (recommendations.isEmpty()) return
    
    val combo = recommendations.take(2)
    var selected = remember { mutableStateListOf<Product>().apply { addAll(combo) } }
    
    Column(Modifier.padding(vertical = 16.dp)) {
        Text("Frequently Bought Together (Demo)", style = MaterialTheme.typography.titleMedium, color = Ink, modifier = Modifier.padding(horizontal = 20.dp))
        Spacer(Modifier.height(12.dp))
        
        Surface(shape = RoundedCornerShape(12.dp), color = CardWhite, border = BorderStroke(1.dp, Border), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
            Column(Modifier.padding(16.dp)) {
                // Main Product
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = true, onCheckedChange = null, colors = CheckboxDefaults.colors(checkedColor = Muted, disabledCheckedColor = Muted), enabled = false)
                    Spacer(Modifier.width(8.dp))
                    Text("This item: ${mainProduct.name}", style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f))
                    Text(formatPrice(mainProduct.pricePaise), style = MaterialTheme.typography.labelMedium, color = Ink)
                }
                
                // Recommended Products
                combo.forEach { p ->
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                        Checkbox(
                            checked = selected.contains(p),
                            onCheckedChange = { if (it) selected.add(p) else selected.remove(p) },
                            colors = CheckboxDefaults.colors(checkedColor = Coral)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(p.name, style = MaterialTheme.typography.bodyMedium, color = Ink, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatPrice(p.pricePaise), style = MaterialTheme.typography.labelMedium, color = Ink)
                    }
                }
                
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = Border)
                Spacer(Modifier.height(12.dp))
                
                val total = mainProduct.pricePaise + selected.sumOf { it.pricePaise }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Total Price:", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text(formatPrice(total), style = MaterialTheme.typography.titleMedium, color = Ink)
                    }
                    Button(
                        onClick = { 
                            onAddToCart(mainProduct.id)
                            selected.forEach { onAddToCart(it.id) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Coral),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Add Selected to Cart")
                    }
                }
            }
        }
    }
}
