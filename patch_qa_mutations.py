import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'r') as f:
    content = f.read()

# Update signature of ProductReviewsSection
content = content.replace(
    'fun ProductReviewsSection(reviews: List<Review>) {',
    'fun ProductReviewsSection(reviews: List<Review>, onHelpful: (String) -> Unit = {}, onSubmitReview: (Review) -> Unit = {}) {'
)

# Replace the "Write a Review" button and ReviewCard in ProductReviewsSection
write_review = """        var showWriteReview by remember { mutableStateOf(false) }
        Button(
            onClick = { showWriteReview = true },
            colors = ButtonDefaults.buttonColors(containerColor = CardWhite, contentColor = Coral),
            border = BorderStroke(1.dp, Coral),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Write a Review")
        }
        
        if (showWriteReview) {
            var title by remember { mutableStateOf("") }
            var body by remember { mutableStateOf("") }
            var rating by remember { mutableStateOf(5) }
            
            AlertDialog(
                onDismissRequest = { showWriteReview = false },
                title = { Text("Write a Review") },
                text = {
                    Column {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (1..5).forEach { r ->
                                Text("⭐", modifier = Modifier.clickable { rating = r }, color = if (rating >= r) Coral else Muted)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = title, onValueChange = { title = it }, placeholder = { Text("Title") })
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(value = body, onValueChange = { body = it }, placeholder = { Text("Describe your experience") })
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        val review = Review(java.util.UUID.randomUUID().toString(), "", "Demo User", rating, title, body, "Just now")
                        onSubmitReview(review)
                        showWriteReview = false
                    }) { Text("Submit") }
                },
                dismissButton = {
                    TextButton(onClick = { showWriteReview = false }) { Text("Cancel") }
                }
            )
        }"""

content = re.sub(
    r'        Button\(\n            onClick = \{ /\* Demo \*/ \},\n            colors = ButtonDefaults.buttonColors\(containerColor = CardWhite, contentColor = Coral\),\n            border = BorderStroke\(1.dp, Coral\),\n            modifier = Modifier.fillMaxWidth\(\)\n        \) \{\n            Text\("Write a Review"\)\n        \}',
    write_review,
    content
)

# Update ReviewCard Helpful click
content = content.replace(
    'fun ReviewCard(review: Review) {',
    'fun ReviewCard(review: Review, onHelpful: (String) -> Unit = {}) {'
)
content = content.replace(
    'Text("Helpful (${review.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { /* Demo */ }.padding(end = 12.dp))',
    'Text("Helpful (${review.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpful(review.id) }.padding(end = 12.dp))'
)
content = content.replace(
    'ReviewCard(review)',
    'ReviewCard(review, onHelpful)'
)

# Update ProductQASection
content = content.replace(
    'fun ProductQASection(comments: List<Comment>, questions: List<Question>) {',
    'fun ProductQASection(comments: List<Comment>, questions: List<Question>, onHelpfulComment: (String) -> Unit = {}, onHelpfulQuestion: (String) -> Unit = {}, onSubmitComment: (Comment) -> Unit = {}, onSubmitQuestion: (Question) -> Unit = {}) {'
)

qa_actions = """                    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Helpful (${c.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpfulComment(c.id) })
                        Text("Reply", style = MaterialTheme.typography.bodySmall, color = Muted)
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }"""

content = re.sub(
    r'                    Row\(Modifier.padding\(vertical = 4.dp\), horizontalArrangement = Arrangement.spacedBy\(12.dp\)\) \{\n                        Text\("Helpful \(\$\{c.helpfulCount\}\)", style = MaterialTheme.typography.bodySmall, color = Coral\)\n                        Text\("Reply", style = MaterialTheme.typography.bodySmall, color = Muted\)\n                        Text\("Report", style = MaterialTheme.typography.bodySmall, color = Muted\)\n                    \}',
    qa_actions,
    content
)

qa_question_actions = """                    Row(Modifier.padding(top = 4.dp, start = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Helpful (${q.helpfulCount})", style = MaterialTheme.typography.bodySmall, color = Coral, modifier = Modifier.clickable { onHelpfulQuestion(q.id) })
                        Text("Report", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }"""

content = re.sub(
    r'                    Row\(Modifier.padding\(top = 4.dp, start = 16.dp\), horizontalArrangement = Arrangement.spacedBy\(12.dp\)\) \{\n                        Text\("Helpful \(\$\{q.helpfulCount\}\)", style = MaterialTheme.typography.bodySmall, color = Coral\)\n                        Text\("Report", style = MaterialTheme.typography.bodySmall, color = Muted\)\n                    \}',
    qa_question_actions,
    content
)

# Add Comment and Question forms
qa_forms = """            var showWriteComment by remember { mutableStateOf(false) }
            Text("+ Add Comment", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { showWriteComment = true })
            if (showWriteComment) {
                var text by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showWriteComment = false },
                    title = { Text("Add Comment") },
                    text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("Write your comment") }) },
                    confirmButton = { TextButton(onClick = { onSubmitComment(Comment(java.util.UUID.randomUUID().toString(), "", "Demo User", text, "Just now")); showWriteComment = false }) { Text("Submit") } }
                )
            }"""

content = content.replace(
    '            Text("+ Add Comment", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { /* Demo */ })',
    qa_forms
)

qa_q_forms = """            var showWriteQuestion by remember { mutableStateOf(false) }
            Text("+ Ask a Question", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { showWriteQuestion = true })
            if (showWriteQuestion) {
                var text by remember { mutableStateOf("") }
                AlertDialog(
                    onDismissRequest = { showWriteQuestion = false },
                    title = { Text("Ask a Question") },
                    text = { OutlinedTextField(value = text, onValueChange = { text = it }, placeholder = { Text("What do you want to know?") }) },
                    confirmButton = { TextButton(onClick = { onSubmitQuestion(Question(java.util.UUID.randomUUID().toString(), "", "Demo User", text, "Just now")); showWriteQuestion = false }) { Text("Submit") } }
                )
            }"""

content = content.replace(
    '            Text("+ Ask a Question", style = MaterialTheme.typography.labelMedium, color = Coral, modifier = Modifier.clickable { /* Demo */ })',
    qa_q_forms
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductExtraComponents.kt', 'w') as f:
    f.write(content)
