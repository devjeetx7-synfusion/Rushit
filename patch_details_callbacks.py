import re

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'r') as f:
    content = f.read()

# Update ProductDetailsScreen signature
content = content.replace(
    '    onComments: () -> Unit,\n) {',
    '    onComments: () -> Unit,\n    onHelpfulReview: (String) -> Unit,\n    onHelpfulComment: (String) -> Unit,\n    onHelpfulQuestion: (String) -> Unit,\n    onSubmitReview: (Review) -> Unit,\n    onSubmitComment: (Comment) -> Unit,\n    onSubmitQuestion: (Question) -> Unit,\n) {'
)

# Update ProductReviewsSection call
content = content.replace(
    'ProductReviewsSection(state.reviews(product.id).take(2))',
    'ProductReviewsSection(state.reviews(product.id).take(2), onHelpful = onHelpfulReview, onSubmitReview = { r -> onSubmitReview(r.copy(productId = product.id)) })'
)

# Update ProductQASection call
content = content.replace(
    'ProductQASection(state.comments(product.id).take(2), state.questions(product.id).take(2))',
    'ProductQASection(state.comments(product.id).take(2), state.questions(product.id).take(2), onHelpfulComment = onHelpfulComment, onHelpfulQuestion = onHelpfulQuestion, onSubmitComment = { c -> onSubmitComment(c.copy(productId = product.id)) }, onSubmitQuestion = { q -> onSubmitQuestion(q.copy(productId = product.id)) })'
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductDetailsScreen.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductScreensExt.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'fun ProductReviewsScreen(state: ShopUiState, productId: String, onBack: () -> Unit) {',
    'fun ProductReviewsScreen(state: ShopUiState, productId: String, onBack: () -> Unit, onHelpfulReview: (String) -> Unit, onSubmitReview: (Review) -> Unit) {'
)
content = content.replace(
    'ProductReviewsSection(reviews)',
    'ProductReviewsSection(reviews, onHelpful = onHelpfulReview, onSubmitReview = { r -> onSubmitReview(r.copy(productId = productId)) })'
)

content = content.replace(
    'fun ProductCommentsScreen(state: ShopUiState, productId: String, onBack: () -> Unit) {',
    'fun ProductCommentsScreen(state: ShopUiState, productId: String, onBack: () -> Unit, onHelpfulComment: (String) -> Unit, onHelpfulQuestion: (String) -> Unit, onSubmitComment: (Comment) -> Unit, onSubmitQuestion: (Question) -> Unit) {'
)
content = content.replace(
    'ProductQASection(comments, emptyList())',
    'ProductQASection(comments, state.questions(productId), onHelpfulComment = onHelpfulComment, onHelpfulQuestion = onHelpfulQuestion, onSubmitComment = { c -> onSubmitComment(c.copy(productId = productId)) }, onSubmitQuestion = { q -> onSubmitQuestion(q.copy(productId = productId)) })'
)

with open('app/src/main/java/com/rushit/app/ui/screens/store/ProductScreensExt.kt', 'w') as f:
    f.write(content)

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'r') as f:
    content = f.read()

content = content.replace(
    'onComments = { nav.navigate("product-comments/${android.net.Uri.encode(id)}") { launchSingleTop = true } })',
    'onComments = { nav.navigate("product-comments/${android.net.Uri.encode(id)}") { launchSingleTop = true } },\n                            onHelpfulReview = shop::markReviewHelpful,\n                            onHelpfulComment = shop::markCommentHelpful,\n                            onHelpfulQuestion = shop::markQuestionHelpful,\n                            onSubmitReview = { shop.submitReview(id, it) },\n                            onSubmitComment = { shop.submitComment(id, it) },\n                            onSubmitQuestion = { shop.submitQuestion(id, it) })'
)

content = content.replace(
    'if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductReviewsScreen(shopState, id, onBack = { nav.popBackStack() })',
    'if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductReviewsScreen(shopState, id, onBack = { nav.popBackStack() }, onHelpfulReview = shop::markReviewHelpful, onSubmitReview = { shop.submitReview(id, it) })'
)

content = content.replace(
    'if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductCommentsScreen(shopState, id, onBack = { nav.popBackStack() })',
    'if (!needsStartupRoute) com.rushit.app.ui.screens.store.ProductCommentsScreen(shopState, id, onBack = { nav.popBackStack() }, onHelpfulComment = shop::markCommentHelpful, onHelpfulQuestion = shop::markQuestionHelpful, onSubmitComment = { shop.submitComment(id, it) }, onSubmitQuestion = { shop.submitQuestion(id, it) })'
)

with open('app/src/main/java/com/rushit/app/ui/navigation/RushItApp.kt', 'w') as f:
    f.write(content)

