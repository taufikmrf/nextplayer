package dev.anilbeesetti.nextplayer.feature.player

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.sharp.Battery0Bar
import androidx.compose.material.icons.sharp.Battery1Bar
import androidx.compose.material.icons.sharp.Battery2Bar
import androidx.compose.material.icons.sharp.Battery3Bar
import androidx.compose.material.icons.sharp.Battery4Bar
import androidx.compose.material.icons.sharp.Battery5Bar
import androidx.compose.material.icons.sharp.Battery6Bar
import androidx.compose.material.icons.sharp.BatteryChargingFull
import androidx.compose.material.icons.sharp.BatteryFull
import androidx.compose.material.icons.sharp.HourglassBottom
import androidx.compose.material.icons.sharp.HourglassDisabled
import androidx.compose.material.icons.sharp.HourglassEmpty
import androidx.compose.material.icons.sharp.HourglassFull
import androidx.compose.material.icons.sharp.HourglassTop
import androidx.compose.material.icons.sharp.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util.getStringForTime
import androidx.media3.ui.compose.state.rememberProgressStateWithTickInterval
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.feature.player.buttons.PlayerButtonBlackAlpha
import dev.anilbeesetti.nextplayer.feature.player.state.ControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.PlayerOrientationEffect
import dev.anilbeesetti.nextplayer.feature.player.state.rememberBrightnessState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberControlsVisibilityState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberCuesState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberErrorState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPictureInPictureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberSeekGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberSubtitlePositionState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberTapGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVideoZoomAndContentScaleState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeAndBrightnessGestureState
import dev.anilbeesetti.nextplayer.feature.player.state.rememberVolumeState
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerErrorDialogs
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerGestures
import dev.anilbeesetti.nextplayer.feature.player.ui.PlayerVerticalGestureIndicators
import dev.anilbeesetti.nextplayer.feature.player.ui.SubtitleConfiguration
import dev.anilbeesetti.nextplayer.feature.player.ui.preview.rememberPreviewPlayer
import dev.anilbeesetti.nextplayer.feature.player.utils.PlayerApi
import dev.anilbeesetti.nextplayer.settings.screens.subtitle.SubtitlePreferencesScreen
import dev.anilbeesetti.nextplayer.settings.screens.subtitle.SubtitlePreferencesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

val LocalControlsVisibilityState = compositionLocalOf<ControlsVisibilityState?> { null }
val LocalUseMaterialYouControls = compositionLocalOf { false }

