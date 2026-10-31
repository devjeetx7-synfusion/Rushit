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
        if (expanded) {
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
    Column(Modifier.padding(vertical = 12.dp)) {
        Text("Check Delivery Options", style = MaterialTheme.typography.titleMedium, color = Ink)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = pincode,
                onValueChange = { pincode = it.take(6) },
                placeholder = { Text("Enter Pincode") },
                modifier = Modifier.weight(1f).height(50.dp),
                textStyle = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { result = if (pincode.length == 6) "Delivery available by tomorrow. COD available." else "Invalid pincode" },
                colors = ButtonDefaults.buttonColors(containerColor = Coral),
                modifier = Modifier.height(50.dp)
            ) {
                Text("Check")
            }
        }
        if (result != null) {
            Text(result!!, style = MaterialTheme.typography.bodySmall, color = if (result!!.contains("available")) Success else Muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun ProductReviewsSection(reviews: List<Review>) {
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
            ReviewCard(review)
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
fun ReviewCard(review: Review) {
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
                Text("Helpful (${review.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { /* Demo */ }.padding(end = 12.dp))
                Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.clickable { /* Demo */ })
            }
        }
    }
}

@Composable
fun ProductQASection(comments: List<Comment>, questions: List<Question>) {
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
                        Text("Helpful (${c.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral)
                        Text("Reply", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    c.replies.forEach { r ->
                        Text("↳ ${r.author}: ${r.text}", style = MaterialTheme.typography.bodySmall, color = Muted, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                }
            }
            Text("+ Add Comment", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { /* Demo */ })
        } else {
            if (questions.isEmpty()) Text("No questions yet.", style = MaterialTheme.typography.bodyMedium, color = Muted)
            questions.forEach { q ->
                Column(Modifier.padding(bottom = 12.dp)) {
                    Text("Q: ${q.text}", style = MaterialTheme.typography.labelMedium, color = Ink)
                    if (q.answer != null) {
                        Text("A: ${q.answer}", style = MaterialTheme.typography.bodyMedium, color = Muted, modifier = Modifier.padding(start = 16.dp, top = 4.dp))
                    }
                    Row(Modifier.padding(top = 4.dp, start = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Helpful (${q.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral)
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
            Text("+ Ask a Question", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { /* Demo */ })
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
