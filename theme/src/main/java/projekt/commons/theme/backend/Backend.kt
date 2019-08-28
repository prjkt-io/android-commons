/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import android.os.RemoteException
import projekt.commons.theme.ThemeApp.getBackend
import projekt.commons.theme.ThemeApp.OverlayState

/**
 * Interface to do overlay related operations.
 *
 * @see getBackend
 */
interface Backend {
    /**
     * Returns map of installed overlay package name and its state.
     *
     * @see OverlayState
     */
    val overlayState: Map<String, Int>

    /**
     * Returns list of target packages that overlaid by multiple
     * overlays.
     */
    val targetWithMultipleOverlay: List<String>

    /**
     * @suppress
     * Overlay misc state value
     */
    val miscStateValue: Int

    /**
     * @suppress
     * Overlay disabled state value
     */
    val disabledStateValue: Int

    /**
     * @suppress
     * Overlay enabled state value
     */
    val enabledStateValue: Int

    /**
     * Installs overlay to the system.
     *
     * @param paths List of overlay package paths to be installed.
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun installOverlay(paths: List<String>)

    /**
     * Uninstalls overlay from the system.
     *
     * @param packages List of overlay package names to be uninstalled.
     * @param restartUi Whether to restert SystemUI after operation
     * is done.
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun uninstallOverlay(packages: List<String>, restartUi: Boolean)

    /**
     * Switches overlay state.
     *
     * @param packages List of overlay package names to be switched.
     * @param state `true` to enable, `false` otherwise.
     * @param restartUi Whether to restert SystemUI after operation
     * is done.
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean)

    /**
     * Sets priority of overlays.
     *
     * @param packages List of overlays to be set. In sequence, the
     * priority will be lower to higher.
     * @param restartUi Whether to restert SystemUI after operation
     * is done.
     *
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun setPriority(packages: List<String>, restartUi: Boolean)

    /**
     * Returns list of enabled overlay package names for a target.
     *
     * @param targetPackage Target package of the overlay.
     * @return Will never be `null`
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun getEnabledOverlayWithTarget(targetPackage: String): List<String>

    /**
     * Restarts SystemUI
     *
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun restartSystemUi()

    /**
     * Applies custom fonts to the system.
     *
     * @param themePid Theme package name.
     * @param name fonts package name inside the theme.
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun applyFonts(themePid: String, name: String)

    /**
     * Restores fonts to system default.
     *
     * @throws RemoteException
     */
    @Throws(RemoteException::class)
    fun restoreFonts()
}
