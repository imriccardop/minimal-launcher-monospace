package com.riccardopatane.minimallauncher.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.riccardopatane.minimallauncher.R
import com.riccardopatane.minimallauncher.data.AppRepository
import com.riccardopatane.minimallauncher.data.DeviceState
import com.riccardopatane.minimallauncher.data.SettingsRepository
import com.riccardopatane.minimallauncher.data.UsageStatsRepository
import com.riccardopatane.minimallauncher.model.AppEntry
import com.riccardopatane.minimallauncher.model.DateFormat
import com.riccardopatane.minimallauncher.model.FavoriteMarker
import com.riccardopatane.minimallauncher.model.FavoritePosition
import com.riccardopatane.minimallauncher.model.TimeSeparator
import com.riccardopatane.minimallauncher.util.BlackWallpaper
import com.riccardopatane.minimallauncher.util.LaunchIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class Screen { Home, Settings, FavoritesPicker, AppList }

data class LauncherUiState(
    val allApps: List<AppEntry> = emptyList(),
    val favorites: List<AppEntry> = emptyList(),
    val filteredApps: List<AppEntry> = emptyList(),
    val query: String = "",
    val use24h: Boolean = true,
    val dateFormat: DateFormat = DateFormat.entries.first(),
    val favoriteMarker: FavoriteMarker = FavoriteMarker.NONE,
    val favoritePosition: FavoritePosition = FavoritePosition.TOP,
    val timeSeparator: TimeSeparator = TimeSeparator.COLON,
    val showUnlocks: Boolean = true,
    val showNotifications: Boolean = true,
)

data class UsageInfo(
    val granted: Boolean,
    val millis: Long,
    val unlocks: Int,
)

class LauncherViewModel(app: Application) : AndroidViewModel(app) {

    companion object {
        /** Maximum number of favorite apps. */
        const val MAX_FAVORITES = 8
    }

    private val appRepository = AppRepository(app)
    private val settingsRepository = SettingsRepository(app)
    private val usageStatsRepository = UsageStatsRepository(app)

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val query = MutableStateFlow("")
    private val now = MutableStateFlow(System.currentTimeMillis())
    private val battery = MutableStateFlow(-1)
    private val usage = MutableStateFlow(UsageInfo(granted = false, millis = 0L, unlocks = 0))
    private val notificationsToday = MutableStateFlow(0)
    private val notifAccess = MutableStateFlow(false)

    private data class SettingsState(
        val favorites: List<String>,
        val use24h: Boolean,
        val dateFormat: DateFormat,
        val favoriteMarker: FavoriteMarker,
        val favoritePosition: FavoritePosition,
        val timeSeparator: TimeSeparator,
        val showUnlocks: Boolean,
        val showNotifications: Boolean,
    )

    // two-stage combine: the combine vararg tops out at 5 flows
    private val settingsBase = combine(
        settingsRepository.favorites,
        settingsRepository.use24h,
        settingsRepository.dateFormat,
        settingsRepository.favoriteMarker,
        settingsRepository.timeSeparator,
    ) { favs, use24h, dateFmt, marker, separator ->
        SettingsBase(favs, use24h, dateFmt, marker, separator)
    }

    private data class SettingsBase(
        val favorites: List<String>,
        val use24h: Boolean,
        val dateFormat: DateFormat,
        val favoriteMarker: FavoriteMarker,
        val timeSeparator: TimeSeparator,
    )

    private val settingsState = combine(
        settingsBase,
        settingsRepository.favoritePosition,
        settingsRepository.showUnlocks,
        settingsRepository.showNotifications,
    ) { base, position, showUnlocks, showNotifs ->
        SettingsState(
            base.favorites,
            base.use24h,
            base.dateFormat,
            base.favoriteMarker,
            position,
            base.timeSeparator,
            showUnlocks,
            showNotifs,
        )
    }

