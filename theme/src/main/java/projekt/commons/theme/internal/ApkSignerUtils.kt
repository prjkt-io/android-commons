package projekt.commons.theme.internal

import android.content.Context
import android.os.Build
import com.android.apksig.ApkSigner
import projekt.commons.theme.R
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey
import java.security.cert.X509Certificate
import java.util.ArrayList

fun signApk(context: Context, input: File, output: File, minSdkVersion: Int = Build.VERSION.SDK_INT): Boolean {
    return try {
        context.resources.openRawResource(R.raw.key).use { signKey ->
            val keyPass = "overlay".toCharArray()
            val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
            keyStore.load(signKey, keyPass)
            val privateKey = keyStore.getKey("key", keyPass) as PrivateKey
            val certs = ArrayList<X509Certificate>()
            certs.add(keyStore.getCertificateChain("key")[0] as X509Certificate)
            val signerConfig = ApkSigner.SignerConfig.Builder("overlay", privateKey, certs).build()
            val signerConfigs = ArrayList<ApkSigner.SignerConfig>()
            signerConfigs.add(signerConfig)
            val apkSigner = ApkSigner.Builder(signerConfigs)
            apkSigner.setV1SigningEnabled(false)
                    .setV2SigningEnabled(true)
                    .setV3SigningEnabled(true)
                    .setInputApk(input)
                    .setOutputApk(output)
                    .setMinSdkVersion(minSdkVersion)
                    .build()
                    .sign()
        }
        true
    } catch (e: Exception) {
        false
    }
}
