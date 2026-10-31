import re

with open('app/src/main/java/com/rushit/app/ui/viewmodel/ShopViewModel.kt', 'r') as f:
    content = f.read()

# Make the lists mutable by replacing them in ShopUiState
# Wait, it's better to keep ShopUiState immutable, and just let ViewModel replace them.

new_methods = """
    fun submitReview(productId: String, review: com.rushit.app.data.model.Review) {
        _state.update { it.copy(allReviews = listOf(review) + it.allReviews) }
    }

    fun submitComment(productId: String, comment: com.rushit.app.data.model.Comment) {
        _state.update { it.copy(allComments = listOf(comment) + it.allComments) }
    }

    fun submitQuestion(productId: String, question: com.rushit.app.data.model.Question) {
        _state.update { it.copy(allQuestions = listOf(question) + it.allQuestions) }
    }

    fun markReviewHelpful(reviewId: String) {
        _state.update { 
            val updated = it.allReviews.map { r -> if (r.id == reviewId) r.copy(helpfulCount = r.helpfulCount + 1) else r }
            it.copy(allReviews = updated) 
        }
    }

    fun markCommentHelpful(commentId: String) {
        _state.update { 
            val updated = it.allComments.map { c -> if (c.id == commentId) c.copy(helpfulCount = c.helpfulCount + 1) else c }
            it.copy(allComments = updated) 
        }
    }

    fun markQuestionHelpful(questionId: String) {
        _state.update { 
            val updated = it.allQuestions.map { q -> if (q.id == questionId) q.copy(helpfulCount = q.helpfulCount + 1) else q }
            it.copy(allQuestions = updated) 
        }
    }
"""

content = re.sub(
    r'    private fun readRecentlyViewed',
    new_methods + '\n    private fun readRecentlyViewed',
    content
)

with open('app/src/main/java/com/rushit/app/ui/viewmodel/ShopViewModel.kt', 'w') as f:
    f.write(content)
