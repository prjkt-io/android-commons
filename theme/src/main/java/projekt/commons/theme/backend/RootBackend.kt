/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.O
import com.topjohnwu.superuser.Shell
import projekt.commons.theme.internal.isPackageInstalled
import java.util.ArrayList
import java.util.HashMap

/**
 * Root Backend for version prior to pie
 * Install method using pm install
 */
internal class RootBackend : Backend {

    private val overlayList: List<String>
        get() = Shell.su(OVERLAY_LIST).exec().out

    override val overlayState: Map<String, Int>
        get() {
            val out = HashMap<String, Int>()
            val outList = overlayList
            for (line in outList) {
                if (line.startsWith(ENABLED_PREFIX)) {
                    if (line.substring(4).isPackageInstalled()) {
                        out[line.substring(4)] = STATE_ENABLED
                    }
                } else if (line.startsWith(DISABLED_PREFIX)) {
                    if (line.substring(4).isPackageInstalled()) {
                        out[line.substring(4)] = STATE_DISABLED
                    }
                } else if (line.startsWith(MISSING_TARGET_PREFIX)) {
                    if (line.substring(4).isPackageInstalled()) {
                        out[line.substring(4)] = STATE_MISSING_TARGET
                    }
                }
            }
            return out
        }

    override val targetWithMultipleOverlay: List<String>
        get() {
            val out = ArrayList<String>()
            val outList = overlayList
            var currentApp = ""
            var counter = 0
            for (line in outList) {
                if (line.startsWith(ENABLED_PREFIX)) {
                    if (line.substring(4).isPackageInstalled()) {
                        counter++
                    }
                } else if (!line.startsWith(ENABLED_PREFIX) && !line.startsWith(DISABLED_PREFIX) &&
                    !line.startsWith(MISSING_TARGET_PREFIX)
                ) {
                    if (counter > 1) {
                        out.add(currentApp)
                    }
                    counter = 0
                    currentApp = line
                }
            }
            if (counter > 1) {
                out.add(currentApp)
            }
            return out
        }

    override val miscStateValue: Int
        get() = STATE_MISSING_TARGET

    override val disabledStateValue: Int
        get() = STATE_DISABLED

    override val enabledStateValue: Int
        get() = STATE_ENABLED

    override fun installOverlay(paths: List<String>) {
        val commands = ArrayList<String>()
        paths.forEach { path -> commands.add("$PM_INSTALL $path") }
        Shell.su(*commands.toTypedArray()).submit()
    }

    override fun uninstallOverlay(packages: List<String>, restartUi: Boolean) {
        val commands = ArrayList<String>()
        packages.forEach { pkg -> commands.add("$PM_UNINSTALL $pkg") }
        if (restartUi) {
            commands.add(KILL_SYSTEMUI)
        }
        Shell.su(*commands.toTypedArray()).submit()
    }

    override fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean) {
        val command = if (state) OVERLAY_ENABLE else OVERLAY_DISABLE
        val commands = ArrayList<String>()
        packages.forEach { pkg -> commands.add("$command $pkg") }
        if (restartUi) {
            commands.add(KILL_SYSTEMUI)
        }
        Shell.su(*commands.toTypedArray()).submit()
    }

    override fun setPriority(packages: List<String>, restartUi: Boolean) {
        val n = packages.size
        val commands = ArrayList<String>()
        for (i in 0 until n - 1) {
            commands.add(OVERLAY_SET_PRIORITY + " " + packages[i + 1] + " " + packages[i])
        }
        if (restartUi) {
            commands.add(KILL_SYSTEMUI)
        }
        Shell.su(*commands.toTypedArray()).submit()
    }

    override fun getEnabledOverlayWithTarget(targetPackage: String): List<String> {
        val out = ArrayList<String>()
        val outList = overlayList
        var found = false
        for (line in outList) {
            if (!found) {
                if (line == targetPackage) {
                    found = true
                }
            } else {
                if (line.startsWith(ENABLED_PREFIX)) {
                    if (line.substring(4).isPackageInstalled()) {
                        out.add(line.substring(4))
                    }
                } else if (!line.startsWith(DISABLED_PREFIX) && !line.startsWith(MISSING_TARGET_PREFIX)) {
                    // Done listing overlays, return value
                    return out
                }
            }
        }
        return out
    }

    override fun restartSystemUi() {
        Shell.su(KILL_SYSTEMUI).submit()
    }

    override fun applyFonts(themePid: String, name: String) {}

    override fun restoreFonts() {}

    companion object {
        private val STATE_MISSING_TARGET = if (SDK_INT >= O) 0 else 1
        private val STATE_DISABLED = if (SDK_INT >= O) 2 else 4
        private val STATE_ENABLED = if (SDK_INT >= O) 3 else 5

        private const val OVERLAY_ENABLE = "cmd overlay enable"
        private const val OVERLAY_DISABLE = "cmd overlay disable"
        private const val OVERLAY_LIST = "cmd overlay list"
        private const val OVERLAY_SET_PRIORITY = "cmd overlay set-priority"
        private const val PM_INSTALL = "pm install -r"
        private const val PM_UNINSTALL = "pm uninstall"
        private const val KILL_SYSTEMUI = "pkill -f com.android.systemui"

        private const val ENABLED_PREFIX = "[x]"
        private const val DISABLED_PREFIX = "[ ]"
        private const val MISSING_TARGET_PREFIX = "---"
    }
}
