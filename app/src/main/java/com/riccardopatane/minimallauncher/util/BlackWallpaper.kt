package com.riccardopatane.minimallauncher.util

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color

object BlackWallpaper {

    /** Applies pure black as both home and lock screen background. */
    fun apply(context: Context) {
        val wm = WallpaperManager.getInstance(context)
        val black = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        black.eraseColor(Color.BLACK)
        wm.setBitmap(
            black,
            null,
            true,
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK,
        )
    }
}
