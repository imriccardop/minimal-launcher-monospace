package com.riccardopatane.minimallauncher.data

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Counts the notifications posted today. The system binds it when the user
 * grants notification access (Settings → Notifications access).
 */
class NotificationCountService : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        // exclude system notifications (android) and our own
        if (sbn.packageName == "android") return
        if (sbn.packageName == packageName) return
        // sbn.key = "package|id|tag": updates of the same notification are
        // not counted again
        scope.launch {
            SettingsRepository(this@NotificationCountService)
                .registerNotification(sbn.key)
        }
    }
}
