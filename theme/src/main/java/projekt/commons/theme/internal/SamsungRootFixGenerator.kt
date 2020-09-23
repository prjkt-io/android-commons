package projekt.commons.theme.internal

import android.util.Xml
import com.topjohnwu.superuser.Shell
import projekt.commons.buildtools.BuildTools
import projekt.commons.theme.ThemeApp
import projekt.commons.theme.ThemeApplication
import java.io.File
import java.io.FileOutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets

internal object SamsungRootFixGenerator {
    private val TEMP_DIR = File(ThemeApplication.instance.cacheDir, "one_fix")
    private val OUTPUT_DIR = File("/data/overlays/currentstyle")

    fun run(): Boolean {
        TEMP_DIR.deleteRecursively()
        val outputUnsigned = File(TEMP_DIR, "unsigned.apk")
        val tempApk = File(TEMP_DIR, "fix.apk")
        val resDir = File(TEMP_DIR, "res")
        if (!generateResource(resDir)) {
            return false
        }
        val manifest = File(TEMP_DIR, "AndroidManifest.xml")
        manifest.apply {
            if (!createNewFile()) {
                return false
            }
            FileOutputStream(this).use { fos ->
                fos.write(generateManifest().toByteArray(StandardCharsets.UTF_8))
            }
        }
        val overlayBuildCommands = StringBuilder().apply {
            append("${BuildTools.getAapt(ThemeApplication.instance).absolutePath} p -f ")
            append("-M ${manifest.absolutePath} ")
            append("-S ${resDir.absolutePath} ")
            append("-I /system/framework/framework-res.apk ")
            append("-F ${outputUnsigned.absolutePath} \n")
        }.toString()
        val result = Runtime.getRuntime().exec(overlayBuildCommands)
        result.waitFor()
        if (result.exitValue() != 0) {
            return false
        }
        if (!signApk(ThemeApplication.instance, outputUnsigned, tempApk)) {
            return false
        }
        outputUnsigned.delete()
        manifest.delete()
        resDir.deleteRecursively()

        val to = File(OUTPUT_DIR, "one_fix.apk")
        val commands = ArrayList<String>().apply {
            add("cp -f $tempApk $to")
            add("chmod 644 $to")
            add("chown system:system $to")
        }
        Shell.su(*commands.toTypedArray()).exec()
        return true
    }

    private fun generateResource(targetDir: File): Boolean {
        val resources = ThemeApplication.instance.resources
        val id = resources.getIdentifier("config_nightDisplayAvailable", "bool", "android")
        val content = Xml.newSerializer().document(xmlStringWriter = StringWriter()) {
            element("resources") {
                element("bool") {
                    attribute("name", "config_nightDisplayAvailable")
                    text("${resources.getBoolean(id)}")
                }
            }
        }
        val dir = File(targetDir, "values")
        dir.mkdirs()
        File(dir, "bool.xml").apply {
            if (!createNewFile()) {
                return false
            }
            FileOutputStream(this).use { fos ->
                fos.write(content.toByteArray(StandardCharsets.UTF_8))
            }
        }
        return true
    }

    private fun generateManifest(): String {
        return Xml.newSerializer().document(xmlStringWriter = StringWriter()) {
            element("manifest") {
                attribute("xmlns:android", "http://schemas.android.com/apk/res/android")
                attribute("package", "samsung.root.fix")

                element("uses-permission") {
                    attribute("android:name", ThemeApp.SAMSUNG_OVERLAY_PERMISSION)
                }
                element("uses-permission") {
                    attribute("android:name", ThemeApp.OVERLAY_PERMISSION)
                }

                element("application") {
                    attribute("android:allowBackup", "false")
                    attribute("android:hasCode", "false")
                }

                element("overlay") {
                    attribute("android:priority", "1")
                    attribute("android:targetPackage", "fwk")
                }
            }
        }
    }
}
