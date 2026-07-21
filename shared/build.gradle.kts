plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
    jvm("desktop")

    sourceSets {
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.components.resources)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.sqlite.bundled)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidInstrumentedTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.room.testing)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.test.runner)
        }
    }
}

android {
    namespace = "com.geepscontrol.shared"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    sourceSets.getByName("androidTest").assets.srcDir("$projectDir/schemas")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspDesktop", libs.androidx.room.compiler)
}

ksp { arg("room.schemaLocation", "$projectDir/schemas") }

// Compose Multiplatform 1.9 and KSP both contribute generated Kotlin sources.
// Gradle 8.13 requires their ordering to be explicit for Android variants.
val composeResourceTasks =
    listOf(
        "generateResourceAccessorsForAndroidMain",
        "generateActualResourceCollectorsForAndroidMain",
        "generateComposeResClass",
        "generateExpectResourceCollectorsForCommonMain",
        "generateResourceAccessorsForCommonMain",
    )

tasks.matching { it.name == "kspDebugKotlinAndroid" || it.name == "kspReleaseKotlinAndroid" }.configureEach {
    dependsOn(composeResourceTasks)
    if (name.contains("Debug")) dependsOn("generateResourceAccessorsForAndroidDebug")
    if (name.contains("Release")) dependsOn("generateResourceAccessorsForAndroidRelease")
}
