package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import dev.anilbeesetti.nextplayer.core.data.repository.fake.FakePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SubtitlePreferencesViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repository = FakePreferencesRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher = dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updateSubtitleTextColorUpdatesRepositoryAndState() = runTest(context = dispatcher) {
        val viewModel = SubtitlePreferencesViewModel(
            preferencesRepository = repository,
            output = SubtitlePreferencesViewModel.Output(navigateUp = {}),
        )

        val newTextColor = 0xFFFFFF00.toInt()
        viewModel.onAction(
            action = SubtitlePreferencesUiEvent.UpdateSubtitleTextColor(newTextColor),
        )
        advanceUntilIdle()

        assertEquals(
            newTextColor,
            repository.playerPreferences.value.subtitleTextColor,
        )
        assertEquals(
            newTextColor,
            viewModel.state.value.preferences.subtitleTextColor,
        )
    }

    @Test
    fun updateSubtitleShadowColorUpdatesRepositoryAndState() = runTest(context = dispatcher) {
        val viewModel = SubtitlePreferencesViewModel(
            preferencesRepository = repository,
            output = SubtitlePreferencesViewModel.Output(navigateUp = {}),
        )

        val newShadowColor = 0xFFFF0000.toInt()
        viewModel.onAction(
            action = SubtitlePreferencesUiEvent.UpdateSubtitleShadowColor(newShadowColor),
        )
        advanceUntilIdle()

        assertEquals(
            newShadowColor,
            repository.playerPreferences.value.subtitleShadowColor,
        )
        assertEquals(
            newShadowColor,
            viewModel.state.value.preferences.subtitleShadowColor,
        )
    }

    @Test
    fun updateSubtitleShadowRadiusUpdatesRepositoryAndState() = runTest(context = dispatcher) {
        val viewModel = SubtitlePreferencesViewModel(
            preferencesRepository = repository,
            output = SubtitlePreferencesViewModel.Output(navigateUp = {}),
        )

        val newRadius = 5.0f
        viewModel.onAction(
            action = SubtitlePreferencesUiEvent.UpdateSubtitleShadowRadius(newRadius),
        )
        advanceUntilIdle()

        assertEquals(
            newRadius,
            repository.playerPreferences.value.subtitleShadowRadius,
            0.01f,
        )
        assertEquals(
            newRadius,
            viewModel.state.value.preferences.subtitleShadowRadius,
            0.01f,
        )
    }
}