@OptIn(UnstableApi::class)
@Composable
fun MediaPlayerScreen(viewModel: PlayerViewModel, player: Player?, api: PlayerApi? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showSubtitlePreferences by remember { mutableStateOf(false) }

    if (showSubtitlePreferences) {
        val subtitlePreferencesViewModel = koinViewModel<SubtitlePreferencesViewModel>(
            parameters = {
                parametersOf(
                    SubtitlePreferencesViewModel.Output(
                        navigateUp = { showSubtitlePreferences = false },
                    ),
                )
            },
        )
        SubtitlePreferencesScreen(viewModel = subtitlePreferencesViewModel)
    } else {
        MediaPlayerContent(
            player = player,
            state = state,
            onAction = viewModel::onAction,
            api = api,
            onOpenSubtitleSettingsClick = { showSubtitlePreferences = true },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
internal fun MediaPlayerContent(
    player: Player?,
    state: PlayerUiState,
    onAction: (PlayerAction) -> Unit,
    api: PlayerApi? = null,
    onOpenSubtitleSettingsClick: () -> Unit = {},
) {
    val playerPreferences = state.playerPreferences
    val isPreview = LocalInspectionMode.current
    // Layout previews have neither a PlayerActivity nor a live video/audio session.
    val volumeState = if (!isPreview) {
        rememberVolumeState(
            player = player,
            showVolumePanelIfHeadsetIsOn = playerPreferences.showSystemVolumePanel,
        )
    } else {
        null
    }
    if (player == null) {
        Box(Modifier.fillMaxSize().background(Color.Black))
        return
    }
    val context = LocalContext.current
    val controlsVisibilityState = rememberControlsVisibilityState(
        player = player,
        hideAfter = playerPreferences.controllerAutoHideTimeout.seconds,
    )
    val tapGestureState = rememberTapGestureState(
        player = player,
        doubleTapGesture = playerPreferences.doubleTapGesture,
        seekIncrementMillis = playerPreferences.seekIncrement.seconds.inWholeMilliseconds,
        useLongPressGesture = playerPreferences.useLongPressControls,
        longPressSpeed = playerPreferences.longPressControlsSpeed,
    )
    val seekGestureState = rememberSeekGestureState(
        player = player,
        sensitivity = playerPreferences.seekSensitivity,
        enableSeekGesture = playerPreferences.useSeekControls,
    )
    val pictureInPictureState = if (!isPreview) {
        rememberPictureInPictureState(
            player = player,
            autoEnter = playerPreferences.autoPip,
        )
    } else {
        null
    }
    val videoZoomAndContentScaleState = rememberVideoZoomAndContentScaleState(
        player = player,
        initialContentScale = api?.videoContentScale ?: playerPreferences.playerVideoZoom,
        enableZoomGesture = playerPreferences.useZoomControls,
        enablePanGesture = playerPreferences.enablePanGesture,
        onEvent = { onAction(PlayerAction.OnVideoZoomEvent(event = it)) },
    )
    val brightnessState = if (!isPreview) rememberBrightnessState() else null
    val volumeAndBrightnessGestureState = if (volumeState != null && brightnessState != null) {
        rememberVolumeAndBrightnessGestureState(
            volumeState = volumeState,
            brightnessState = brightnessState,
            enableVolumeGesture = playerPreferences.enableVolumeSwipeGesture,
            enableBrightnessGesture = playerPreferences.enableBrightnessSwipeGesture,
            volumeGestureSensitivity = playerPreferences.volumeGestureSensitivity,
            brightnessGestureSensitivity = playerPreferences.brightnessGestureSensitivity,
        )
    } else {
        null
    }
    if (!isPreview) {
        PlayerOrientationEffect(
            player = player,
            screenOrientation = playerPreferences.playerScreenOrientation,
            orientationOverride = api?.orientation,
        )
    }
    val errorState = rememberErrorState(player = player)
    val cuesState = rememberCuesState(player = player)
    val subtitlePositionState = rememberSubtitlePositionState(
        savedOffsetY = playerPreferences.subtitleOffsetY,
        onPositionSaved = { onAction(PlayerAction.UpdateSubtitleOffset(it)) },
    )
    val hapticFeedback = LocalHapticFeedback.current
    var wasDraggingSubtitle by remember { mutableStateOf(false) }
    LaunchedEffect(subtitlePositionState.isDragging) {
        if (subtitlePositionState.isDragging) {
            wasDraggingSubtitle = true
            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
            controlsVisibilityState.hideControls()
        } else if (wasDraggingSubtitle) {
            wasDraggingSubtitle = false
            controlsVisibilityState.showControls()
        }
    }

    val windowInfo = LocalWindowInfo.current
    LaunchedEffect(windowInfo.isWindowFocused) {
        if (!windowInfo.isWindowFocused && player.isPlaying) {
            player.pause()
        }
    }

    LaunchedEffect(pictureInPictureState?.isInPictureInPictureMode) {
        if (pictureInPictureState?.isInPictureInPictureMode == true) {
            controlsVisibilityState.hideControls()
        }
    }
    LaunchedEffect(tapGestureState.isLongPressGestureInAction) {
        if (tapGestureState.isLongPressGestureInAction) controlsVisibilityState.hideControls()
    }
    if (brightnessState != null) {
        LifecycleEventEffect(Lifecycle.Event.ON_START) {
            if (playerPreferences.rememberPlayerBrightness) {
                brightnessState.setBrightness(playerPreferences.playerBrightness)
            }
        }
        LaunchedEffect(brightnessState.currentBrightness) {
            if (playerPreferences.rememberPlayerBrightness) {
                onAction(PlayerAction.UpdatePlayerBrightness(brightnessState.currentBrightness))
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (pictureInPictureState != null) {
            PlayerContentFrame(
                player = player,
                pictureInPictureState = pictureInPictureState,
                videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                videoVisible = api?.videoVisible ?: true,
                displayAspectRatio = api?.displayAspectRatio,
                cuesState = cuesState,
                subtitlePositionState = subtitlePositionState,
                subtitleConfiguration = SubtitleConfiguration(
                    useSystemCaptionStyle = playerPreferences.useSystemCaptionStyle,
                    showBackground = playerPreferences.subtitleBackground,
                    font = playerPreferences.subtitleFont,
                    textSize = playerPreferences.subtitleTextSize,
                    textBold = playerPreferences.subtitleTextBold,
                    applyEmbeddedStyles = playerPreferences.applyEmbeddedStyles,
                    useMaterialYouControls = playerPreferences.useMaterialYouControls,
                    controlsVisible = controlsVisibilityState.controlsVisible &&
                        !controlsVisibilityState.controlsLocked,
                    textColor = playerPreferences.subtitleTextColor,
                    shadowColor = playerPreferences.subtitleShadowColor,
                    shadowRadius = playerPreferences.subtitleShadowRadius,
                    externalFontUri = playerPreferences.subtitleExternalFontUri,
                    backgroundOpacity = playerPreferences.subtitleBackgroundOpacity,
                ),
            )
            if (volumeAndBrightnessGestureState != null) {
                PlayerGestures(
                    controlsVisibilityState = controlsVisibilityState,
                    tapGestureState = tapGestureState,
                    pictureInPictureState = pictureInPictureState,
                    seekGestureState = seekGestureState,
                    videoZoomAndContentScaleState = videoZoomAndContentScaleState,
                    volumeAndBrightnessGestureState = volumeAndBrightnessGestureState,
                    subtitlePositionState = subtitlePositionState,
                    hasSubtitles = cuesState.currentCues?.cues?.isNotEmpty() == true,
                    canRepositionSubtitles = cuesState.canReposition,
                )
            }
        }
        MediaPlayerControls(
            player = player,
            state = state,
            onAction = onAction,
            controlsVisibilityState = controlsVisibilityState,
            tapGestureState = tapGestureState,
            seekGestureState = seekGestureState,
            videoZoomAndContentScaleState = videoZoomAndContentScaleState,
            dpadUpDown = api?.dpadUpDown,
            isPipSupported = pictureInPictureState?.isPipSupported == true,
            onPictureInPictureClick = {
                pictureInPictureState?.let {
                    if (!it.hasPipPermission) {
                        Toast.makeText(
                            context,
                            R.string.enable_pip_from_settings,
                            Toast.LENGTH_SHORT,
                        ).show()
                        it.openPictureInPictureSettings()
                    } else {
                        it.enterPictureInPictureMode()
                    }
                }
            },
            onOpenSubtitleSettingsClick = onOpenSubtitleSettingsClick,
        )
        if (volumeAndBrightnessGestureState != null && volumeState != null &&
            brightnessState != null
        ) {
            PlayerVerticalGestureIndicators(
                activeGesture = volumeAndBrightnessGestureState.activeGesture,
                volumePercentage = volumeState.volumePercentage,
                maxVolumePercentage = volumeState.maxVolumePercentage,
                brightnessPercentage = brightnessState.brightnessPercentage,
            )
        }

        val isPlayingState = rememberIsPlayingState(player = player)
        val isPipMode = pictureInPictureState?.isInPictureInPictureMode == true
        val showPlayingInfoOverlay = isPlayingState &&
            !controlsVisibilityState.controlsVisible &&
            !isPipMode

        AnimatedVisibility(
            visible = showPlayingInfoOverlay,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            PlayingPlaybackInfoOverlay(
                player = player,
                overlayTextColor = playerPreferences.playingInfoOverlayTextColor,
            )
        }
    }

    if (api?.suppressErrorMessage != true) {
        PlayerErrorDialogs(
            decoderRecoveryState = state.decoderServiceState.recoveryState,
            playbackError = errorState.playbackError,
            hasNextMediaItem = player.hasNextMediaItem(),
            onTryDecoderFallback = { onAction(PlayerAction.TryDecoderFallback) },
            onPlayNextVideo = {
                errorState.dismiss()
                player.seekToNext()
                player.play()
            },
            onExit = {
                errorState.dismiss()
                onAction(PlayerAction.NavigateUp)
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Preview(
    name = "Landscape",
    widthDp = 960,
    heightDp = 540,
)
@Preview(
    name = "Portrait",
    widthDp = 540,
    heightDp = 960,
)
@Composable
private fun MediaPlayerContentPreview() {
    NextPlayerTheme(darkTheme = true) {
        MediaPlayerContent(
            player = rememberPreviewPlayer(),
            state = PlayerUiState(),
            onAction = {},
        )
    }
}

@Composable
private fun rememberIsPlayingState(player: Player?): Boolean {
    var isPlaying by remember(key1 = player) { mutableStateOf(value = player?.isPlaying == true) }
    DisposableEffect(key1 = player) {
        if (player == null) return@DisposableEffect onDispose {}
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChanged: Boolean) {
                isPlaying = isPlayingChanged
            }
        }
        player.addListener(listener)
        isPlaying = player.isPlaying
        onDispose {
            player.removeListener(listener)
        }
    }
    return isPlaying
}

@OptIn(UnstableApi::class)
@Composable
private fun PlayingPlaybackInfoOverlay(
    player: Player,
    overlayTextColor: Int,
    modifier: Modifier = Modifier,
) {
    val progressState = rememberProgressStateWithTickInterval(player = player)
    val context = LocalContext.current

    var currentTimeText by remember { mutableStateOf(value = "") }
    LaunchedEffect(key1 = Unit) {
        val formatter = SimpleDateFormat(
            "HH:mm",
            Locale.getDefault(),
        )
        while (isActive) {
            currentTimeText = formatter.format(Date())
            delay(timeMillis = 1000L)
        }
    }

    var batteryPercentage by remember { mutableIntStateOf(value = -1) }
    var isCharging by remember { mutableStateOf(value = false) }
    DisposableEffect(key1 = context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(
                    BatteryManager.EXTRA_LEVEL,
                    -1,
                ) ?: -1
                val scale = intent?.getIntExtra(
                    BatteryManager.EXTRA_SCALE,
                    -1,
                ) ?: -1
                val status = intent?.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    -1,
                ) ?: -1
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
                if (level >= 0 && scale > 0) {
                    batteryPercentage = (level * 100) / scale
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(
            receiver,
            filter,
        )
        onDispose {
            runCatching { context.unregisterReceiver(receiver) }
        }
    }

    val elapsedText = remember(key1 = progressState.currentPositionMs) {
        val wholeSeconds = progressState.currentPositionMs / 1000L
        getStringForTime(wholeSeconds * 1000L)
    }

    val remainingText = remember(
        key1 = progressState.currentPositionMs,
        key2 = progressState.durationMs,
    ) {
        if (progressState.durationMs != C.TIME_UNSET) {
            val remainingMs =
                (progressState.durationMs - progressState.currentPositionMs).coerceAtLeast(0L)
            val wholeSeconds = remainingMs / 1000L
            getStringForTime(wholeSeconds * 1000L)
        } else {
            ""
        }
    }

    val isPlayingState = rememberIsPlayingState(player = player)
    val playbackIcon = remember(
        key1 = progressState.currentPositionMs,
        key2 = progressState.durationMs,
        key3 = isPlayingState,
    ) {
        getPlaybackIcon(
            currentPosition = progressState.currentPositionMs,
            duration = progressState.durationMs,
            isPlaying = isPlayingState,
        )
    }

    val textColor = Color(overlayTextColor)
    val overlayTextStyle = MaterialTheme.typography.labelMedium.copy(
        fontWeight = FontWeight.Bold,
        shadow = Shadow(
            color = Color.Black,
            offset = Offset(
                x = 2f,
                y = 2f,
            ),
            blurRadius = 4f,
        ),
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = 18.dp,
                vertical = 1.dp,
            ),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Bottom-left: Elapsed time & Remaining time
        Surface(
            color = PlayerButtonBlackAlpha,
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .offset(y = (-0.5).dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = playbackIcon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = textColor,
                        )
                    }
                    Text(
                        text = elapsedText,
                        style = overlayTextStyle,
                        color = textColor,
                    )
                }
                if (remainingText.isNotEmpty()) {
                    Text(
                        text = "/",
                        style = overlayTextStyle,
                        color = textColor.copy(alpha = 0.6f),
                    )
                    Text(
                        text = remainingText,
                        style = overlayTextStyle,
                        color = textColor,
                    )
                }
            }
        }

        // Bottom-right: Current clock & Battery percentage
        Surface(
            color = PlayerButtonBlackAlpha,
            shape = RoundedCornerShape(8.dp),
        ) {
            Row(
                modifier = Modifier.padding(
                    horizontal = 10.dp,
                    vertical = 4.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentTimeText.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(y = (-0.5).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Sharp.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = textColor,
                            )
                        }
                        Text(
                            text = currentTimeText,
                            style = overlayTextStyle,
                            color = textColor,
                        )
                    }
                }
                if (batteryPercentage >= 0) {
                    if (currentTimeText.isNotEmpty()) {
                        Box(modifier = Modifier.size(5.dp))
                    }
                    val batteryIcon = remember(
                        key1 = batteryPercentage,
                        key2 = isCharging,
                    ) {
                        getBatteryIcon(
                            level = batteryPercentage,
                            isCharging = isCharging,
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "$batteryPercentage%",
                            style = overlayTextStyle,
                            color = textColor,
                        )
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .offset(y = (-0.5).dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = batteryIcon,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = textColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getBatteryIcon(level: Int, isCharging: Boolean): ImageVector {
    if (isCharging) return Icons.Sharp.BatteryChargingFull
    return when {
        level <= 5 -> Icons.Sharp.Battery0Bar
        level <= 15 -> Icons.Sharp.Battery1Bar
        level <= 30 -> Icons.Sharp.Battery2Bar
        level <= 45 -> Icons.Sharp.Battery3Bar
        level <= 60 -> Icons.Sharp.Battery4Bar
        level <= 75 -> Icons.Sharp.Battery5Bar
        level <= 90 -> Icons.Sharp.Battery6Bar
        else -> Icons.Sharp.BatteryFull
    }
}

private fun getPlaybackIcon(
    currentPosition: Long,
    duration: Long,
    isPlaying: Boolean,
): ImageVector {
    if (!isPlaying) return Icons.Sharp.HourglassDisabled
    if (duration <= 0L) return Icons.Sharp.HourglassTop
    val progress = (currentPosition.toDouble() / duration * 100)
        .toInt()
        .coerceIn(
            0,
            100,
        )
    return when (progress) {
        in 0..25 -> Icons.Sharp.HourglassTop
        in 26..50 -> Icons.Sharp.HourglassFull
        in 51..99 -> Icons.Sharp.HourglassBottom
        else -> Icons.Sharp.HourglassEmpty
    }
}
