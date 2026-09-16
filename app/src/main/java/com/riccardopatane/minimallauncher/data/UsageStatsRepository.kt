package com.riccardopatane.minimallauncher.data

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.os.Process
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UsageStatsRepository(private val context: Context) {

    /** PACKAGE_USAGE_STATS is not a runtime permission: check it via AppOps. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = if (Build.VERSION.SDK_INT >= 29) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /**
     * Milliseconds of usage since midnight, with the same definition as
     * Digital Wellbeing: foreground time ONLY while the screen is on (the
     * foreground time with the screen off — e.g. overnight in Bedtime mode —
     * doesn't count). -1 if the permission is missing.
     *
     * Intersection of the two event streams: ACTIVITY_RESUMED/PAUSED (app in
     * foreground) and SCREEN_INTERACTIVE/NON_INTERACTIVE (screen on).
     * 24-hour lookback to know the initial state at midnight.
     */
    suspend fun screenOnMillisToday(): Long = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext -1L
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return@withContext -1L
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val events = usm.queryEvents(startOfDay - 24 * 3_600_000L, now)
            ?: return@withContext 0L

        var screenOn = false
        var fgSince: Long? = null
        var total = 0L
        val e = UsageEvents.Event()

        fun closeForeground(at: Long) {
            fgSince?.let { s ->
                total += (at - maxOf(s, startOfDay)).coerceAtLeast(0L)
            }
            fgSince = null
        }

        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            when (e.eventType) {
                UsageEvents.Event.SCREEN_INTERACTIVE -> {
                    screenOn = true
                    // an app was foreground with the screen off: its time
                    // restarts from here (screen-off time doesn't count)
                    if (fgSince != null) fgSince = e.timeStamp
                }
                UsageEvents.Event.SCREEN_NON_INTERACTIVE -> {
                    screenOn = false
                    closeForeground(e.timeStamp)
                }
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    if (screenOn && fgSince == null) fgSince = e.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED -> closeForeground(e.timeStamp)
            }
        }
        // current session (screen on and app foreground right now)
        if (screenOn) closeForeground(now)
        total.coerceAtMost(now - startOfDay)
    }

    /**
     * Number of unlocks since midnight: each KEYGUARD_HIDDEN is one unlock.
     * 0 if the permission is missing (recent events may lag a few minutes).
     */
    suspend fun unlockCountToday(): Int = withContext(Dispatchers.IO) {
        if (!hasUsageAccess()) return@withContext 0
        val usm = context.getSystemService(UsageStatsManager::class.java) ?: return@withContext 0
        val now = System.currentTimeMillis()
        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val events = usm.queryEvents(startOfDay, now) ?: return@withContext 0
        var count = 0
        val e = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.KEYGUARD_HIDDEN) count++
        }
        count
    }
}
