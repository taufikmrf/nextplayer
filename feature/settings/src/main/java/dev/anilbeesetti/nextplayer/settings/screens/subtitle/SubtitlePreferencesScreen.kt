package dev.anilbeesetti.nextplayer.settings.screens.subtitle

import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.anilbeesetti.nextplayer.core.model.Font
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.ClickablePreferenceItem
import dev.anilbeesetti.nextplayer.core.ui.components.ListSectionTitle
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSlider
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitch
import dev.anilbeesetti.nextplayer.core.ui.components.PreferenceSwitchWithDivider
import dev.anilbeesetti.nextplayer.core.ui.components.RadioTextButton
import dev.anilbeesetti.nextplayer.core.ui.components.rememberTvListFocusRequester
import dev.anilbeesetti.nextplayer.core.ui.components.tvFocusDown
import dev.anilbeesetti.nextplayer.core.ui.components.tvListFocus
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import dev.anilbeesetti.nextplayer.core.ui.theme.NextPlayerTheme
import dev.anilbeesetti.nextplayer.settings.composables.OptionsDialog
import dev.anilbeesetti.nextplayer.settings.extensions.name
import dev.anilbeesetti.nextplayer.settings.utils.LocalesHelper
import java.nio.charset.Charset

private data class SubtitleColorOption(val color: Int, val labelRes: Int)

private val SubtitleTextColorOptions = listOf(
    SubtitleColorOption(
        0xFFFFFFFF.toInt(),
        R.string.color_white,
    ),
    SubtitleColorOption(
        0xFF000000.toInt(),
        R.string.color_black,
    ),
    SubtitleColorOption(
        0xFFFFFF00.toInt(),
        R.string.color_yellow,
    ),
    SubtitleColorOption(
        0xFFFF0000.toInt(),
        R.string.color_red,
    ),
    SubtitleColorOption(
        0xFF00FF00.toInt(),
        R.string.color_green,
    ),
    SubtitleColorOption(
        0xFF00FFFF.toInt(),
        R.string.color_cyan,
    ),
    SubtitleColorOption(
        0xFF0000FF.toInt(),
        R.string.color_blue,
    ),
)

private val SubtitleShadowColorOptions = listOf(
    SubtitleColorOption(
        0,
        R.string.none,
    ),
    SubtitleColorOption(
        0xFFFFFFFF.toInt(),
        R.string.color_white,
    ),
    SubtitleColorOption(
        0xFF000000.toInt(),
        R.string.color_black,
    ),
    SubtitleColorOption(
        0xFFFFFF00.toInt(),
        R.string.color_yellow,
    ),
    SubtitleColorOption(
        0xFFFF0000.toInt(),
        R.string.color_red,
    ),
    SubtitleColorOption(
        0xFF00FF00.toInt(),
        R.string.color_green,
    ),
    SubtitleColorOption(
        0xFF00FFFF.toInt(),
        R.string.color_cyan,
    ),
    SubtitleColorOption(
        0xFF0000FF.toInt(),
        R.string.color_blue,
    ),
)

@Composable
private fun getSubtitleTextColorName(color: Int): String {
    val option = SubtitleTextColorOptions.firstOrNull { it.color == color }
    return if (option != null) {
        stringResource(id = option.labelRes)
    } else {
        String.format(
            "#%08X",
            color,
        )
    }
}

@Composable
private fun getSubtitleShadowColorName(color: Int): String {
    val option = SubtitleShadowColorOptions.firstOrNull { it.color == color }
    return if (option != null) {
        stringResource(id = option.labelRes)
    } else {
        String.format(
            "#%08X",
            color,
        )
    }
}

