package com.riccardopatane.minimallauncher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.ui.components.SearchField
import com.riccardopatane.minimallauncher.ui.theme.Black
import com.riccardopatane.minimallauncher.ui.theme.Gray
import com.riccardopatane.minimallauncher.ui.theme.White
import com.riccardopatane.minimallauncher.ui.theme.monoStyle

/** Dedicated favorites management screen, with its own search. */
@Composable
fun FavoritesScreen(viewModel: LauncherViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var pickerQuery by remember { mutableStateOf("") }

    val filtered = if (pickerQuery.isBlank()) state.allApps
    else state.allApps.filter { it.label.contains(pickerQuery, ignoreCase = true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
    ) {
        BasicText(
            stringResource(R.string.settings_back),
            style = monoStyle(18.sp, Gray),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack)
                .padding(horizontal = 20.dp, vertical = 12.dp),
        )
        BasicText(
            stringResource(R.string.settings_favorites),
            style = monoStyle(32.sp),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        SearchField(
            query = pickerQuery,
            onQueryChange = { pickerQuery = it },
            onImeSearch = {},
            focusRequester = remember { FocusRequester() },
            onFocusChanged = {},
            modifier = Modifier.padding(top = 4.dp),
        )
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            items(filtered, key = { it.packageName }) { entry ->
                val isFav = state.favorites.any { it.packageName == entry.packageName }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleFavorite(entry) }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    // pinned = white, otherwise gray
                    BasicText(
                        entry.label,
                        style = monoStyle(20.sp, if (isFav) White else Gray),
                    )
                }
            }
        }
    }
}
