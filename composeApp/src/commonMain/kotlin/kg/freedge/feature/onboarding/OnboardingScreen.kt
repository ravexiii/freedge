package kg.freedge.feature.onboarding

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import kg.freedge.app.LocalAppDeps
import kg.freedge.feature.main.isRussian

private data class OnboardingPage(val emoji: String, val title: String, val subtitle: String)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    val deps = LocalAppDeps.current
    val vm = viewModel<OnboardingViewModel> { OnboardingViewModel(deps.onboardingPrefs) }

    val isRu = remember { isRussian() }
    val pages = if (isRu) russianPages() else englishPages()

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

        HorizontalPager(state = pagerState, modifier = Modifier.weight(4f)) { index ->
            val page = pages[index]
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(page.emoji, fontSize = 52.sp, modifier = Modifier.padding(22.dp))
                }
                Spacer(Modifier.height(28.dp))
                Text(page.title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(
                    page.subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier.padding(vertical = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .size(if (selected) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        AnimatedVisibility(visible = isLastPage, enter = fadeIn(), exit = fadeOut()) {
            Button(
                onClick = { vm.completeOnboarding(onComplete) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(if (isRu) "Начать" else "Get started", fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

private fun russianPages() = listOf(
    OnboardingPage("📷", "Сфотографируйте холодильник", "Откройте холодильник, наведите камеру и сделайте снимок"),
    OnboardingPage("🍳", "Получите рецепты из того, что есть", "Freedge распознает продукты и предложит простые блюда")
)

private fun englishPages() = listOf(
    OnboardingPage("📷", "Take a fridge photo", "Open your fridge, aim the camera and take a shot"),
    OnboardingPage("🍳", "Get recipes from what you have", "Freedge identifies ingredients and suggests simple dishes")
)
