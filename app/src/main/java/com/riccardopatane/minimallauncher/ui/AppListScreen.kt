package com.riccardopatane.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.ui.components.AppRow
import com.riccardopatane.minimallauncher.ui.components.SearchField
import com.riccardopatane.minimallauncher.ui.theme.Black
import com.riccardopatane.minimallauncher.ui.theme.Gray
import com.riccardopatane.minimallauncher.ui.theme.monoStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Separate app list screen: search on top (keyboard active on entry),
 * alphabetical list below. Scrolling the list hides the keyboard; returning
 * to the top brings it back.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppListScreen(viewModel: LauncherViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    var searchFocused by remember { mutableStateOf(false) }
    var keyboardShown by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val screenHeightDp = LocalConfiguration.current.screenHeightDp
    val backThresholdPx = with(density) { (screenHeightDp * 0.15f).dp.toPx() }

    // on entry: focus the field and show the keyboard
    LaunchedEffect(Unit) {
        delay(150)
        focusRequester.requestFocus()
        keyboard?.show()
        keyboardShown = true
    }

    // list scroll → keyboard away; at the top → keyboard up
    LaunchedEffect(listState) {
        snapshotFlow {
            !listState.isScrollInProgress to listState.firstVisibleItemIndex
        }.collect { (settled, first) ->
            if (!settled) return@collect
            if (first == 0 && !keyboardShown) {
                focusRequester.requestFocus()
                withTimeoutOrNull(1_000) {
                    while (!searchFocused) delay(50)
                }
                keyboard?.show()
                keyboardShown = true
            } else if (first > 0 && keyboardShown) {
                focusManager.clearFocus()
                keyboard?.hide()
                keyboardShown = false
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .pointerInput(backThresholdPx) {
                var accumulatedY = 0f
                detectVerticalDragGestures(
                    onDragStart = { accumulatedY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        // no consume(): the list must scroll normally
                        accumulatedY += dragAmount
                    },
                    onDragEnd = {
                        // at the top of the list, an upward gesture (positive
                        // dragAmount — sign verified on the device) → main page.
                        // Below the top, the scrollable consumes the drag and
                        // this detector cancels itself.
                        val atTop = listState.firstVisibleItemIndex == 0 &&
                            listState.firstVisibleItemScrollOffset == 0
                        if (atTop && accumulatedY >= backThresholdPx) {
                            onBack()
                        }
                    },
                )
            },
    ) {
        SearchField(
            query = state.query,
            onQueryChange = viewModel::setQuery,
            onImeSearch = viewModel::onSearchSubmitted,
            focusRequester = focusRequester,
            onFocusChanged = { searchFocused = it },
            modifier = Modifier.padding(top = 8.dp),
        )
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            when {
                state.query.isBlank() ->
                    items(state.allApps, key = { it.packageName }) { entry ->
                        AppRow(entry.label) { viewModel.launch(entry) }
                    }
                state.filteredApps.isEmpty() ->
                    item(key = "no_results") {
                        BasicText(
                            stringResource(R.string.no_results),
                            style = monoStyle(16.sp, Gray),
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                        )
                    }
                else ->
                    items(state.filteredApps, key = { it.packageName }) { entry ->
                        AppRow(entry.label) { viewModel.launch(entry) }
                    }
            }
        }
    }
}
