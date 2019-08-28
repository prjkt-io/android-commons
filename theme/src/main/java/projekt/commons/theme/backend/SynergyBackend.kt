/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme.backend

import android.content.Intent
import android.os.Parcelable
import androidx.core.content.FileProvider
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_DISABLED
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_ENABLED
import projekt.andromeda.client.util.OverlayInfo.OverlayState.STATE_UNKNOWN
import projekt.commons.theme.ThemeApplication
import java.io.File
import java.net.URLConnection

internal class SynergyBackend : Backend {

    override val overlayState: Map<String, Int>
        get() = emptyMap()

    override val targetWithMultipleOverlay: List<String>
        get() = emptyList()

    override val miscStateValue: Int
        get() = STATE_UNKNOWN

    override val disabledStateValue: Int
        get() = STATE_DISABLED

    override val enabledStateValue: Int
        get() = STATE_ENABLED

    override fun installOverlay(paths: List<String>) {
        // Use ACTION_SEND intent to send only one item
        if (paths.size == 1) {
            val file = File(paths[0])
            val fileUri = FileProvider.getUriForFile(ThemeApplication.instance,
                    "${ThemeApplication.instance.packageName}.provider", file)
            Intent(Intent.ACTION_SEND).run {
                `package` = "projekt.samsung.theme.compiler"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                type = URLConnection.guessContentTypeFromName(file.name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ThemeApplication.instance.startActivity(this)
            }
        } else if (paths.size > 1) {
            val fileExtras = arrayListOf<Parcelable>()
            paths.forEach { path ->
                val fileUri = FileProvider.getUriForFile(ThemeApplication.instance,
                        "${ThemeApplication.instance.packageName}.provider", File(path))
                fileExtras.add(fileUri)
            }
            Intent(Intent.ACTION_SEND_MULTIPLE).run {
                `package` = "projekt.samsung.theme.compiler"
                putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileExtras)
                type = URLConnection.guessContentTypeFromName(File(paths[0]).name)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
                ThemeApplication.instance.startActivity(this)
            }
        }
    }

    override fun uninstallOverlay(packages: List<String>, restartUi: Boolean) {}

    override fun switchOverlay(packages: List<String>, state: Boolean, restartUi: Boolean) {}

    override fun setPriority(packages: List<String>, restartUi: Boolean) {}

    override fun getEnabledOverlayWithTarget(targetPackage: String): List<String> = emptyList()

    override fun restartSystemUi() {}

    override fun applyFonts(themePid: String, name: String) {}

    override fun restoreFonts() {}
}
