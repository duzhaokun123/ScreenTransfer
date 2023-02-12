plugins {
    id("com.android.library")
}

android {
    compileSdk = 33

    defaultConfig {
        minSdk = 31
        targetSdk = 33

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    namespace = "io.github.duzhaokun123.android_stub"
}

dependencies {
    annotationProcessor("dev.rikka.tools.refine:annotation-processor:4.1.0")
    compileOnly("dev.rikka.tools.refine:annotation:4.1.0")
    compileOnly("androidx.annotation:annotation:1.5.0")
    compileOnly("dev.rikka.hidden:stub:3.4.3")
}
