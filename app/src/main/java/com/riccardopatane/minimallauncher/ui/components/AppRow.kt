package com.riccardopatane.minimallauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopatane.minimallauncher.ui.theme.monoStyle

@Composable
fun AppRow(label: String, marker: String = "", modifier: Modifier = Modifier, onClick: () -> Unit) {
    BasicText(
        text = if (marker.isEmpty()) label else "$marker $label",
        style = monoStyle(20.sp),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
    )
}
