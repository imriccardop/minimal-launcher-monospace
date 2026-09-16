package com.riccardopatane.minimallauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.riccardopatane.minimallauncher.model.AppEntry
import java.text.Collator

class AppRepository(private val context: Context) {

    /** All launchable apps, alphabetically sorted, excluding the launcher itself. */
    fun loadApps(): List<AppEntry> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = context.packageManager
        val resolved = pm.queryIntentActivities(intent, 0)
        val collator = Collator.getInstance()
        val seen = HashSet<String>()
        val apps = resolved
            .asSequence()
            .filter { it.activityInfo.packageName != context.packageName }
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (!seen.add(pkg)) return@mapNotNull null // one package, one row
                AppEntry(
                    label = info.loadLabel(pm).toString(),
                    packageName = pkg,
                    activityName = info.activityInfo.name,
                )
            }
            .sortedWith(compareBy(collator) { it.label.lowercase() })
            // belt and braces: unique LazyColumn keys even if queryIntentActivities
            // returns duplicate entries (one ResolveInfo per matched filter)
            .distinctBy { it.packageName }
            .toList()
        return apps
    }

    fun launch(entry: AppEntry) {
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(entry.packageName, entry.activityName))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
