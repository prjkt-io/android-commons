/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import android.provider.Settings
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import projekt.commons.shell.Shell.exec
import projekt.commons.theme.BuildConfig
import projekt.commons.theme.ThemeApplication
import projekt.commons.theme.internal.getApplicationLabel
import projekt.commons.theme.internal.isPackageInstalled
import java.util.ArrayList
import java.util.HashMap

/**
 * Root backend for Pie onwards
 * Install method using system-less magisk module
 */
internal class PieRootBackend : Backend {

    private val overlayList: List<String>
        get() = exec(OVERLAY_LIST).output

    private val magiskModuleInstalled: Boolean
        get() = exec("test -d $MAGISK_MODULE_DIR || echo '0'").output.isEmpty()

    internal val magiskModuleActivated: Boolean
        get() {
            if (magiskModuleInstalled) {
                return exec("test -f $MAGISK_MODULE_DIR/update || echo '0'").output.isNotEmpty()
            }
            return false
        }

    internal val magiskModuleDisabled: Boolean
        get() {
            if (magiskModuleInstalled) {
                return exec("test -f $MAGISK_MODULE_DIR/disable || echo '0'").output.isNullOrEmpty()
            }
            return false
        }

    override val overlayState: Map<String, Int>
        get() {
            val out = HashMap<String, Int>()
            val outList = overlayList
            for (line in outList) {
                when {
                    line.startsWith(ENABLED_PREFIX) -> {
                        out[line.substring(4)] = STATE_ENABLED
                    }
                    line.startsWith(DISABLED_PREFIX) -> {
                        out[line.substring(4)] = STATE_DISABLED
                    }
                    line.startsWith(MISSING_TARGET_PREFIX) -> {
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

    /**
     * These fields should only be used on Samsung OneUI devices
     */
    private val samsungExposurePrefKey = "current_sec_active_themepackage"
    private val samsungExposurePrefValue = "theme"
    val samsungExposureSwitchable: Boolean
        get() {
            val prefValue = Settings.System.getString(
                    ThemeApplication.instance.contentResolver,
                    samsungExposurePrefKey
            )
            return prefValue == samsungExposurePrefValue || prefValue == null
        }
    var samsungExposureEnabled: Boolean
        get() {
            return Settings.System.getString(
                    ThemeApplication.instance.contentResolver,
                    samsungExposurePrefKey
            ) == samsungExposurePrefValue
        }
        set(value) {
            val newValue = if (value) samsungExposurePrefValue else null
            exec("settings put system $samsungExposurePrefKey $newValue")
        }

    override fun installOverlay(paths: List<String>) {
        val n = paths.size
        val commands = ArrayList<String>()
        for (i in 0 until n) {
            val split = paths[i].split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            commands.add("$MOVE_OVERLAY ${paths[i]} $INSTALL_PREFIX${split[split.size - 1]}")
            commands.add("chmod 644 $INSTALL_PREFIX${split[split.size - 1]}")
        }
        GlobalScope.launch {
            exec(commands.toTypedArray())
        }
    }

    override fun uninstallOverlay(packages: List<String>, restartUi: Boolean) {
        val n = packages.size
        val commands = ArrayList<String>()
        for (i in 0 until n) {
            commands.add(REMOVE_OVERLAY + " " + INSTALL_PREFIX + packages[i] + ".apk")
        }
        if (restartUi) {
            commands.add(KILL_SYSTEMUI)
        }
        GlobalScope.launch {
            exec(commands.toTypedArray())
        }
    }

    override fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean) {
        val n = packages.size
        val command = if (state) OVERLAY_ENABLE else OVERLAY_DISABLE
        val commands = ArrayList<String>()
        for (i in 0 until n) {
            commands.add(command + " " + packages[i])
        }
        if (restartUi) {
            commands.add(KILL_SYSTEMUI)
        }
        exec(commands.toTypedArray())
    }

    override fun setPriority(packages: List<String>, restartUi: Boolean) {
        with(packages.reversed()) {
            val commands = ArrayList<String>()
            for (i in 0 until size - 1) {
                commands.add(OVERLAY_SET_PRIORITY + " " + get(i) + " " + get(i + 1))
            }
            if (restartUi) {
                commands.add(KILL_SYSTEMUI)
            }
            exec(commands.toTypedArray())
        }
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
        exec(KILL_SYSTEMUI)
    }

    override fun applyFonts(themePid: String, name: String) {}

    override fun restoreFonts() {}

    internal fun installMagiskModule(): Boolean {
        if (!magiskModuleInstalled) {
            val appLabel = ThemeApplication.instance.packageName.getApplicationLabel()
            val command = "set -ex \n" +
                    "mkdir -p $MAGISK_MODULE_DIR; " +
                    "printf " +
                    "'name=$appLabel Overlay Helper\n" +
                    "version=${BuildConfig.VERSION_NAME}\n" +
                    "versionCode=${BuildConfig.VERSION_CODE}\n" +
                    "author=$appLabel\n" +
                    "description=System-less overlay system for $appLabel\n" +
                    "minMagisk=$MIN_MAGISK_VERSION\n'" +
                    " > $MAGISK_MODULE_DIR/module.prop; " +
                    "touch $MAGISK_MODULE_DIR/auto_mount; " +
                    "mkdir -p $PIE_INSTALL_DIR; "
            exec(command)
            return true
        }
        return false
    }

    internal fun checkMagisk(): Boolean {
        return exec("su -v").output.joinToString().contains("magisk", true) &&
                getMagiskVersion() >= MIN_MAGISK_VERSION
    }

    private fun getMagiskVersion(): Int {
        val output = exec("su -V").output
        if (output.size == 1) { // Strict rule rules
            try {
                return output[0].toInt()
            } catch (ignored: NumberFormatException) {
            }
        }
        return -1
    }

    companion object {
        private const val MIN_MAGISK_VERSION = 18000

        private const val STATE_MISSING_TARGET = 0
        private const val STATE_DISABLED = 2
        private const val STATE_ENABLED = 3

        private val MAGISK_MODULE_DIR = "${getMagiskDirectory()}/img/${ThemeApplication.instance.packageName}.helper/"
        internal val PIE_INSTALL_DIR = MAGISK_MODULE_DIR + "system/app/"

        private const val OVERLAY_ENABLE = "cmd overlay enable"
        private const val OVERLAY_DISABLE = "cmd overlay disable"
        private const val OVERLAY_LIST = "cmd overlay list"
        private const val OVERLAY_SET_PRIORITY = "cmd overlay set-priority"
        private const val KILL_SYSTEMUI = "pkill -f com.android.systemui"
        private const val MOVE_OVERLAY = "cp -f"
        private const val REMOVE_OVERLAY = "rm -f"

        internal val INSTALL_PREFIX = "${PIE_INSTALL_DIR}_"
        private const val ENABLED_PREFIX = "[x]"
        private const val DISABLED_PREFIX = "[ ]"
        private const val MISSING_TARGET_PREFIX = "---"

        private fun getMagiskDirectory(): String {
            return "/sbin/.magisk"
        }
    }
}
