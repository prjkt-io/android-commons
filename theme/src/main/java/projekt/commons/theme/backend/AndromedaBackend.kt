/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import projekt.andromeda.client.AndromedaOverlayManager
import projekt.andromeda.client.AndromedaPackageManager
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_DISABLED
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_ENABLED
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_UNKNOWN
import projekt.commons.theme.internal.isPackageInstalled
import java.util.ArrayList

internal class AndromedaBackend : Backend {

    override val overlayState: Map<String, Int>
        get() = AndromedaOverlayManager.overlayState

    override val targetWithMultipleOverlay: List<String>
        get() {
            val out = ArrayList<String>()
            val map = AndromedaOverlayManager.allOverlay
            for ((target, targetOverlays) in map) {
                var count = 0
                for (oi in targetOverlays) {
                    if (oi.isEnabled && oi.packageName.isPackageInstalled()) {
                        count++
                    }
                }
                if (count > 1) {
                    out.add(target)
                }
            }
            return out
        }

    override val miscStateValue: Int
        get() = STATE_UNKNOWN

    override val disabledStateValue: Int
        get() = STATE_DISABLED

    override val enabledStateValue: Int
        get() = STATE_ENABLED

    override fun installOverlay(paths: List<String>) {
        for (path in paths) {
            AndromedaPackageManager.install(path, AndromedaPackageManager.INSTALL_REINSTALL_APP)
        }
    }

    override fun uninstallOverlay(packages: List<String>, restartUi: Boolean) {
        for (packageName in packages) {
            AndromedaPackageManager.uninstall(packageName, 0)
        }
    }

    override fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean) {
        AndromedaOverlayManager.switchOverlay(packages, state)
    }

    override fun setPriority(packages: List<String>, restartUi: Boolean) {
        AndromedaOverlayManager.setPriority(packages)
    }

    override fun getEnabledOverlayWithTarget(targetPackage: String): List<String> {
        val out = ArrayList<String>()
        val map = AndromedaOverlayManager.allOverlay
        for ((target, targetOverlays) in map) {
            if (target == targetPackage) {
                for (oi in targetOverlays) {
                    if (oi.isEnabled && oi.packageName.isPackageInstalled()) {
                        out.add(oi.packageName)
                    }
                }
                return out
            }
        }
        return out
    }

    override fun restartSystemUi() {}

    override fun applyFonts(themePid: String, name: String) {}

    override fun restoreFonts() {}
}