private fun getFileName(context: android.content.Context, uri: Uri): String {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(
            uri,
            null,
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    result = cursor.getString(nameIndex)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/')
        if (cut != null && cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result ?: "External Font"
}

@Composable
fun SubtitlePreferencesScreen(viewModel: SubtitlePreferencesViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    SubtitlePreferencesScreenContent(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(
    ExperimentalMaterial3ExpressiveApi::class,
)
@Composable
private fun SubtitlePreferencesScreenContent(
    state: SubtitlePreferencesUiState,
    onAction: (SubtitlePreferencesUiEvent) -> Unit,
) {
    val languages = remember {
        listOf(
            element = Pair(
                first = "None",
                second = "",
            ),
        ) +
            LocalesHelper.getAvailableLocales()
    }
    val charsetResource = stringArrayResource(id = R.array.charsets_list)
    val context = LocalContext.current

    val listFocusRequester = rememberTvListFocusRequester()
    Scaffold(
        topBar = {
            NextTopAppBar(
                title = stringResource(id = R.string.subtitle),
                navigationIcon = {
                    FilledTonalIconButton(
                        onClick = {
                            onAction(SubtitlePreferencesUiEvent.NavigateUp)
                        },
                        modifier = Modifier.tvFocusDown(target = listFocusRequester),
                    ) {
                        Icon(
                            imageVector = NextIcons.ArrowBack,
                            contentDescription = stringResource(id = R.string.navigate_up),
                        )
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(state = rememberScrollState())
                .tvListFocus(focusRequester = listFocusRequester)
                .padding(paddingValues = innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            ListSectionTitle(text = stringResource(id = R.string.playback))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.preferred_subtitle_lang),
                    description =
                        LocalesHelper.getLocaleDisplayLanguage(
                            state.preferences.preferredSubtitleLanguage,
                        )
                            .takeIf { it.isNotBlank() }
                            ?: stringResource(R.string.preferred_subtitle_lang_description),
                    icon = NextIcons.Language,
                    onClick = {
                        onAction(
                            SubtitlePreferencesUiEvent.ShowDialog(
                                value = SubtitlePreferenceDialog.SubtitleLanguageDialog,
                            ),
                        )
                    },
                    isFirstItem = true,
                )
                ClickablePreferenceItem(
                    title = stringResource(R.string.subtitle_text_encoding),
                    description = charsetResource.first {
                        it.contains(state.preferences.subtitleTextEncoding)
                    },
                    icon = NextIcons.Subtitle,
                    onClick = {
                        onAction(
                            SubtitlePreferencesUiEvent.ShowDialog(
                                value = SubtitlePreferenceDialog.SubtitleEncodingDialog,
                            ),
                        )
                    },
                    isLastItem = true,
                )
            }
            ListSectionTitle(text = stringResource(id = R.string.appearance_name))
            Column(
                verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
            ) {
                PreferenceSwitchWithDivider(
                    title = stringResource(R.string.system_caption_style),
                    description = stringResource(R.string.system_caption_style_desc),
                    icon = NextIcons.Caption,
                    isChecked = state.preferences.useSystemCaptionStyle,
                    onChecked = {
                        onAction(SubtitlePreferencesUiEvent.ToggleUseSystemCaptionStyle)
                    },
                    onClick = {
                        context.startActivity(Intent(Settings.ACTION_CAPTIONING_SETTINGS))
                    },
                    isFirstItem = true,
                )
                val externalUri = state.preferences.subtitleExternalFontUri
                val defaultFontName = state.preferences.subtitleFont.name()
                val fontDescription = if (externalUri.isNotBlank()) {
                    remember(externalUri) {
                        runCatching {
                            val uri = Uri.parse(externalUri)
                            getFileName(
                                context,
                                uri,
                            )
                        }.getOrNull()
                    } ?: defaultFontName
                } else {
                    defaultFontName
                }

                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_font),
                    description = fontDescription,
                    icon = NextIcons.Font,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    onClick = {
                        onAction(
                            SubtitlePreferencesUiEvent.ShowDialog(
                                value = SubtitlePreferenceDialog.SubtitleFontDialog,
                            ),
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_text_bold),
                    description = stringResource(id = R.string.subtitle_text_bold_desc),
                    icon = NextIcons.Bold,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    isChecked = state.preferences.subtitleTextBold,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleTextBold) },
                )
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_text_size),
                    description = state.preferences.subtitleTextSize.toString(),
                    icon = NextIcons.FontSize,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    value = state.preferences.subtitleTextSize.toFloat(),
                    valueRange = 10f..60f,
                    onValueChange = {
                        onAction(SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(it.toInt()))
                    },
                    trailingContent = {
                        FilledIconButton(
                            enabled = state.preferences.useSystemCaptionStyle.not(),
                            onClick = {
                                onAction(
                                    SubtitlePreferencesUiEvent.UpdateSubtitleFontSize(
                                        PlayerPreferences.DEFAULT_SUBTITLE_TEXT_SIZE,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_seek_increment,
                                ),
                            )
                        }
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_text_color),
                    description = getSubtitleTextColorName(state.preferences.subtitleTextColor),
                    icon = NextIcons.Appearance,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    onClick = {
                        onAction(
                            SubtitlePreferencesUiEvent.ShowDialog(
                                value = SubtitlePreferenceDialog.SubtitleTextColorDialog,
                            ),
                        )
                    },
                )
                PreferenceSwitch(
                    title = stringResource(id = R.string.subtitle_background),
                    description = stringResource(id = R.string.subtitle_background_desc),
                    icon = NextIcons.Background,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    isChecked = state.preferences.subtitleBackground,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleSubtitleBackground) },
                )
                val isBackgroundEnabled = state.preferences.useSystemCaptionStyle.not() &&
                    state.preferences.subtitleBackground
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_background_opacity),
                    description = "${(state.preferences.subtitleBackgroundOpacity * 100).toInt()}%",
                    icon = NextIcons.Contrast,
                    enabled = isBackgroundEnabled,
                    value = state.preferences.subtitleBackgroundOpacity,
                    valueRange = 0f..1f,
                    onValueChange = {
                        onAction(SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundOpacity(it))
                    },
                    trailingContent = {
                        FilledIconButton(
                            enabled = isBackgroundEnabled,
                            onClick = {
                                onAction(
                                    SubtitlePreferencesUiEvent.UpdateSubtitleBackgroundOpacity(
                                        PlayerPreferences.DEFAULT_SUBTITLE_BACKGROUND_OPACITY,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_seek_increment,
                                ),
                            )
                        }
                    },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.subtitle_shadow_color),
                    description = getSubtitleShadowColorName(state.preferences.subtitleShadowColor),
                    icon = NextIcons.Background,
                    enabled = state.preferences.useSystemCaptionStyle.not(),
                    onClick = {
                        onAction(
                            SubtitlePreferencesUiEvent.ShowDialog(
                                value = SubtitlePreferenceDialog.SubtitleShadowColorDialog,
                            ),
                        )
                    },
                )
                val isShadowEnabled = state.preferences.useSystemCaptionStyle.not() &&
                    state.preferences.subtitleShadowColor != 0
                PreferenceSlider(
                    title = stringResource(id = R.string.subtitle_shadow_thickness),
                    description = state.preferences.subtitleShadowRadius.toInt().toString(),
                    icon = NextIcons.FontSize,
                    enabled = isShadowEnabled,
                    value = state.preferences.subtitleShadowRadius,
                    valueRange = 0f..10f,
                    onValueChange = {
                        onAction(SubtitlePreferencesUiEvent.UpdateSubtitleShadowRadius(it))
                    },
                    trailingContent = {
                        FilledIconButton(
                            enabled = isShadowEnabled,
                            onClick = {
                                onAction(
                                    SubtitlePreferencesUiEvent.UpdateSubtitleShadowRadius(
                                        2.0f,
                                    ),
                                )
                            },
                        ) {
                            Icon(
                                imageVector = NextIcons.History,
                                contentDescription = stringResource(
                                    id = R.string.reset_seek_increment,
                                ),
                            )
                        }
                    },
                )
                PreferenceSwitch(
                    title = stringResource(R.string.embedded_styles),
                    description = stringResource(R.string.embedded_styles_desc),
                    icon = NextIcons.Style,
                    isChecked = state.preferences.applyEmbeddedStyles,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ToggleApplyEmbeddedStyles) },
                )
                ClickablePreferenceItem(
                    title = stringResource(id = R.string.reset_subtitle_position),
                    description = stringResource(id = R.string.reset_subtitle_position_desc),
                    icon = NextIcons.History,
                    enabled = state.preferences.subtitleOffsetY != 0f,
                    onClick = { onAction(SubtitlePreferencesUiEvent.ResetSubtitlePosition) },
                    isLastItem = true,
                )
            }
        }

        state.showDialog?.let { showDialog ->
            when (showDialog) {
                SubtitlePreferenceDialog.SubtitleLanguageDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.preferred_subtitle_lang),
                        onDismissClick = {
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = languages) {
                            RadioTextButton(
                                text = it.first,
                                selected = it.second == state.preferences.preferredSubtitleLanguage,
                                onClick = {
                                    onAction(
                                        SubtitlePreferencesUiEvent.UpdateSubtitleLanguage(
                                            it.second,
                                        ),
                                    )
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleFontDialog -> {
                    val fontPickerLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.OpenDocument(),
                    ) { uri ->
                        if (uri != null) {
                            try {
                                context.contentResolver.takePersistableUriPermission(
                                    uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                )
                            } catch (_: Exception) {}
                            onAction(
                                SubtitlePreferencesUiEvent.UpdateSubtitleExternalFont(
                                    uri.toString(),
                                ),
                            )
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        }
                    }

                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_font),
                        onDismissClick = {
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(Font.entries.toTypedArray()) { font ->
                            RadioTextButton(
                                text = font.name(),
                                selected = font == state.preferences.subtitleFont &&
                                    state.preferences.subtitleExternalFontUri.isBlank(),
                                onClick = {
                                    onAction(
                                        SubtitlePreferencesUiEvent.UpdateSubtitleFont(value = font),
                                    )
                                    onAction(
                                        SubtitlePreferencesUiEvent.UpdateSubtitleExternalFont(
                                            value = "",
                                        ),
                                    )
                                    onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                                },
                            )
                        }
                        item {
                            val externalUri = state.preferences.subtitleExternalFontUri
                            val isSelected = externalUri.isNotBlank()
                            val defaultExternalFontName = stringResource(
                                R.string.select_external_font,
                            )
                            val fontName = if (externalUri.isNotBlank()) {
                                remember(externalUri) {
                                    runCatching {
                                        val uri = Uri.parse(externalUri)
                                        getFileName(
                                            context,
                                            uri,
                                        )
                                    }.getOrNull()
                                } ?: defaultExternalFontName
                            } else {
                                defaultExternalFontName
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = isSelected,
                                        onClick = {
                                            if (externalUri.isBlank()) {
                                                fontPickerLauncher.launch(
                                                    arrayOf(
                                                        "font/*",
                                                        "application/x-font-*",
                                                        "application/octet-stream",
                                                        "*/*",
                                                    ),
                                                )
                                            } else {
                                                // Already selected
                                            }
                                        },
                                        role = Role.RadioButton,
                                    )
                                    .padding(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            imageVector = NextIcons.Font,
                                            contentDescription = null,
                                            modifier = Modifier.size(24.dp),
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(text = fontName)
                                    }
                                    if (externalUri.isNotBlank()) {
                                        TextButton(
                                            onClick = {
                                                fontPickerLauncher.launch(
                                                    arrayOf(
                                                        "font/*",
                                                        "application/x-font-*",
                                                        "application/octet-stream",
                                                        "*/*",
                                                    ),
                                                )
                                            },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.padding(start = 36.dp),
                                        ) {
                                            Text(text = stringResource(R.string.change_font))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleEncodingDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_encoding),
                        onDismissClick = {
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = charsetResource) {
                            val currentCharset = it.substringAfterLast(
                                delimiter = "(",
                                missingDelimiterValue = "",
                            ).removeSuffix(suffix = ")")
                            if (currentCharset.isEmpty() || Charset.isSupported(currentCharset)) {
                                RadioTextButton(
                                    text = it,
                                    selected =
                                        currentCharset == state.preferences.subtitleTextEncoding,
                                    onClick = {
                                        onAction(
                                            SubtitlePreferencesUiEvent.UpdateSubtitleEncoding(
                                                value = currentCharset,
                                            ),
                                        )
                                        onAction(
                                            SubtitlePreferencesUiEvent.ShowDialog(value = null),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleTextColorDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_text_color),
                        onDismissClick = {
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = SubtitleTextColorOptions) { option ->
                            RadioColorButton(
                                color = option.color,
                                text = stringResource(id = option.labelRes),
                                selected = option.color == state.preferences.subtitleTextColor,
                                onClick = {
                                    onAction(
                                        SubtitlePreferencesUiEvent.UpdateSubtitleTextColor(
                                            value = option.color,
                                        ),
                                    )
                                    onAction(
                                        SubtitlePreferencesUiEvent.ShowDialog(value = null),
                                    )
                                },
                            )
                        }
                    }
                }

                SubtitlePreferenceDialog.SubtitleShadowColorDialog -> {
                    OptionsDialog(
                        text = stringResource(id = R.string.subtitle_shadow_color),
                        onDismissClick = {
                            onAction(SubtitlePreferencesUiEvent.ShowDialog(value = null))
                        },
                    ) {
                        items(items = SubtitleShadowColorOptions) { option ->
                            RadioColorButton(
                                color = option.color,
                                text = stringResource(id = option.labelRes),
                                selected = option.color == state.preferences.subtitleShadowColor,
                                onClick = {
                                    onAction(
                                        SubtitlePreferencesUiEvent.UpdateSubtitleShadowColor(
                                            value = option.color,
                                        ),
                                    )
                                    onAction(
                                        SubtitlePreferencesUiEvent.ShowDialog(value = null),
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RadioColorButton(
    color: Int,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .size(20.dp)
                .background(
                    color = if (color == 0) Color.Transparent else Color(color),
                    shape = CircleShape,
                )
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (color == 0) {
                Icon(
                    imageVector = NextIcons.Close,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = text)
    }
}

@PreviewLightDark
@Composable
private fun SubtitlePreferencesScreenPreview() {
    NextPlayerTheme {
        SubtitlePreferencesScreenContent(
            state = SubtitlePreferencesUiState(),
            onAction = {},
        )
    }
}
