package dev.anilbeesetti.nextplayer.settings.navigation

import androidx.compose.runtime.SideEffect
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import dev.anilbeesetti.nextplayer.settings.screens.subtitle.SubtitlePreferencesScreen
import dev.anilbeesetti.nextplayer.settings.screens.subtitle.SubtitlePreferencesViewModel
import kotlinx.serialization.Serializable
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Serializable
object SubtitlePreferencesRoute : NavKey

fun NavBackStack<NavKey>.navigateToSubtitlePreferences() {
    add(element = SubtitlePreferencesRoute)
}

fun EntryProviderScope<NavKey>.subtitlePreferencesEntry(onNavigateUp: () -> Unit) {
    entry<SubtitlePreferencesRoute> {
        val context = androidx.compose.ui.platform.LocalContext.current
        val output = SubtitlePreferencesViewModel.Output(
            navigateUp = {
                val activity = context as? android.app.Activity
                if (activity?.intent?.getBooleanExtra(
                        "open_subtitle_settings",
                        false,
                    ) == true
                ) {
                    activity.finish()
                } else {
                    onNavigateUp()
                }
            },
        )
        val viewModel = koinViewModel<SubtitlePreferencesViewModel>(
            parameters = { parametersOf(output) },
        )
        SideEffect { viewModel.output = output }
        SubtitlePreferencesScreen(viewModel = viewModel)
    }
}
