import kotlin.collections.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("cn.dorck.code.guarder") version "0.2.0-alpha"
}

android {
    namespace = "com.dorck.app.obfuscate"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.dorck.app.obfuscate"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
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
}

codeGuard {
    enable = true
    variantConstraints = hashSetOf("debug")
    processingPackages = hashSetOf(
        "com.dorck.app.obfuscate.simple",
        "com.dorck.code.sample.library.SimpleClassInLib"
    )
    genClassCount = 5
    isSkipJar = false
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation(project(":librarysample"))
}
