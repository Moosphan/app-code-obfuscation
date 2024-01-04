plugins {
    id("com.android.library")
    id("cn.dorck.code.guarder")
}

android {
    namespace = "com.dorck.code.sample.library"
    compileSdk = 32

    defaultConfig {
        minSdk = 21
        targetSdk = 32

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

codeGuard {
    enable = true
    variantConstraints = hashSetOf("debug")
    processingPackages = hashSetOf(
        "com.dorck.code.sample.library"
    )
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation("com.google.android.material:material:1.4.0")
}