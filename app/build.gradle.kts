plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing comes from the environment (GitHub Actions secrets); keys are never committed.
val releaseKeystore: String? = System.getenv("PAISA_KEYSTORE_PATH")

android {
    namespace = "io.github.mkmanishkandel.paisaaayo"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.mkmanishkandel.paisaaayo"
        minSdk = 26
        targetSdk = 36
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"
        // Derived from the version so every release (and every rebuild of it) gets the same,
        // ever-increasing code that Google Play requires: 1.2.3 -> 10203.
        versionCode = versionCodeFor(versionName!!)
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("PAISA_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("PAISA_KEY_ALIAS")
                keyPassword = System.getenv("PAISA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        abortOnError = true
        checkDependencies = true
        warningsAsErrors = false
    }
    androidResources {
        // Ship only the languages the app is translated into (AndroidX brings ~80 more).
        localeFilters += listOf("en", "ne")
        // Voice clips are opened with AssetManager.openFd(), which needs them stored uncompressed.
        noCompress += listOf("ogg", "m4a", "mp3", "wav")
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = false
    }
    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":parser"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.animation)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.biometric)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

fun versionCodeFor(version: String): Int {
    val parts = version.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
    val (major, minor, patch) = (parts + listOf(0, 0, 0)).take(3)
    require(minor < 100 && patch < 100) { "Version $version: minor and patch must be below 100" }
    return major * 10_000 + minor * 100 + patch
}
