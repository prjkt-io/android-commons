/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import projekt.commons.theme.ThemeApp
import projekt.commons.theme.ThemeApp.getOverlayPackageInfo
import projekt.commons.theme.internal.METADATA_INSTALL_TIMESTAMP

/**
 * Switches overlay state.
 *
 * @param overlay Overlay package name to be switched.
 * @param state `true` to state, `false` otherwise.
 * is done.
 */
fun Backend.switchOverlay(overlay: String, state: Boolean) {
    val list = listOf(overlay)
    this.switchOverlay(list, state, shouldRestartSystemUi(list))
}

/**
 * Switches overlay state.
 *
 * @param overlays List of overlay package names to be switched.
 * @param state `true` to enable, `false` otherwise.
 */
fun Backend.switchOverlay(overlays: List<String>, state: Boolean) {
    this.switchOverlay(overlays, state, shouldRestartSystemUi(overlays))
}

/**
 * Sets priority of overlays.
 *
 * @param overlays List of overlays to be set. In sequence, the
 * priority will be lower to higher.
 */
fun Backend.setPriority(overlays: List<String>) {
    this.setPriority(overlays, shouldRestartSystemUi(overlays))
}

/**
 * Uninstalls overlay from the system.
 *
 * @param overlays List of overlay package names to be uninstalled.
 */
fun Backend.uninstallOverlay(overlays: List<String>) {
    this.uninstallOverlay(overlays, shouldRestartSystemUi(overlays))
}

/**
 * Checks whether the installed overlay is the intended ones.
 * This is checked against timestamp in the overlay metadata.
 *
 * @param name Package name of the overlay.
 * @param timeStamp Timestamp of the intended overlay.
 */
fun Backend.isOverlayNewest(name: String, timeStamp: Long): Boolean {
    // No need to check on Synergy
    if (ThemeApp.isSynergy) {
        return true
    }
    // Check if overlay is settled on system
    if (ThemeApp.isSubstratumService && !this.overlayState.containsKey(name)) {
        return false
    }
    // Finally check if expected timestamp is matched with the one inside overlay
    when (val overlayTimestamp = getOverlayPackageInfo(name)?.metaData?.get(METADATA_INSTALL_TIMESTAMP)) {
        is Float -> {
            if (overlayTimestamp != timeStamp.toFloat()) {
                return false
            }
        }
        is String -> {
            // Metadata of an inactive overlay is weird string, so
            // let's try to convert it to float
            try {
                if (overlayTimestamp.toFloat() != timeStamp.toFloat()) {
                    return false
                }
            } catch (ignored: Exception) {
            }
        }
        else -> return false
    }
    return true
}

private fun shouldRestartSystemUi(overlays: List<String>): Boolean {
    if (!ThemeApp.isSubstratumService) {
        for (overlay in overlays) {
            if (overlay.startsWith("com.android.systemui")) {
                return true
            }
        }
    }
    return false
}