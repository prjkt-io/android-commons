/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme

/**
 * Class to represent the options for theme app configurations.
 */
class ThemeAppOptions private constructor(
        internal val withAndromeda: Boolean,
        internal val withSubstratumService: Boolean,
        internal val withRoot: Boolean,
        internal val withPieRoot: Boolean,
        internal val withAndromedaSamsung: Boolean,
        internal val withSynergy: Boolean,
        internal val armSupport: Boolean,
        internal val arm64Support: Boolean
) {

    private constructor(builder: Builder) : this(
            builder.andromedaSupport,
            builder.substratumServiceSupport,
            builder.rootSupport,
            builder.pieRootSupport,
            builder.andromedaSamsungSupport,
            builder.synergySupport,
            builder.armSupport,
            builder.arm64Support
    )

    companion object {
        /**
         * Combine all of the options that have been set and returns
         * a new [ThemeAppOptions] object.
         */
        inline fun build(block: Builder.() -> Unit) = Builder().apply(block).build()
    }

    /**
     * Builder class for [ThemeAppOptions] objects.
     */
    class Builder {
        /**
         * Whether to support device with Andromeda Server installed.
         */
        var andromedaSupport = false

        /**
         * Whether to support device that runs Substratum Service.
         */
        var substratumServiceSupport = false

        /**
         * Whether to support Rooted pre-Pie device.
         */
        var rootSupport = false

        /**
         * Whether to support Rooted Pie+ device.
         */
        var pieRootSupport = false

        /**
         * Whether to support pre-Pie Samsung devices with
         * Andromeda Server installed.
         */
        var andromedaSamsungSupport = false

        /**
         * Whether to support device with Synergy installed.
         */
        var synergySupport = false

        /**
         * Whether to support device with armeabi-v7a architecture
         */
        var armSupport = true

        /**
         * Whether to support device with arm64-v8a architecture
         */
        var arm64Support = true

        /**
         * Combine all of the options that have been set and returns
         * a new [ThemeAppOptions] object.
         */
        fun build() = ThemeAppOptions(this)
    }
}