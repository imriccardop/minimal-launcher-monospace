package com.riccardopatane.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.model.CarsonMode
import com.riccardopatane.minimallauncher.model.CrtGrid
import com.riccardopatane.minimallauncher.model.DateFormat
import com.riccardopatane.minimallauncher.model.FavoriteMarker
import com.riccardopatane.minimallauncher.model.FavoritePosition
import com.riccardopatane.minimallauncher.model.ThemeLevel
import com.riccardopatane.minimallauncher.model.TimeSeparator
import com.riccardopatane.minimallauncher.ui.theme.LocalLauncherColors
import com.riccardopatane.minimallauncher.ui.theme.launcherStyle
import com.riccardopatane.minimallauncher.util.LaunchIntents
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
    onOpenFavorites: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val usage by viewModel.usageInfo.collectAsStateWithLifecycle()
    val notifAccess by viewModel.notifAccessFlow.collectAsStateWithLifecycle()
    val now by viewModel.nowFlow.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val colors = LocalLauncherColors.current
    val instant = remember(now) { Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()) }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "back") {
            BasicText(
                stringResource(R.string.settings_back),
                style = launcherStyle(18.sp, colors.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onBack)
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        item(key = "title") {
            BasicText(
                stringResource(R.string.settings_title),
                style = launcherStyle(32.sp, colors.text),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        // favorites management lives in a dedicated screen: here just the
        // entry point with the count
        item(key = "favorites_row") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenFavorites)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                BasicText(
                    stringResource(R.string.settings_favorites),
                    style = launcherStyle(20.sp, colors.text),
                )
                BasicText(
                    "  ${state.favorites.size} →",
                    style = launcherStyle(20.sp, colors.secondary),
                )
            }
        }
        item(key = "sec_favpos") {
            SectionLabel(stringResource(R.string.settings_favorites_position))
        }
        FavoritePosition.entries.forEach { position ->
            item(key = "favpos_${position.name}") {
                ChoiceRow(
                    label = position.label,
                    selected = position == state.favoritePosition,
                    onClick = { viewModel.setFavoritePosition(position) },
                )
            }
        }
        item(key = "sec_marker") {
            SectionLabel(stringResource(R.string.settings_favorites_marker))
        }
        FavoriteMarker.entries.forEach { marker ->
            item(key = "marker_${marker.name}") {
                ChoiceRow(
                    label = marker.label,
                    selected = marker == state.favoriteMarker,
                    onClick = { viewModel.setFavoriteMarker(marker) },
                )
            }
        }
        item(key = "sec_time") {
            SectionLabel(stringResource(R.string.settings_time_format))
        }
        item(key = "time") {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
                BasicText(
                    stringResource(R.string.settings_time_24h),
                    style = launcherStyle(20.sp, if (state.use24h) colors.text else colors.secondary),
                    modifier = Modifier
                        .clickable { viewModel.setUse24h(true) }
                        .padding(end = 20.dp)
                        .padding(vertical = 4.dp),
                )
                BasicText(
                    stringResource(R.string.settings_time_12h),
                    style = launcherStyle(20.sp, if (!state.use24h) colors.text else colors.secondary),
                    modifier = Modifier
                        .clickable { viewModel.setUse24h(false) }
                        .padding(vertical = 4.dp),
                )
            }
        }
        item(key = "sec_date") {
            SectionLabel(stringResource(R.string.settings_date_format))
        }
        DateFormat.entries.forEach { format ->
            item(key = "date_${format.name}") {
                // live label: the format applied to today's date
                val label = remember(format, instant) {
                    DateTimeFormatter.ofPattern(format.pattern, Locale.ENGLISH).format(instant)
                }
                ChoiceRow(
                    label = label,
                    selected = format == state.dateFormat,
                    onClick = { viewModel.setDateFormat(format) },
                )
            }
        }
        item(key = "sec_separator") {
            SectionLabel(stringResource(R.string.settings_time_separator))
        }
        TimeSeparator.entries.forEach { separator ->
            item(key = "separator_${separator.name}") {
                ChoiceRow(
                    label = separator.label,
                    selected = separator == state.timeSeparator,
                    onClick = { viewModel.setTimeSeparator(separator) },
                )
            }
        }
        item(key = "sec_homeinfo") {
            SectionLabel(stringResource(R.string.settings_home_info))
        }
        item(key = "show_unlocks") {
            ChoiceRow(
                label = stringResource(R.string.settings_show_unlocks),
                selected = state.showUnlocks,
                onClick = { viewModel.setShowUnlocks(!state.showUnlocks) },
            )
        }
        item(key = "show_notifications") {
            ChoiceRow(
                label = stringResource(R.string.settings_show_notifications),
                selected = state.showNotifications,
                onClick = { viewModel.setShowNotifications(!state.showNotifications) },
            )
        }
        item(key = "sec_look") {
            SectionLabel(stringResource(R.string.settings_look))
        }
        ThemeLevel.entries.forEach { level ->
            item(key = "look_${level.name}") {
                ChoiceRow(
                    label = level.label,
                    selected = level == state.themeLevel,
                    onClick = { viewModel.setThemeLevel(level) },
                )
            }
        }
        item(key = "sec_crt") {
            SectionLabel(stringResource(R.string.settings_crt))
        }
        item(key = "crt_glow_none") {
            ChoiceRow(
                label = stringResource(R.string.settings_crt_glow_none),
                selected = !state.crtGlow,
                onClick = { viewModel.setCrtGlow(false) },
            )
        }
        item(key = "crt_glow_light") {
            ChoiceRow(
                label = stringResource(R.string.settings_crt_glow_light),
                selected = state.crtGlow,
                onClick = { viewModel.setCrtGlow(true) },
            )
        }
        CrtGrid.entries.forEach { grid ->
            item(key = "crt_grid_${grid.name}") {
                ChoiceRow(
                    label = grid.label,
                    selected = grid == state.crtGrid,
                    onClick = { viewModel.setCrtGrid(grid) },
                )
            }
        }
        item(key = "sec_typography") {
            SectionLabel(stringResource(R.string.settings_typography))
        }
        CarsonMode.entries.forEach { mode ->
            item(key = "carson_${mode.name}") {
                ChoiceRow(
                    label = mode.label,
                    selected = mode == state.carsonMode,
                    onClick = { viewModel.setCarsonMode(mode) },
                )
            }
        }
        item(key = "default_launcher") {
            BasicText(
                stringResource(R.string.settings_default_launcher),
                style = launcherStyle(20.sp, colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        context.startActivity(LaunchIntents.defaultHomeIntent(context))
                    }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        item(key = "black_wallpaper") {
            BasicText(
                stringResource(R.string.settings_black_wallpaper),
                style = launcherStyle(20.sp, colors.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.applyBlackWallpaper() }
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )
        }
        if (!usage.granted) {
            item(key = "usage_grant") {
                BasicText(
                    stringResource(R.string.settings_usage_grant),
                    style = launcherStyle(20.sp, colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { context.startActivity(LaunchIntents.usageAccessIntent(context)) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        if (!notifAccess) {
            item(key = "notif_grant") {
                BasicText(
                    stringResource(R.string.settings_notif_grant),
                    style = launcherStyle(20.sp, colors.text),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            context.startActivity(LaunchIntents.notificationAccessIntent(context))
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                )
            }
        }
        item(key = "sec_about") {
            SectionLabel(stringResource(R.string.settings_about))
        }
        item(key = "font_credit") {
            // the full license stays in the bundle (assets/OFL.txt);
            // here just the attribution line
            BasicText(
                stringResource(R.string.settings_font_credit),
                style = launcherStyle(14.sp, colors.secondary),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        item(key = "designer_credit") {
            BasicText(
                stringResource(R.string.settings_designer_credit),
                style = launcherStyle(14.sp, colors.secondary),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        LaunchIntents.openUrl(context, "https://github.com/imriccardop")
                    }
                    .padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
    }
}

/** Single-choice row: the only difference is the color (white/gray). */
@Composable
private fun ChoiceRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    ) {
        BasicText(
            label,
            style = launcherStyle(20.sp, if (selected) LocalLauncherColors.current.text else LocalLauncherColors.current.secondary),
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    BasicText(
        "⁂ $text",
        style = launcherStyle(24.sp, LocalLauncherColors.current.text),
        modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 4.dp),
    )
}
