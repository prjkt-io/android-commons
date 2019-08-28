/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme

import android.os.Bundle

/**
 * A data class of an overlay package.
 * @param name Package name of the overlay.
 * @param versionCode Version code of the overlay.
 * @param versionName Version name of the overlay.
 * @param metaData Bundle of metadata pulled from the overlay manifest.
 */
data class OverlayPackageInfo(
    val name: String,
    val versionCode: Long,
    val versionName: String,
    val metaData: Bundle
)