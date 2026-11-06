import org.gradle.jvm.toolchain.JavaLanguageVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

java { toolchain { languageVersion = JavaLanguageVersion.of(libs.versions.java.get().toInt()) } }

android {
    namespace = "dev.hdytrfli.omata"
    compileSdk = libs.versions.compile.sdk.get().toInt()
    buildToolsVersion = libs.versions.build.tools.get()

    defaultConfig {
        applicationId = "dev.hdytrfli.omata"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()

        versionCode = libs.versions.app.code.get().toInt()
        versionName = libs.versions.app.name.get()
    }

    signingConfigs {
        // Provide RELEASE_KEYSTORE_PATH / _PASSWORD / KEY_ALIAS / KEY_PASSWORD to
        // produce an installable signed release; otherwise the release APK is unsigned.
        val keystorePath = System.getenv("RELEASE_KEYSTORE_PATH")
        if (keystorePath != null) {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        named("debug") { applicationIdSuffix = ".debug" }
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Sideload-friendly build: minified like release, signed with the debug
        // keystore so it installs and updates over USB/adb without a release key.
        register("staging") {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            signingConfig =
                signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    // ML Kit ships native libraries for every ABI; ship one thin APK per device.
    splits {
        abi {
            isEnable = true
            reset()
            include(project.findProperty("targetAbi") as String? ?: "arm64-v8a")
            isUniversalApk = false
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

tasks.register("verifyReleaseTag") {
    val expectedVersion = providers.provider { libs.versions.app.name.get() }
    val actualTag = providers.gradleProperty("expectedTag")

    doLast {
        val tag = actualTag.orNull?.takeIf { it.isNotBlank() } ?: error("Missing -PexpectedTag")
        val expected = "v${expectedVersion.get()}"
        check(tag == expected) {
            "Release tag $tag does not match app-name $expected in gradle/libs.versions.toml"
        }
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Bundled, fully on-device OCR model (~4 MB of native libraries).
    implementation(libs.mlkit.text.recognition)
}
