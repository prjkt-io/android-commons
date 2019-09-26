/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.buildtools

import android.content.Context
import android.os.Build
import android.text.TextUtils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * Main class for build tools setup
 */
object BuildTools {
    private val AAPT_MD5 = when {
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> "C3928C2C3BFA403EA44BE8ED053E715B" // arm64
        else -> "E310A29F1D2709F2A2C880464A1D4198" // arm
    }
    private val ZIPALIGN_MD5 = when {
        Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> "E9D702460C70F65692ED3A2F2D4A9E7C" // arm64
        else -> "7FB621179486A620B7B2EE3713DDCADC" // arm
    }

    private fun getBinariesDir(context: Context) = File(context.dataDir, "bin")

    /**
     * Returns AAPT object in form of [File]
     */
    fun getAapt(context: Context) = File(getBinariesDir(context), "aapt")

    /**
     * Returns Zipalign object in form of [File]
     */
    fun getZipalign(context: Context) = File(getBinariesDir(context), "zipalign")

    /**
     * Extracts AAPT and Zipalign binaries and updates them if needed
     *
     * @param context Context
     * @param arm Set `true` if you want to support arm architecture
     * @param arm64 Set `true` if you want to support arm64 architecture
     * @return `true` if host architecture is supported and binary setup is succeeded
     */
    fun setup(context: Context, arm: Boolean, arm64: Boolean): Boolean {
        with(getBinariesDir(context)) {
            if (!exists()) {
                mkdirs()
            }
        }

        // Check if device arch is supported
        when {
            Build.SUPPORTED_ABIS.contains("x86") -> return false
            Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() && !arm64 -> return false
            Build.SUPPORTED_64_BIT_ABIS.isEmpty() && !arm -> return false
        }

        // Check and set AAPT
        with(getAapt(context)) {
            if (!exists() || !checkSum(this, AAPT_MD5)) {
                delete()
                val aaptRawInt = when {
                    Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> R.raw.aapt_arm64
                    else -> R.raw.aapt_arm
                }
                extract(context, aaptRawInt, this)
                setExecutable(true)
            }
        }

        // Check and set Zipalign
        with(getZipalign(context)) {
            if (!exists() || !checkSum(this, ZIPALIGN_MD5)) {
                delete()
                val zipalignRawInt = when {
                    Build.SUPPORTED_64_BIT_ABIS.isNotEmpty() -> R.raw.zipalign_arm64
                    else -> R.raw.zipalign_arm
                }
                extract(context, zipalignRawInt, this)
                setExecutable(true)
            }
        }
        return true
    }

    /**
     * Runs checksum to the file
     *
     * @param file [File] to check
     * @param reference Checksum to compare with
     * @return `true` if file checksum is equal with the reference
     */
    private fun checkSum(file: File, reference: String): Boolean {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("MD5")
        } catch (e: NoSuchAlgorithmException) {
            return false
        }
        FileInputStream(file).use { fis ->
            val buffer = ByteArray(8192)
            var read: Int = fis.read(buffer)
            return try {
                while (read > 0) {
                    digest.update(buffer, 0, read)
                    read = fis.read(buffer)
                }
                val md5sum = digest.digest()
                val bigInt = BigInteger(1, md5sum)
                var output = bigInt.toString(16)
                // Fill to 32 chars
                output = String.format("%32s", output).replace(' ', '0')
                TextUtils.equals(output, reference)
            } catch (e: IOException) {
                false
            }
        }
    }

    /**
     * Extracts files from raw resources
     *
     * @param context Context
     * @param res raw resource id to extract
     * @param target target file location
     */
    private fun extract(context: Context, res: Int, target: File) {
        context.resources.openRawResource(res).use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }
}
