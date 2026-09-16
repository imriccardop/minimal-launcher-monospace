package com.riccardopatane.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.model.FavoriteMarker
import com.riccardopatane.minimallauncher.model.FavoritePosition
import com.riccardopatane.minimallauncher.model.toSuperscript
import com.riccardopatane.minimallauncher.ui.components.AppRow
import com.riccardopatane.minimallauncher.ui.components.HeaderBlock
import com.riccardopatane.minimallauncher.ui.theme.Black
import com.riccardopatane.minimallauncher.ui.theme.Gray
import com.riccardopatane.minimallauncher.ui.theme.monoStyle
import com.riccardopatane.minimallauncher.util.LaunchIntents

/**
 * Main page: header + favorites, no scrolling. The app list is a separate
 * screen, reached with the downward gesture.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenSettings: () -> Unit,
    onOpenAppList: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val now by viewModel.nowFlow.collectAsStateWithLifecycle()
    val battery by viewModel.batteryFlow.collectAsStateWithLifecycle()
    val usage by viewModel.usageInfo.collectAsStateWithLifecycle()
    val notifCount by viewModel.notificationsTodayFlow.collectAsStateWithLifecycle()
    val notifAccess by viewModel.notifAccessFlow.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    // horizontal threshold (phone/camera): 30% of the width
    val swipeThresholdPx = with(density) { (configuration.screenWidthDp * 0.30f).dp.toPx() }
    // vertical threshold (opens the app list): 22% of the height
    val openListThresholdPx = with(density) { (configuration.screenHeightDp * 0.22f).dp.toPx() }
    val haptics = LocalHapticFeedback.current

    Box(
        Modifier
            .fillMaxSize()
            .background(Black)
            .pointerInput(swipeThresholdPx) {
                var accumulated = 0f
                detectHorizontalDragGestures(
                    onDragStart = { accumulated = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        accumulated += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        when {
                            // left swipe → phone
                            accumulated <= -swipeThresholdPx -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                LaunchIntents.openDialer(context)
                            }
                            // right swipe → camera
                            accumulated >= swipeThresholdPx -> {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                LaunchIntents.openCamera(context)
                            }
                        }
                    },
                )
            }
            .pointerInput(openListThresholdPx) {
                var accumulatedY = 0f
                detectVerticalDragGestures(
                    onDragStart = {
                        accumulatedY = 0f
                        android.util.Log.w("LauncherDebug", "vdrag start")
                    },
                    onVerticalDrag = { change, dragAmount ->
                        accumulatedY += dragAmount
                        change.consume()
                    },
                    onDragEnd = {
                        // NB: dragAmount is NEGATIVE when the finger moves down;
                        // downward gesture → app list
                        if (accumulatedY <= -openListThresholdPx) {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenAppList()
                        }
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { onOpenSettings() })
            },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
        ) {
            HeaderBlock(
                now = now,
                battery = battery,
                usage = usage,
                use24h = state.use24h,
                dateFormat = state.dateFormat,
                timeSeparator = state.timeSeparator,
                showUnlocks = state.showUnlocks,
                showNotifications = state.showNotifications,
                notificationsAccess = notifAccess,
                notificationCount = notifCount,
            )
            if (state.favoritePosition == FavoritePosition.BOTTOM) {
                Spacer(Modifier.weight(1f))
            }
            Column(Modifier.padding(bottom = 24.dp)) {
                if (state.favorites.isEmpty()) {
                    BasicText(
                        stringResource(R.string.favorites_empty_hint),
                        style = monoStyle(16.sp, Gray),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                } else {
                    // optional marker before the favorites. For numbers:
                    // BOTTOM position decreasing (the lowest = 0), TOP
                    // position increasing (the first = 0)
                    state.favorites.forEachIndexed { index, entry ->
                        val marker = when (state.favoriteMarker) {
                            FavoriteMarker.SUPERSCRIPT ->
                                when (state.favoritePosition) {
                                    FavoritePosition.BOTTOM ->
                                        toSuperscript(state.favorites.size - 1 - index)
                                    FavoritePosition.TOP -> toSuperscript(index)
                                }
                            FavoriteMarker.ASTERISM -> "⁂"
                            FavoriteMarker.ARROW -> "⟶"
                            FavoriteMarker.DIAGONAL -> "↘"
                            FavoriteMarker.GEQ -> "≥"
                            FavoriteMarker.TILDE -> "〜"
                            FavoriteMarker.NONE -> ""
                        }
                        AppRow(entry.label, marker) { viewModel.launch(entry) }
                    }
                }
            }
        }
    }
}
