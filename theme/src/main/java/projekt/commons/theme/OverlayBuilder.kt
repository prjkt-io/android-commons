/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.theme

import android.os.Build
import android.util.ArrayMap
import androidx.preference.PreferenceManager
import com.android.apksig.ApkSigner
import com.topjohnwu.superuser.Shell
import projekt.commons.buildtools.BuildTools.getAapt
import projekt.commons.buildtools.BuildTools.getZipalign
import projekt.commons.theme.ThemeApp.OVERLAY_PERMISSION
import projekt.commons.theme.ThemeApp.SAMSUNG_OVERLAY_PERMISSION
import projekt.commons.theme.internal.METADATA_INSTALL_TIMESTAMP
import projekt.commons.theme.internal.SPLIT_DENSITY_IDENTIFIERS
import java.io.*
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.ArrayList
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 * A class for building overlays.
 * @param packageName Overlay package name.
 * @param targetPackageName Overlay target package name.
 * @param timestamp install timestamp, [System.currentTimeMillis] can be used for this.
 * @param versionCode Overlay version code.
 * @param versionName Overlay version name.
 * @param label Overlay package label.
 * @param metaData Collections of metadata to be added to the overlay manifest.
 * @param outDir target directory to put the generated overlay.
 */
class OverlayBuilder(
    private val packageName: String,
    private val targetPackageName: String,
    private val timestamp: Long,
    private val versionCode: Int? = null,
    private val versionName: String? = null,
    private val label: String? = null,
    private val metaData: ArrayMap<String, String>? = null,
    private val buildSplitApk: Boolean = true,
    private val outDir: File = File(ThemeApplication.instance.externalCacheDir, "overlays")
) {

    private val workDir = File(ThemeApplication.instance.cacheDir, "overlay_builder")

    private var extraBasePackagePath = emptyArray<String>()
    private var resourceDirs = emptyArray<String>()
    private var assetDir: String? = null

    /**
     * Adds extra base package (APK) to compile the overlays with. Equivalent
     * with the -I modifiers on AAPT.
     *
     * This method can be called multiple times as multiple base package
     * is supported.
     *
     * @param basePackage Absolute path of the package.
     */
    fun addExtraBasePackage(basePackage: String) {
        extraBasePackagePath += basePackage
    }

    /**
     * Adds directory of resources to be compiled. Equivalent with the -S
     * modifiers on AAPT.
     *
     * This method can be called multiple times as multiple resource
     * directories is supported.
     *
     * @param resDir directory of resources.
     */
    fun addResourceDir(resDir: File) {
        resourceDirs += resDir.absolutePath
    }

    /**
     * Sets directory of asset to be compiled. Equivalent with the -A
     * modifiers on AAPT.
     *
     * AAPT only supports one asset directory so this method will
     * replace the previously set asset directory.
     *
     * @param _assetDir directory of asset.
     */
    fun setAssetDir(_assetDir: File) {
        assetDir = _assetDir.absolutePath
    }

    /**
     * Runs the overlay builder.
     *
     * @return Result of builder in form of [Result].
     * @see Result
     */
    fun exec(): Result {
        if (resourceDirs.isEmpty()) {
            return Result.Failure("Resource directory cannot be empty!")
        }
        if (!outDir.isDirectory) {
            if (!outDir.mkdirs()) {
                return Result.Failure("Failed to create overlay cache directory")
            }
        }

        workDir.mkdirs()
        prepareResources()

        val result: Result = Result.Failure("")
        workDir.listFiles { file -> file.isDirectory }?.forEach {
            compileOverlay(it)
        }
        workDir.deleteRecursively()
        return result
    }

    /**
     * Generate base APK's AndroidManifest.xml to workDir root.
     */
    private fun generateBaseManifest(): String {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        // Root manifest element
        val rootElement = document.createElement("manifest")
        rootElement.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        rootElement.setAttribute("package", packageName)
        versionCode?.let {
            rootElement.setAttribute("android:versionCode", it.toString())
        }
        versionName?.let {
            rootElement.setAttribute("android:versionName", it)
        }

        // Overlay package attributes
        val overlayElement = document.createElement("overlay")
        overlayElement.setAttribute("android:targetPackage", targetPackageName)
        rootElement.appendChild(overlayElement)

        // Q overlays needs to "target" Q
        val usesSdkElement = document.createElement("uses-sdk")
        usesSdkElement.setAttribute("android:targetSdkVersion", Build.VERSION.SDK_INT.toString())
        rootElement.appendChild(usesSdkElement)

        // Proper permission for Samsung devices to utilize the overlay
        if (ThemeApp.isSamsung) {
            val usesPermissionSamsung = document.createElement("uses-permission")
            usesPermissionSamsung.setAttribute("android:name", SAMSUNG_OVERLAY_PERMISSION)
            rootElement.appendChild(usesPermissionSamsung)
        }

        // "permission" for easy overlay listing
        val permission = document.createElement("uses-permission")
        permission.setAttribute("android:name", OVERLAY_PERMISSION)
        rootElement.appendChild(permission)

        // Application attributes (for metadata)
        val applicationElement = document.createElement("application")
        applicationElement.setAttribute("android:allowBackup", "false")
        applicationElement.setAttribute("android:hasCode", "false")
        applicationElement.setAttribute("android:isSplitRequired", "$buildSplitApk")
        label?.let {
            applicationElement.setAttribute("android:label", it)
        }

        // Metadata
        metaData?.forEach { name, value ->
            val child = document.createElement("meta-data")
            child.setAttribute("android:name", name)
            child.setAttribute("android:value", value)
            applicationElement.appendChild(child)
        }

        val installTimestampElement = document.createElement("meta-data")
        installTimestampElement.setAttribute("android:name", METADATA_INSTALL_TIMESTAMP)
        installTimestampElement.setAttribute("android:value", timestamp.toString())
        applicationElement.appendChild(installTimestampElement)

        // Insert all child to parent
        rootElement.appendChild(applicationElement)
        document.appendChild(rootElement)

        // Finally get the manifest file
        val transformer = TransformerFactory.newInstance().newTransformer()
        val outWriter = StringWriter()
        transformer.transform(DOMSource(document), StreamResult(outWriter))

        outWriter.use { sw ->
            return sw.toString()
        }
    }

    /**
     * Generate split APK's AndroidManifest.xml.
     */
    private fun generateSplitManifest(splitConfig: String): String {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

        // Root manifest element
        val rootElement = document.createElement("manifest")
        rootElement.setAttribute("xmlns:android", "http://schemas.android.com/apk/res/android")
        rootElement.setAttribute("package", packageName)
        versionCode?.let {
            rootElement.setAttribute("android:versionCode", it.toString())
        }
        versionName?.let {
            rootElement.setAttribute("android:versionName", it)
        }
        rootElement.setAttribute("configForSplit", "")
        rootElement.setAttribute("split", "config.$splitConfig")

        // Application attributes
        val applicationElement = document.createElement("application")
        applicationElement.setAttribute("android:hasCode", "false")

        // Insert all child to parent
        rootElement.appendChild(applicationElement)
        document.appendChild(rootElement)

        // Finally get the manifest file
        val transformer = TransformerFactory.newInstance().newTransformer()
        val outWriter = StringWriter()
        transformer.transform(DOMSource(document), StreamResult(outWriter))

        outWriter.use { sw ->
            return sw.toString()
        }
    }

    private fun prepareResources() {
        var mainDir: File? = null
        resourceDirs.forEachIndexed { i, dir ->
            if (i < resourceDirs.lastIndex) {
                File(dir).apply {
                    copyRecursively(File(resourceDirs[i + 1]), overwrite = true)
                    deleteRecursively()
                }
            } else {
                mainDir = File(dir)
            }
        }

        mainDir?.apply {
            // Prepare split resources
            listFiles()?.forEach { dir ->
                if (dir.name.startsWith("drawable-")) {
                    val identifier = dir.name.substringAfter("drawable-")
                    if (SPLIT_DENSITY_IDENTIFIERS.contains(identifier)) {
                        val splitDir = File(workDir, "$packageName.split_config.$identifier")
                        dir.copyRecursively(File(splitDir, dir.name))
                        dir.deleteRecursively()
                        FileWriter(File(workDir, "${splitDir.name}.AndroidManifest.xml")).use { fw ->
                            fw.write(generateSplitManifest(identifier))
                        }
                    }
                } else if (dir.name.startsWith("values-")) {
                    // TODO: Split for values
                }
            }

            // Prepare base resources
            copyRecursively(File(workDir, packageName))
            deleteRecursively()
            FileWriter(File(workDir, "$packageName.AndroidManifest.xml")).use { fw ->
                fw.write(generateBaseManifest())
            }
        }
    }

    private fun compileOverlay(resDir: File): Result {
        val unsigned = File(outDir, "${resDir.name}-unsigned.apk")
        val aligned = File(outDir, "${resDir.name}-unsigned-aligned.apk")
        val signed = File(outDir, "${resDir.name}.apk")

        // Compile unsigned APK
        var doLegacyCompile = false
        val tempManifest = File(resDir.parent, "AndroidManifest.xml")
        File(resDir.parent, "${resDir.name}.AndroidManifest.xml").apply {
            copyTo(tempManifest)
            delete()
        }
        do {
            val command = StringBuilder()
            // Make sure this will call AAPT duh
            command.append(getAapt(ThemeApplication.instance).absolutePath).append(" p ")

            // The manifest
            command.append("-M ").append(tempManifest).append(" ")

            // Add resource directories
            command.append("-S ").append(resDir.absolutePath).append(" ")

            // Add asset directory to base package if set
            if (resDir.name == packageName && !assetDir.isNullOrEmpty()) {
                command.append("-A ").append(assetDir).append(" ")
            }

            // Compile against framework by default and target package
            // when we're not legacy compiling
            command.append("-I /system/framework/framework-res.apk ")
            if (!doLegacyCompile) {
                extraBasePackagePath.forEach { path ->
                    if (File(path).exists()) {
                        command.append("-I ").append(path).append(" ")
                    }
                }
            }

            // Specify the output dir
            command.append("-F ").append(unsigned.absolutePath).append(" ")
            command.append("--auto-add-overlay ")
            command.append("-f ")
            command.append('\n')

            // Run command and see
            var error = ""
            val process = Runtime.getRuntime().exec(command.toString())
            process.waitFor()
            BufferedReader(InputStreamReader(process.errorStream)).use { err ->
                err.forEachLine { line ->
                    if (line.contains("types not allowed")) {
                        val forceNewCompiler = PreferenceManager
                            .getDefaultSharedPreferences(ThemeApplication.instance)
                            .getBoolean("force_new_compiler", false)
                        if (!doLegacyCompile && !forceNewCompiler) {
                            doLegacyCompile = true
                        } else {
                            // Still failed with legacy compile, throw error
                            error = "$error\n${line}"
                        }
                    } else {
                        // If output exists then compilation is failed
                        error = "$error\n${line}"
                    }
                }
            }
            process.destroy()
            if (!doLegacyCompile && error.isNotEmpty()) {
                return Result.Failure(error)
            }
        } while (doLegacyCompile)

        // Just to make sure the compile is going fine so far
        if (!unsigned.isFile) {
            return Result.Failure("Failed to compile overlay")
        }

        // Zipalign the compiled overlay
        Shell.sh("${getZipalign(ThemeApplication.instance).absolutePath} 4 " +
                "${unsigned.absolutePath} ${aligned.absolutePath}").exec()
        if (!aligned.isFile) {
            return Result.Failure("Failed to zipalign overlay")
        }

        // Sign the zipaligned overlay
        try {
            ThemeApplication.instance.resources.openRawResource(R.raw.key).use { key ->
                val keyPass = "overlay".toCharArray()
                val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
                keyStore.load(key, keyPass)
                val privateKey = keyStore.getKey("key", keyPass) as PrivateKey
                val certs = ArrayList<X509Certificate>()
                certs.add(keyStore.getCertificateChain("key")[0] as X509Certificate)

                val signerConfig = ApkSigner.SignerConfig.Builder("overlay", privateKey, certs).build()
                val signerConfigs = ArrayList<ApkSigner.SignerConfig>()
                signerConfigs.add(signerConfig)
                val apkSigner = ApkSigner.Builder(signerConfigs)
                apkSigner.setV1SigningEnabled(false)
                    .setV2SigningEnabled(true)
                    .setInputApk(aligned)
                    .setOutputApk(signed)
                    .setMinSdkVersion(Build.VERSION.SDK_INT)
                    .build()
                    .sign()
            }
        } catch (e: Exception) {
            return Result.Failure("Failed to sign overlay")
        }

        // Delete unsigned APK
        tempManifest.delete()
        unsigned.delete()
        aligned.delete()

        return Result.Success(signed.absolutePath)
    }

    /**
     * This class represents the result of [exec].
     *
     * @see exec
     */
    sealed class Result {
        /**
         * Operation is success
         *
         * @param path Path of generated overlay.
         */
        class Success(val path: String) : Result()

        /**
         * Operation is failed
         *
         * @param message The error message from the operation.
         */
        class Failure(val message: String) : Result()
    }
}