# prjkt's Android Commons

[![](https://jitpack.io/v/prjkt-io/android-commons.svg)](https://jitpack.io/#prjkt-io/android-commons)

[Docs](https://prjkt-io.github.io/android-commons/)

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}

dependencies {
    // Build tools (AAPT and Zipalign)
    implementation 'com.github.prjkt-io.android-commons:buildtools:[latest_version]'
    
    // Shell utils
    implementation 'com.github.prjkt-io.android-commons:shell:[latest_version]'
    
    // Theme app backend
    implementation 'com.github.prjkt-io.android-commons:theme:[latest_version]'
}
```

# Attention

If your app depends on Build Tools or Theme app backend and published using App Bundles (AAB), you need to add this to your project's `gradle.properties`
```
android.bundle.enableUncompressedNativeLibs=false
``` 