package kg.freedge.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kg.freedge.app.R

private data class OnboardingPage(val emoji: String, val title: String, val subtitle: String)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = viewModel()
) {
    val pages = listOf(
        OnboardingPage(
            emoji = "📷",
            title = stringResource(R.string.onboarding_1_title),
            subtitle = stringResource(R.string.onboarding_1_subtitle)
        ),
        OnboardingPage(
            emoji = "🍳",
            title = stringResource(R.string.onboarding_2_title),
            subtitle = stringResource(R.string.onboarding_2_subtitle)
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(4f)
        ) { page ->
            PageContent(page = pages[page])
        }

        // Индикаторы страниц
        Row(
            modifier = Modifier.padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            this@Column.AnimatedVisibility(
                visible = isLastPage,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Button(
                    onClick = { viewModel.completeOnboarding(onComplete) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(stringResource(R.string.onboarding_start), fontSize = 17.sp)
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 1.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = page.emoji, fontSize = 72.sp)
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = page.title,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = page.subtitle,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )
    }
}
