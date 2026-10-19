plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.kover)
}

android {
    namespace = "com.phaprodevelop.klipsilo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.phaprodevelop.klipsilo"
        minSdk = 26
        targetSdk = 36
        versionCode = 11000
        versionName = "1.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // Release signing comes from deploytool (DEPLOYTOOL_SIGNING_*). Without
    // those variables the release build stays unsigned, as before.
    val signingStore = System.getenv("DEPLOYTOOL_SIGNING_STORE_FILE")
    signingConfigs {
        if (!signingStore.isNullOrBlank()) {
            create("release") {
                storeFile = file(signingStore)
                storePassword = System.getenv("DEPLOYTOOL_SIGNING_STORE_PASSWORD")
                keyAlias = System.getenv("DEPLOYTOOL_SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("DEPLOYTOOL_SIGNING_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (!signingStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        // Side-by-side with the Play app. applicationIdSuffix keeps providers,
        // FileProvider and the share target on ${applicationId}. Release stays
        // com.phaprodevelop.klipsilo / 1.1.0 (versionCode 11000).
        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            matchingFallbacks += listOf("release")
            if (!signingStore.isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    bundle {
        language {
            enableSplit = false
        }
    }
    androidResources {
        generateLocaleConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    sourceSets {
        getByName("test").assets.srcDir("$projectDir/schemas")
    }
}

// Beta versionName follows a tag such as v1.2.0-beta.1.
// versionCode = major*10000 + minor*1000 + patch*100 + betaNumber - 100,
// so 1.1.0 is 11000 and 1.2.0-beta.1 is 11901 (above the Play build, below 1.2.0).
// DEPLOYTOOL_ARTIFACT_REF wins, because a deploy unpacks the tag with git archive
// and that tree has no .git. Otherwise git describe is used. A checkout that is
// not on a beta tag (local main before the tag exists) falls back to 1.2.0-beta.6.
val betaVersionNameProvider =
    providers.environmentVariable("DEPLOYTOOL_ARTIFACT_REF").orElse(
        providers.exec {
            commandLine("git", "describe", "--tags", "--always", "--match", "v[0-9]*")
            workingDir(rootProject.layout.projectDirectory)
            isIgnoreExitValue = true
        }.standardOutput.asText,
    ).map { raw -> betaVersionName(raw) }

fun betaVersionName(raw: String): String {
    val match = Regex("""^v?(\d+\.\d+\.\d+)-beta\.(\d+)""").find(raw.trim())
    return if (match != null) {
        "${match.groupValues[1]}-beta.${match.groupValues[2]}"
    } else {
        "1.2.0-beta.6"
    }
}

fun betaVersionCode(versionName: String): Int {
    val match = Regex("""(\d+)\.(\d+)\.(\d+)-beta\.(\d+)""").find(versionName) ?: return 11906
    val major = match.groupValues[1].toInt()
    val minor = match.groupValues[2].toInt()
    val patch = match.groupValues[3].toInt()
    val beta = match.groupValues[4].toInt().coerceAtLeast(1)
    val base = major * 10000 + minor * 1000 + patch * 100
    return (base - 100 + beta).coerceAtLeast(11000)
}

androidComponents {
    onVariants(selector().withBuildType("beta")) { variant ->
        val code = betaVersionNameProvider.map { betaVersionCode(it) }
        variant.outputs.forEach { output ->
            output.versionName.set(betaVersionNameProvider)
            output.versionCode.set(code)
        }
    }
    // Debug builds get the base version plus the git short hash, e.g.
    // 1.2.0-beta.1-debug+abc1234. A tree without .git (git archive / deploy) keeps
    // the plain base name.
    onVariants(selector().withBuildType("debug")) { variant ->
        val hash =
            providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
                workingDir(rootProject.layout.projectDirectory)
                isIgnoreExitValue = true
            }.standardOutput.asText.map { it.trim() }
        variant.outputs.forEach { output ->
            output.versionName.set(
                betaVersionNameProvider
                    .zip(hash) { base, shortHash ->
                        if (shortHash.isBlank()) base else "$base-debug+$shortHash"
                    },
            )
        }
    }
}

kapt {
    arguments {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.foundation:foundation")
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.commonmark)
    implementation(libs.commonmark.gfm.strikethrough)
    implementation(libs.pdfbox.android)
    implementation("androidx.exifinterface:exifinterface:1.3.7")
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("androidx.camera:camera-camera2:1.4.0")
    implementation("androidx.camera:camera-lifecycle:1.4.0")
    implementation("androidx.camera:camera-view:1.4.0")

    debugImplementation(libs.androidx.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    // Compose UI tests run on Robolectric (no emulator in this round, #79).
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation("androidx.compose.ui:ui-test-junit4")
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.arch.core.testing)
    testImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

ktlint {
    filter {
        exclude { element -> element.file.path.contains("/build/") }
    }
}
