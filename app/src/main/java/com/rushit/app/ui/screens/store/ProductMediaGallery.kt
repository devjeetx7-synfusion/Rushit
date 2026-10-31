package com.rushit.app.ui.screens.store

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.rushit.app.R
import com.rushit.app.data.model.MediaType
import com.rushit.app.data.model.ProductMedia
import com.rushit.app.ui.components.CatalogImage
import com.rushit.app.ui.components.RushIcon
import com.rushit.app.ui.theme.CardWhite
import com.rushit.app.ui.theme.Coral
import com.rushit.app.ui.theme.Ink
import com.rushit.app.ui.theme.Peach
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableProductMediaGallery(
    media: List<ProductMedia>,
    productName: String,
    modifier: Modifier = Modifier
) {
    if (media.isEmpty()) {
        Box(modifier.background(Peach, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            CatalogImage(null, null, productName, Modifier.size(220.dp))
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { media.size })
    val scope = rememberCoroutineScope()
    var fullScreenIndex by remember { mutableStateOf<Int?>(null) }

    Column(modifier) {
        Box(Modifier.fillMaxWidth().height(250.dp).background(Peach, RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                val m = media[page]
                Box(Modifier.fillMaxSize().clickable { fullScreenIndex = page }, contentAlignment = Alignment.Center) {
                    if (m.type == MediaType.VIDEO) {
                        Text("Video Preview (Demo)", style = MaterialTheme.typography.titleLarge, color = Coral)
                        RushIcon(R.drawable.ic_launcher, "Play Video", Modifier.size(48.dp).align(Alignment.Center), Coral)
                    } else {
                        CatalogImage(m.url, m.resId, productName, Modifier.size(220.dp))
                    }
                }
            }
            
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = CardWhite.copy(alpha = 0.8f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp)
            ) {
                Text("${pagerState.currentPage + 1}/${media.size}", style = MaterialTheme.typography.labelSmall, color = Ink, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
        }
        
        Spacer(Modifier.height(12.dp))
        
        if (media.size > 1) {
            val listState = rememberLazyListState()
            LaunchedEffect(pagerState.currentPage) {
                listState.animateScrollToItem(pagerState.currentPage)
            }
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(media) { index, m ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Peach)
                            .border(2.dp, if (isSelected) Coral else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } },
                        contentAlignment = Alignment.Center
                    ) {
                        if (m.type == MediaType.VIDEO) {
                            Text("▶", style = MaterialTheme.typography.bodyLarge, color = Coral)
                        } else {
                            CatalogImage(m.url, m.resId, "Thumbnail", Modifier.size(50.dp))
                        }
                    }
                }
            }
        }
    }
    
    if (fullScreenIndex != null) {
        // Simple full screen viewer simulation
        androidx.compose.ui.window.Dialog(onDismissRequest = { fullScreenIndex = null }, properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                HorizontalPager(state = rememberPagerState(initialPage = fullScreenIndex!!, pageCount = { media.size }), modifier = Modifier.fillMaxSize()) { page ->
                    val m = media[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (m.type == MediaType.VIDEO) {
                            Text("Video Playing... (Demo)", color = Color.White)
                        } else {
                            CatalogImage(m.url, m.resId, productName, Modifier.fillMaxSize())
                        }
                    }
                }
                IconButton(onClick = { fullScreenIndex = null }, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
                    Text("✕", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                }
            }
        }
    }
}
