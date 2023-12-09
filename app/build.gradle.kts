import kotlin.collections.*

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("cn.dorck.code.guarder")
}

android {
    namespace = "com.dorck.app.obfuscate"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.dorck.app.obfuscate"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
    enable = false
    variantConstraints = hashSetOf("debug")
    processingPackages = hashSetOf(
        "com.dorck.app.obfuscate.simple"
    )
}

dependencies {
    implementation("androidx.core:core-ktx:1.7.0")
    implementation(kotlin("stdlib-jdk8"))
    implementation("androidx.appcompat:appcompat:1.3.0")
    implementation( "com.google.android.material:material:1.4.0")
    implementation("androidx.constraintlayout:constraintlayout:2.0.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}