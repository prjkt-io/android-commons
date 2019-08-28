/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import android.content.om.OverlayInfo
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import android.os.Process
import android.util.Log
import projekt.substratum.platform.SubstratumServiceBridge
import java.util.ArrayList
import java.util.HashMap

internal class SubstratumServiceBackend : Backend {

    override val overlayState: Map<String, Int>
        get() {
            val out = HashMap<String, Int>()
            catchException {
                val map = service.getAllOverlays(UID)
                for ((_, targetOverlays) in map) {
                    if (targetOverlays is List<*>) {
                        for (oi in targetOverlays) {
                            if (oi is OverlayInfo) {
                                if (oi.state == STATE_MISSING_TARGET) {
                                    out[oi.packageName] = STATE_MISSING_TARGET
                                } else if (oi.isEnabled) {
                                    out[oi.packageName] = STATE_ENABLED
                                } else if (!oi.isEnabled) {
                                    out[oi.packageName] = STATE_DISABLED
                                }
                            }
                        }
                    }
                }
            }
            return out
        }

    override val targetWithMultipleOverlay: List<String>
        get() {
            val out = ArrayList<String>()
            catchException {
                val map = service.getAllOverlays(UID)
                for ((targetName, targetOverlays) in map) {
                    var count = 0
                    if (targetOverlays is List<*>) {
                        for (oi in targetOverlays) {
                            if (oi is OverlayInfo) {
                                if (oi.isEnabled) {
                                    count++
                                }
                            }
                        }
                        if (count > 1) {
                            out.add(targetName.toString())
                        }
                    }
                }
            }
            return out
        }

    override val miscStateValue: Int
        get() = STATE_MISSING_TARGET

    override val disabledStateValue: Int
        get() = STATE_DISABLED

    override val enabledStateValue: Int
        get() = STATE_ENABLED

    override fun getEnabledOverlayWithTarget(targetPackage: String): List<String> {
        val out = ArrayList<String>()
        catchException {
            val map = service.getAllOverlays(UID)
            for ((targetName, targetOverlays) in map) {
                if (targetPackage == targetName) {
                    if (targetOverlays is List<*>) {
                        for (oi in targetOverlays) {
                            if (oi is OverlayInfo) {
                                if (oi.isEnabled) {
                                    out.add(oi.packageName)
                                }
                            }
                        }
                    }
                    return out
                }
            }
        }
        return out
    }

    override fun installOverlay(paths: List<String>) {
        catchException { service.installOverlay(paths) }
    }

    override fun uninstallOverlay(packages: List<String>, restartUi: Boolean) {
        catchException { service.uninstallOverlay(packages, restartUi) }
    }

    override fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean) {
        catchException { service.switchOverlay(packages, state, restartUi) }
    }

    override fun setPriority(packages: List<String>, restartUi: Boolean) {
        catchException { service.setPriority(packages, restartUi) }
    }

    override fun restartSystemUi() {
        catchException { service.restartSystemUI() }
    }

    override fun applyFonts(themePid: String, name: String) {
        catchException { service.applyFonts(themePid, name) }
    }

    override fun restoreFonts() {
        catchException { service.applyFonts(null, null) }
    }

    private inline fun catchException(action: () -> Unit) {
        try {
            action()
        } catch (e: Exception) {
            Log.wtf("SubstratumServiceBackend", "Exception caught", e)
        }
    }

    companion object {
        private val STATE_MISSING_TARGET = if (SDK_INT >= O) 0 else 1
        private val STATE_DISABLED = if (SDK_INT >= O) 2 else 4
        private val STATE_ENABLED = if (SDK_INT >= O) 3 else 5

        private val UID = Process.myUid() / 100000
        private val service = SubstratumServiceBridge.get()
    }
}
