package com.riccardopatane.minimallauncher.data

import android.content.Context
import android.text.format.DateFormat as AndroidDateFormat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.riccardopatane.minimallauncher.model.AppEntry
import com.riccardopatane.minimallauncher.model.DateFormat
import com.riccardopatane.minimallauncher.model.FavoriteMarker
import com.riccardopatane.minimallauncher.model.FavoritePosition
import com.riccardopatane.minimallauncher.model.TimeSeparator
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        // Favorites as a single "pkg/activity" string per line: order preserved
        // (stringSetPreferencesKey does not guarantee order).
        private val KEY_FAVORITES = stringPreferencesKey("favorites")
        private val KEY_USE_24H = booleanPreferencesKey("use_24h")
        private val KEY_DATE_FORMAT = stringPreferencesKey("date_format")
        private val KEY_FAVORITE_MARKER = stringPreferencesKey("favorite_marker")
        private val KEY_FAVORITE_POSITION = stringPreferencesKey("favorite_position")
        private val KEY_TIME_SEPARATOR = stringPreferencesKey("time_separator")
        private val KEY_SHOW_UNLOCKS = booleanPreferencesKey("show_unlocks")
        private val KEY_SHOW_NOTIFICATIONS = booleanPreferencesKey("show_notifications")
        private val KEY_WALLPAPER_APPLIED = booleanPreferencesKey("wallpaper_applied")

        private fun notificationKey(): androidx.datastore.preferences.core.Preferences.Key<Int> =
            androidx.datastore.preferences.core.intPreferencesKey("notif_" + LocalDate.now())
    }

    /** "packageName/activityName" keys in insertion order. */
    val favorites: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITES].orEmpty()
            .lineSequence()
            .filter { it.isNotBlank() }
            .toList()
    }

    val use24h: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_USE_24H] ?: AndroidDateFormat.is24HourFormat(context)
    }

    /** Default: the first enum entry (ABBREVIATED, the home's historical format). */
    val dateFormat: Flow<DateFormat> = context.dataStore.data.map { prefs ->
        prefs[KEY_DATE_FORMAT]
            ?.let { name -> DateFormat.entries.firstOrNull { it.name == name } }
            ?: DateFormat.entries.first()
    }

    val favoriteMarker: Flow<FavoriteMarker> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_MARKER]
            ?.let { name -> FavoriteMarker.entries.firstOrNull { it.name == name } }
            ?: FavoriteMarker.NONE
    }

    val favoritePosition: Flow<FavoritePosition> = context.dataStore.data.map { prefs ->
        prefs[KEY_FAVORITE_POSITION]
            ?.let { name -> FavoritePosition.entries.firstOrNull { it.name == name } }
            ?: FavoritePosition.TOP
    }

    val timeSeparator: Flow<TimeSeparator> = context.dataStore.data.map { prefs ->
        prefs[KEY_TIME_SEPARATOR]
            ?.let { name -> TimeSeparator.entries.firstOrNull { it.name == name } }
            ?: TimeSeparator.COLON
    }

    val showUnlocks: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_UNLOCKS] ?: true
    }

    val showNotifications: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SHOW_NOTIFICATIONS] ?: true
    }

    suspend fun setFavorite(entry: AppEntry, favorite: Boolean) {
        val key = "${entry.packageName}/${entry.activityName}"
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_FAVORITES].orEmpty()
                .lineSequence()
                .filter { it.isNotBlank() }
                .toMutableList()
            if (favorite && key !in current) current.add(key)
            if (!favorite) current.remove(key)
            prefs[KEY_FAVORITES] = current.joinToString("\n")
        }
    }

    suspend fun setUse24h(value: Boolean) {
        context.dataStore.edit { it[KEY_USE_24H] = value }
    }

    suspend fun setDateFormat(format: DateFormat) {
        context.dataStore.edit { it[KEY_DATE_FORMAT] = format.name }
    }

    suspend fun setFavoriteMarker(marker: FavoriteMarker) {
        context.dataStore.edit { it[KEY_FAVORITE_MARKER] = marker.name }
    }

    suspend fun setFavoritePosition(position: FavoritePosition) {
        context.dataStore.edit { it[KEY_FAVORITE_POSITION] = position.name }
    }

    suspend fun setTimeSeparator(separator: TimeSeparator) {
        context.dataStore.edit { it[KEY_TIME_SEPARATOR] = separator.name }
    }

    suspend fun setShowUnlocks(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_UNLOCKS] = value }
    }

    suspend fun setShowNotifications(value: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_NOTIFICATIONS] = value }
    }

    /** True if the black wallpaper has already been applied by this app. */
    val wallpaperApplied: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_WALLPAPER_APPLIED] ?: false
    }

    suspend fun setWallpaperApplied(value: Boolean) {
        context.dataStore.edit { it[KEY_WALLPAPER_APPLIED] = value }
    }

    /** Notifications posted today (per-date key: resets by itself at midnight). */
    suspend fun notificationCountToday(): Int {
        return context.dataStore.data.first()[notificationKey()] ?: 0
    }

    private fun seenKey(): androidx.datastore.preferences.core.Preferences.Key<Set<String>> =
        androidx.datastore.preferences.core.stringSetPreferencesKey(
            "notif_seen_" + LocalDate.now(),
        )

    /**
     * Registers a notification: counts only the FIRST occurrence per key
     * (package|id|tag) per day — updates don't inflate the counter.
     */
    suspend fun registerNotification(key: String) {
        context.dataStore.edit { prefs ->
            val seen = prefs[seenKey()].orEmpty().toMutableSet()
            if (key !in seen) {
                seen.add(key)
                prefs[seenKey()] = seen
                prefs[notificationKey()] = (prefs[notificationKey()] ?: 0) + 1
            }
        }
    }

    /**
     * Removes counter/seen keys from past days: the counter resets by itself
     * at midnight (per-date key); this just avoids dead keys piling up in the
     * DataStore.
     */
    suspend fun cleanupOldNotificationKeys() {
        context.dataStore.edit { prefs ->
            val today = LocalDate.now().toString()
            val toRemove = prefs.asMap().keys.filter { key ->
                val name = key.name
                (name.startsWith("notif_") || name.startsWith("notif_seen_")) &&
                    !name.endsWith(today)
            }
            toRemove.forEach { prefs.remove(it) }
        }
    }

    /**
     * One-time reset of today's counter: old builds counted every update
     * (inflated counter). From now on only new notifications count.
     */
    suspend fun resetNotificationCountOnce() {
        val resetKey = androidx.datastore.preferences.core.booleanPreferencesKey(
            "notif_reset_v2",
        )
        context.dataStore.edit { prefs ->
            if (prefs[resetKey] != true) {
                prefs[notificationKey()] = 0
                prefs[seenKey()] = emptySet()
                prefs[resetKey] = true
            }
        }
    }
}
