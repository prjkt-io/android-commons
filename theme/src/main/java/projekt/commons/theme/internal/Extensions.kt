/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.internal

import android.content.Context
import android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import projekt.commons.theme.ThemeApplication
import java.lang.reflect.Field

internal const val METADATA_INSTALL_TIMESTAMP = "install_timestamp"

internal fun String.isPackageInstalled(): Boolean {
    return try {
        ThemeApplication.instance.packageManager.getApplicationInfo(this, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}

internal fun String.getApplicationLabel(): String {
    return ThemeApplication.instance.packageManager.getApplicationLabel(
            ThemeApplication.instance.packageManager.getApplicationInfo(this, 0)).toString()
}

internal fun PackageInfo.getCompatLongVersionCode(): Long {
    if (Build.VERSION.SDK_INT >= 28) {
        return longVersionCode
    }
    @Suppress("DEPRECATION")
    return versionCode.toLong()
}

internal fun Context.getOneUiVersion(): Double {
    if (!isSemAvailable()) {
        return 1.0
    }
    val semPlatformIntField: Field = Build.VERSION::class.java.getDeclaredField("SEM_PLATFORM_INT")
    val version: Int = semPlatformIntField.getInt(null) - 90000
    return if (version < 0) {
        1.0
    } else {
        ((version / 10000).toString() + "." + version % 10000 / 100).toDouble()
    }
}

private fun Context.isSemAvailable(): Boolean {
    return packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile") ||
            packageManager.hasSystemFeature("com.samsung.feature.samsung_experience_mobile_lite")
}

internal val Context.isApplicationDebugable: Boolean
    get() = (packageManager.getApplicationInfo(packageName, 0).flags and FLAG_DEBUGGABLE) != 0
