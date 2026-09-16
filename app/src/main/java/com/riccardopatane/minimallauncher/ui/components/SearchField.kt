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
import com.riccardopatane.minimallauncher.ui.theme.Gray
import com.riccardopatane.minimallauncher.ui.theme.White
import com.riccardopatane.minimallauncher.ui.theme.monoStyle

@Composable
fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onImeSearch: () -> Unit,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = monoStyle(20.sp),
        cursorBrush = SolidColor(White),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onImeSearch() }),
        decorationBox = { inner ->
            Box {
                if (query.isEmpty()) {
                    BasicText(stringResource(R.string.search_hint), style = monoStyle(20.sp, Gray))
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
