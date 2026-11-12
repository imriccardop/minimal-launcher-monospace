package com.riccardopatane.minimallauncher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.ui.theme.LocalLauncherColors
import com.riccardopatane.minimallauncher.ui.theme.launcherStyle

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onImeSearch: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalLauncherColors.current
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = launcherStyle(20.sp, colors.text),
        cursorBrush = SolidColor(colors.text),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    BasicText(stringResource(R.string.search_hint), style = launcherStyle(20.sp, colors.secondary))
                }
                inner()
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
    )
}
