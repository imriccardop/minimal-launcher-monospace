package com.riccardopatane.minimallauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.riccardopatane.minimallauncher.model.CarsonMode
import com.riccardopatane.minimallauncher.model.DateFormat
import com.riccardopatane.minimallauncher.model.TimeSeparator
import com.riccardopatane.minimallauncher.ui.UsageInfo
import com.riccardopatane.minimallauncher.ui.carson.CarsonText
import com.riccardopatane.minimallauncher.ui.carson.carsonSeed
import com.riccardopatane.minimallauncher.ui.carson.computeLinePlan
import com.riccardopatane.minimallauncher.util.LaunchIntents
import com.riccardopatane.minimallauncher.ui.theme.LocalLauncherColors
import com.riccardopatane.minimallauncher.ui.theme.launcherStyle
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HeaderBlock(
    now: Long,
    battery: Int,
    usage: UsageInfo,
    use24h: Boolean,
    dateFormat: DateFormat,
    timeSeparator: TimeSeparator,
    showUnlocks: Boolean,
    showNotifications: Boolean,
    notificationsAccess: Boolean,
    notificationCount: Int,
    carsonMode: CarsonMode = CarsonMode.OFF,
    carsonEpoch: Long = 0L,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val colors = LocalLauncherColors.current
    val clockFormat = remember(use24h, timeSeparator) {
        // "mm" = minutes always two digits (zero-padded: 23:01, not 23:1)
        DateTimeFormatter.ofPattern(
            if (use24h) "HH${timeSeparator.patternChar}mm" else "h${timeSeparator.patternChar}mm",
        )
    }
    val dateFormatter = remember(dateFormat) {
        DateTimeFormatter.ofPattern(dateFormat.pattern, Locale.ENGLISH)
    }
    val instant = remember(now) { Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()) }

    Column(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(start = 20.dp, end = 20.dp, top = 36.dp, bottom = 12.dp),
    ) {
        // two top-aligned columns: time+date (left) and battery+usage (right),
        // each pair with tight line spacing (1.05). Tap on time/date → Clock.
        Row(Modifier.fillMaxWidth()) {
            Column(
                Modifier
                    .weight(1f)
                    .clickable { LaunchIntents.openClock(context) },
            ) {
                HeaderLine(
                    text = clockFormat.format(instant),
                    carsonMode = carsonMode,
                    carsonEpoch = carsonEpoch,
                    index = 0,
                    baseSizeSp = 56f,
                    color = colors.secondary,
                )
                HeaderLine(
                    text = dateFormatter.format(instant),
                    carsonMode = carsonMode,
                    carsonEpoch = carsonEpoch,
                    index = 1,
                    baseSizeSp = 18f,
                    color = colors.secondary,
                )
                // reserved weather slot (v2: symbol + temperature here)
                Spacer(Modifier.height(24.dp))
            }
            Column(horizontalAlignment = Alignment.End) {
                HeaderLine(
                    text = if (battery >= 0) "$battery%" else "--",
                    carsonMode = carsonMode,
                    carsonEpoch = carsonEpoch,
                    index = 2,
                    baseSizeSp = 18f,
                    color = colors.secondary,
                    modifier = Modifier.padding(top = 16.dp),
                )
                // tap on usage/unlocks/notifications → Digital Wellbeing.
                // Glyphs to the right of the number, no separator:
                // minutes≠, unlocks≡, notifications≢
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier
                        .clickable { LaunchIntents.openDigitalWellbeing(context) },
                ) {
                    HeaderLine(
                        text = usageText(usage),
                        carsonMode = carsonMode,
                        carsonEpoch = carsonEpoch,
                        index = 3,
                        baseSizeSp = 18f,
                        color = colors.secondary,
                    )
                    if (usage.granted && showUnlocks) {
                        HeaderLine(
                            text = "${usage.unlocks}≡",
                            carsonMode = carsonMode,
                            carsonEpoch = carsonEpoch,
                            index = 4,
                            baseSizeSp = 18f,
                            color = colors.secondary,
                        )
                    }
                    // the number is shown only with the permission active
                    if (showNotifications && notificationsAccess) {
                        HeaderLine(
                            text = "$notificationCount≢",
                            carsonMode = carsonMode,
                            carsonEpoch = carsonEpoch,
                            index = 5,
                            baseSizeSp = 18f,
                            color = colors.secondary,
                        )
                    }
                }
            }
        }
    }
}

private fun usageText(usage: UsageInfo): String {
    if (!usage.granted) return "--"
    // usage time in minutes, glyph ≠ on the right
    return "${usage.millis / 60_000}≠"
}

/**
 * One header line: plain text today, Carson chaos inside the same column
 * when the mode is active. The line height stays 1.05 in OFF mode.
 */
@Composable
private fun HeaderLine(
    text: String,
    carsonMode: CarsonMode,
    carsonEpoch: Long,
    index: Int,
    baseSizeSp: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    if (carsonMode == CarsonMode.OFF) {
        BasicText(
            text,
            style = launcherStyle(baseSizeSp.sp, color, lineHeightFactor = 1.05f),
            modifier = modifier,
        )
    } else {
        CarsonText(
            plan = computeLinePlan(text, carsonMode, carsonSeed(carsonEpoch, index, text), baseSizeSp),
            color = color,
            modifier = modifier,
        )
    }
}
