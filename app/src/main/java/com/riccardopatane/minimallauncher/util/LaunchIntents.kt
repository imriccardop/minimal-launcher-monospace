package com.riccardopatane.minimallauncher.util

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.MediaStore
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import com.riccardopatane.minimallauncher.R

/**
 * Fallback chains for dialer/camera and system shortcuts.
 * NB: CATEGORY_APP_DIALER / CATEGORY_APP_CAMERA do not exist in the SDK.
 */
object LaunchIntents {

    private val FALLBACK_DIALER = listOf("com.google.android.dialer")
    private val FALLBACK_CAMERA = listOf("com.google.android.GoogleCamera")

    fun openDialer(context: Context) {
        // 1. ACTION_DIAL with no data: every dialer declares it
        if (tryLaunch(context, Intent(Intent.ACTION_DIAL))) return
        // 2. system default dialer package
        val telecom = context.getSystemService(TelecomManager::class.java)
        telecom?.defaultDialerPackage?.let { pkg ->
            if (tryLaunchPackage(context, pkg)) return
        }
        // 3. ACTION_DIAL with tel: URI
        if (tryLaunch(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:")))) return
        // 4. known packages
        for (pkg in FALLBACK_DIALER) {
            if (tryLaunchPackage(context, pkg)) return
        }
        toastNotFound(context)
    }

    fun openUrl(context: Context, url: String) {
        if (!tryLaunch(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))) {
            toastNotFound(context)
        }
    }

    fun openDigitalWellbeing(context: Context) {
        // Digital Wellbeing (Pixel): the main settings activity
        if (tryLaunch(
                context,
                Intent().setComponent(
                    ComponentName(
                        "com.google.android.apps.wellbeing",
                        "com.google.android.apps.wellbeing.settings.TopLevelSettingsActivity",
                    ),
                ),
            )
        ) return
        if (tryLaunchPackage(context, "com.google.android.apps.wellbeing")) return
        if (tryLaunch(context, Intent(Settings.ACTION_SETTINGS))) return
        toastNotFound(context)
    }

    fun openClock(context: Context) {
        // standard intent handled by the Clock app
        if (tryLaunch(context, Intent(AlarmClock.ACTION_SHOW_ALARMS))) return
        // fallback: known package
        for (pkg in listOf("com.google.android.deskclock")) {
            if (tryLaunchPackage(context, pkg)) return
        }
        toastNotFound(context)
    }

    fun openCamera(context: Context) {
        // 1. "open camera in still image mode" — declared by AOSP Camera2 and GCam
        if (tryLaunch(context, Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))) return
        // 2. image capture
        if (tryLaunch(context, Intent(MediaStore.ACTION_IMAGE_CAPTURE))) return
        // 3. known packages
        for (pkg in FALLBACK_CAMERA) {
            if (tryLaunchPackage(context, pkg)) return
        }
        toastNotFound(context)
    }

    fun usageAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}"))
            .let { scoped ->
                if (scoped.resolveActivity(context.packageManager) != null) scoped
                else Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            }
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context)
            .contains(context.packageName)

    fun notificationAccessIntent(context: Context): Intent =
        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    /** System page to set the app as the default launcher. */
    fun defaultHomeIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= 29) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
        }
        return Intent(Settings.ACTION_HOME_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun tryLaunch(context: Context, intent: Intent): Boolean {
        if (intent.resolveActivity(context.packageManager) == null) return false
        return try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (_: ActivityNotFoundException) {
            false
        }
    }

    private fun tryLaunchPackage(context: Context, pkg: String): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(pkg) ?: return false
        return tryLaunch(context, intent)
    }

    private fun toastNotFound(context: Context) {
        Toast.makeText(context, R.string.app_not_found, Toast.LENGTH_SHORT).show()
    }
}
