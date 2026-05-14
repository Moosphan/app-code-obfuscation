plugins {
    id("com.android.library")
//    id("cn.dorck.code.guarder") version "0.2.0-alpha"
}

android {
    namespace = "com.dorck.code.sample.library"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
        targetSdk = 34

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

//codeGuard {
//    enable = true
//    variantConstraints = hashSetOf("debug")
//    processingPackages = hashSetOf(
//        "com.dorck.code.sample.library"
//    )
//}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
}