    val uiState: StateFlow<LauncherUiState> = combine(
        allApps,
        settingsState,
        query,
    ) { apps, s, q ->
        LauncherUiState(
            allApps = apps,
            favorites = apps.filter { s.favorites.contains("${it.packageName}/${it.activityName}") },
            filteredApps = if (q.isBlank()) emptyList()
            else apps.filter { it.label.contains(q, ignoreCase = true) },
            query = q,
            use24h = s.use24h,
            dateFormat = s.dateFormat,
            favoriteMarker = s.favoriteMarker,
            favoritePosition = s.favoritePosition,
            timeSeparator = s.timeSeparator,
            showUnlocks = s.showUnlocks,
            showNotifications = s.showNotifications,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LauncherUiState())

    val nowFlow = now.asStateFlow()
    val batteryFlow = battery.asStateFlow()
    val usageInfo = usage.asStateFlow()
    val notificationsTodayFlow = notificationsToday.asStateFlow()
    val notifAccessFlow = notifAccess.asStateFlow()

    /** Counter incremented on each HOME intent: the Home button must always
     *  bring back page 1 even when the activity doesn't go through pause
     *  (ON_RESUME doesn't fire when the launcher is already on top). */
    private val homeReset = MutableStateFlow(0)
    val homeResetFlow = homeReset.asStateFlow()

    fun onHomeIntent() {
        homeReset.value++
    }

    init {
        refreshApps()
        refreshUsage()
        ensureBlackWallpaper()

        viewModelScope.launch {
            DeviceState.minuteTicker().collect { now.value = it }
        }
        viewModelScope.launch {
            DeviceState.batteryPercent(getApplication()).collect { battery.value = it }
        }
        viewModelScope.launch {
            while (true) {
                refreshUsage()
                refreshNotifications()
                refreshNotifAccess()
                delay(60_000)
            }
        }
        // Auto-launch: exactly 1 result → open the app without pressing Enter.
        // collectLatest acts as the debounce: each keystroke cancels the
        // pending delay and restarts it.
        viewModelScope.launch {
            query.collectLatest { q ->
                if (q.isBlank()) return@collectLatest
                val matches = allApps.value.filter { it.label.contains(q, ignoreCase = true) }
                if (matches.size == 1) {
                    delay(300)
                    appRepository.launch(matches.first())
                    query.value = ""
                }
            }
        }
    }

    /** On return to the home: fresh app list, updated counters, clean search. */
    fun onResume() {
        refreshApps()
        refreshUsage()
        refreshNotifications()
        refreshNotifAccess()
        ensureBlackWallpaper()
        query.value = ""
    }

    fun refreshApps() {
        // queryIntentActivities + loadLabel are slow Binder IPC: never on main
        viewModelScope.launch(Dispatchers.IO) {
            allApps.value = appRepository.loadApps()
        }
    }

    fun refreshUsage() {
        viewModelScope.launch {
            val granted = usageStatsRepository.hasUsageAccess()
            val millis = if (granted) usageStatsRepository.screenOnMillisToday() else 0L
            val unlocks = if (granted) usageStatsRepository.unlockCountToday() else 0
            usage.value = UsageInfo(granted, millis, unlocks)
        }
    }

    fun refreshNotifications() {
        viewModelScope.launch {
            settingsRepository.cleanupOldNotificationKeys()
            settingsRepository.resetNotificationCountOnce()
            notificationsToday.value = settingsRepository.notificationCountToday()
        }
    }

    fun refreshNotifAccess() {
        notifAccess.value = LaunchIntents.hasNotificationAccess(getApplication())
    }

    /** Immediately applies the black wallpaper (home + lockscreen) and remembers it. */
    fun applyBlackWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { BlackWallpaper.apply(getApplication()) }
            settingsRepository.setWallpaperApplied(true)
        }
    }

    /** If the app is the default home and hasn't done it yet, applies the
     *  black wallpaper (home + lockscreen) once. */
    fun ensureBlackWallpaper() {
        viewModelScope.launch(Dispatchers.IO) {
            val context = getApplication<Application>()
            if (!isDefaultHome(context)) return@launch
            if (settingsRepository.wallpaperApplied.first()) return@launch
            applyBlackWallpaper()
        }
    }

    private fun isDefaultHome(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val info = context.packageManager.resolveActivity(
            intent, PackageManager.MATCH_DEFAULT_ONLY,
        )
        return info?.activityInfo?.packageName == context.packageName
    }

    fun setQuery(q: String) {
        query.value = q
    }

    fun clearQuery() {
        query.value = ""
    }

    fun launch(entry: AppEntry) {
        appRepository.launch(entry)
    }

    /** Enter key: opens the first result. The bar searches apps only
     *  (no web search): with zero results Enter does nothing. */
    fun onSearchSubmitted() {
        val s = uiState.value
        if (s.query.isBlank()) return
        if (s.filteredApps.isNotEmpty()) {
            appRepository.launch(s.filteredApps.first())
            query.value = ""
        }
    }

    fun toggleFavorite(entry: AppEntry) {
        viewModelScope.launch {
            val key = "${entry.packageName}/${entry.activityName}"
            val current = settingsRepository.favorites.first()
            if (key !in current && current.size >= MAX_FAVORITES) {
                Toast.makeText(
                    getApplication(),
                    R.string.max_favorites,
                    Toast.LENGTH_SHORT,
                ).show()
                return@launch
            }
            settingsRepository.setFavorite(entry, key !in current)
        }
    }

    fun setUse24h(value: Boolean) {
        viewModelScope.launch { settingsRepository.setUse24h(value) }
    }

    fun setDateFormat(format: DateFormat) {
        viewModelScope.launch { settingsRepository.setDateFormat(format) }
    }

    fun setFavoriteMarker(marker: FavoriteMarker) {
        viewModelScope.launch { settingsRepository.setFavoriteMarker(marker) }
    }

    fun setFavoritePosition(position: FavoritePosition) {
        viewModelScope.launch { settingsRepository.setFavoritePosition(position) }
    }

    fun setTimeSeparator(separator: TimeSeparator) {
        viewModelScope.launch { settingsRepository.setTimeSeparator(separator) }
    }

    fun setShowUnlocks(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowUnlocks(value) }
    }

    fun setShowNotifications(value: Boolean) {
        viewModelScope.launch { settingsRepository.setShowNotifications(value) }
    }
}
