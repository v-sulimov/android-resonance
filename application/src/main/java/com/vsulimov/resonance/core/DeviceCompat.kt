package com.vsulimov.resonance.core

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Device compatibility utilities for features that depend on OEM implementation
 * rather than API level alone.
 */
object DeviceCompat {

    /**
     * OEM manufacturers that report API 31+ but don't implement Material You.
     *
     * These devices expose `android.R.color.system_accent*` resources with
     * hard-coded default (blue) values instead of wallpaper-derived colors.
     */
    private val DYNAMIC_COLOR_EXCLUDED_MANUFACTURERS = setOf(
        "huawei",
        "honor"
    )

    /**
     * Returns `true` if the device supports Material You dynamic color.
     *
     * API 31+ is necessary but not sufficient — some OEMs (Huawei, Honor)
     * ship EMUI / HarmonyOS with API 31+ without implementing Material You.
     */
    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    fun isDynamicColorAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return false
        return Build.MANUFACTURER.lowercase() !in DYNAMIC_COLOR_EXCLUDED_MANUFACTURERS
    }
}
