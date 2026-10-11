plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

// Spec F00 (SF00-R13): the release is signed only when the four credentials exist and are not blank.
// They live outside the repository, in ~/.gradle/gradle.properties (Spec 01, "Firma de los APK").
val releaseSigning = listOf(
    "WEIGHTRACK_RELEASE_STORE_FILE",
    "WEIGHTRACK_RELEASE_STORE_PASSWORD",
    "WEIGHTRACK_RELEASE_KEY_ALIAS",
    "WEIGHTRACK_RELEASE_KEY_PASSWORD",
).associateWith { providers.gradleProperty(it).orNull?.takeIf(String::isNotBlank) }
val hasReleaseSigning = releaseSigning.values.all { it != null }

android {
    namespace = "com.juancholopes.weightrack"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.juancholopes.weightrack"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigning.getValue("WEIGHTRACK_RELEASE_STORE_FILE")!!)
                storePassword = releaseSigning.getValue("WEIGHTRACK_RELEASE_STORE_PASSWORD")
                keyAlias = releaseSigning.getValue("WEIGHTRACK_RELEASE_KEY_ALIAS")
                keyPassword = releaseSigning.getValue("WEIGHTRACK_RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Installs next to the release as com.juancholopes.weightrack.debug (S03-R07, Spec F00 SF00-R12).
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    // Spec 31 (D4): repository fakes and their shared contract tests, visible from test and androidTest.
    testFixtures {
        enable = true
    }
}

room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.navigationevent.compose)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.framework)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.core)
    ksp(libs.androidx.room3.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testFixturesImplementation(libs.junit)
    testFixturesImplementation(libs.kotlinx.coroutines.core)
    testFixturesImplementation(libs.kotlinx.coroutines.test)
    // The Compose compiler plugin runs on every Kotlin compilation of :app and needs the runtime (Spec 31).
    testFixturesImplementation(platform(libs.androidx.compose.bom))
    testFixturesImplementation(libs.androidx.compose.runtime)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room3.testing)
    androidTestImplementation(libs.kotlinx.coroutines.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}