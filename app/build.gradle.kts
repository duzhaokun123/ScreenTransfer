import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties
import java.io.ByteArrayOutputStream
import java.nio.file.Paths

val localProperties = gradleLocalProperties(rootDir)

plugins {
    id("com.android.application")
    kotlin("android")
    id("kotlin-android")
    id("dev.rikka.tools.refine") version "4.1.0"
}

android {
    val buildTime = System.currentTimeMillis()
    val baseVersionName = "0.1"

    compileSdk = 33

    defaultConfig {
        applicationId = "io.github.duzhaokun123.screentransfer"
        minSdk = 33
        targetSdk = 33
        versionCode = 1
        versionName = "$baseVersionName-git.$gitHash${if (isDirty) "-dirty" else ""}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("long", "BUILD_TIME", buildTime.toString())
    }
    externalNativeBuild {
        cmake {
            path = File(projectDir, "src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
//    packagingOptions {
//        resources.excludes.addAll(
//            arrayOf(
//                "META-INF/**",
//                "kotlin/**"
//            )
//        )
//    }
    signingConfigs {
        create("release") {
            storeFile = file("../releaseKey.jks")
            storePassword = System.getenv("REL_KEY")
            keyAlias = "key0"
            keyPassword = System.getenv("REL_KEY")
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = true
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
//            sourceSets.getByName("main").java.srcDir(File("build/generated/ksp/release/kotlin"))
        }
        getByName("debug") {
            val minifyEnabled = localProperties.getProperty("minify.enabled", "false")
            isMinifyEnabled = minifyEnabled.toBoolean()
            isShrinkResources = minifyEnabled.toBoolean()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
//            sourceSets.getByName("main").java.srcDir(File("build/generated/ksp/debug/kotlin"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        useK2 = true
    }
    buildFeatures {
        viewBinding = true
        aidl = true
    }
    lint {
        abortOnError = false
    }
    namespace = "io.github.duzhaokun123.screentransfer"
}

dependencies {
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.activity:activity-ktx:1.6.1")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //kotlinx-coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.6.4")

    compileOnly(project(":android-stub"))
    compileOnly("dev.rikka.hidden:stub:3.4.3")

    //never upgrade until new extension function
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")
    compileOnly("de.robv.android.xposed:api:82")

    implementation("com.google.code.gson:gson:2.10.1")

    //lifecycle
    val lifecycleVersion = "2.5.1"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    //ViewBindingUtil
    implementation("com.github.matsudamper:ViewBindingUtil:0.1")

    //FlexboxLayout
//    implementation("com.google.android.flexbox:flexbox:3.0.0")

    val ktor_version = "2.2.3"
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-cio:$ktor_version")
    implementation("io.ktor:ktor-server-websockets:$ktor_version")
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-websockets:$ktor_version")
}

val optimizeReleaseRes = task("optimizeReleaseRes").doLast {
    val aapt2 = Paths.get(
        project.android.sdkDirectory.path,
        "build-tools", project.android.buildToolsVersion, "aapt2"
    )
    val zip = Paths.get(
        project.buildDir.path, "intermediates",
        "optimized_processed_res", "release", "resources-release-optimize.ap_"
    )
    val optimized = File("${zip}.opt")
    val cmd = exec {
        commandLine(aapt2, "optimize", "--collapse-resource-names", "-o", optimized, zip)
        isIgnoreExitValue = true
    }
    if (cmd.exitValue == 0) {
        delete(zip)
        optimized.renameTo(zip.toFile())
    }
}
tasks.whenTaskAdded {
    when (name) {
        "optimizeReleaseResources" -> {
            finalizedBy(optimizeReleaseRes)
        }
    }
}

val gitHash: String
    get() {
        val out = ByteArrayOutputStream()
        val cmd = exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
            standardOutput = out
            isIgnoreExitValue = true
        }
        return if (cmd.exitValue == 0)
            out.toString().trim()
        else
            "(error)"
    }

val isDirty: Boolean
    get() {
        val out = ByteArrayOutputStream()
        exec {
            commandLine("git", "diff", "--stat")
            standardOutput = out
            isIgnoreExitValue = true
        }
        return out.size() != 0
    }
