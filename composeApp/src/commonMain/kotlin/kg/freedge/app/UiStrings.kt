package kg.freedge.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kg.freedge.feature.main.isRussian

data class UiStrings(
    val allow: String,
    val cancel: String,
    val cameraPermissionBody: String,
    val cameraPermissionTitle: String,
    val cameraReadyHint: String,
    val copy: String,
    val delete: String,
    val deleteScanBody: String,
    val deleteScanDetailBody: String,
    val deleteScanTitle: String,
    val generatedInFreedge: String,
    val getStarted: String,
    val history: String,
    val historyEmpty: String,
    val inspiration: String,
    val next: String,
    val photo: String,
    val recipeIdeas: String,
    val retry: String,
    val scanDetail: String,
    val share: String,
    val takeAnother: String,
    val onboardingPhotoTitle: String,
    val onboardingPhotoBody: String,
    val onboardingRecipesTitle: String,
    val onboardingRecipesBody: String,
    val servingIdeas: String
) {
    fun photoBy(name: String): String =
        if (this === RuUiStrings) "Фото: $name / Pexels" else "Photo: $name / Pexels"
}

@Composable
fun rememberUiStrings(): UiStrings {
    val isRu = isRussian()
    return remember(isRu) { if (isRu) RuUiStrings else EnUiStrings }
}

private val EnUiStrings = UiStrings(
    allow = "Allow",
    cancel = "Cancel",
    cameraPermissionBody = "Freedge needs camera access to photograph your fridge.",
    cameraPermissionTitle = "Camera access required",
    cameraReadyHint = "Aim at the open fridge and take a clear photo.",
    copy = "Copy",
    delete = "Delete",
    deleteScanBody = "Photo and recipe will be removed from history.",
    deleteScanDetailBody = "Photo and recipe will be deleted.",
    deleteScanTitle = "Delete scan?",
    generatedInFreedge = "Generated in Freedge",
    getStarted = "Get started",
    history = "History",
    historyEmpty = "No scans yet",
    inspiration = "Inspiration",
    next = "Next",
    photo = "Photo",
    recipeIdeas = "Recipe ideas",
    retry = "Retry",
    scanDetail = "Scan detail",
    share = "Share",
    takeAnother = "Take another",
    onboardingPhotoTitle = "Take a fridge photo",
    onboardingPhotoBody = "Open your fridge, aim the camera and take a clear shot.",
    onboardingRecipesTitle = "Get recipes from what you have",
    onboardingRecipesBody = "Freedge identifies ingredients and suggests simple dishes.",
    servingIdeas = "Serving ideas"
)

private val RuUiStrings = UiStrings(
    allow = "Разрешить",
    cancel = "Отмена",
    cameraPermissionBody = "Freedge нужен доступ к камере, чтобы сфотографировать холодильник.",
    cameraPermissionTitle = "Нужен доступ к камере",
    cameraReadyHint = "Наведи камеру на открытый холодильник и сделай четкий снимок.",
    copy = "Копировать",
    delete = "Удалить",
    deleteScanBody = "Фото и рецепт будут удалены из истории.",
    deleteScanDetailBody = "Фото и рецепт будут удалены.",
    deleteScanTitle = "Удалить скан?",
    generatedInFreedge = "Сгенерировано в Freedge",
    getStarted = "Начать",
    history = "История",
    historyEmpty = "Пока нет сканов",
    inspiration = "Вдохновение",
    next = "Далее",
    photo = "Фото",
    recipeIdeas = "Идеи для готовки",
    retry = "Попробовать снова",
    scanDetail = "Детали скана",
    share = "Поделиться",
    takeAnother = "Сфотографировать еще",
    onboardingPhotoTitle = "Сфотографируйте холодильник",
    onboardingPhotoBody = "Откройте холодильник, наведите камеру и сделайте четкий снимок.",
    onboardingRecipesTitle = "Получите рецепты из того, что есть",
    onboardingRecipesBody = "Freedge распознает продукты и предложит простые блюда.",
    servingIdeas = "Пример подачи"
)
