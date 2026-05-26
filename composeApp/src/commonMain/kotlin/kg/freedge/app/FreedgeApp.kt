package kg.freedge.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import kg.freedge.app.theme.FreedgeTheme

@Composable
fun FreedgeApp(deps: AppDependencies) {
    CompositionLocalProvider(LocalAppDeps provides deps) {
        FreedgeTheme {
            AppNavGraph()
        }
    }
}
