pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        mavenLocal()
    }

    /*resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "cn.dorck.code.guarder") {
                useModule("cn.dorck:code-guard-plugin:0.1.1-LOCAL")
//                useModule("cn.dorck.android:code-guard-plugin:0.0.1-SNAPSHOT")
            }
        }
    }*/
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        mavenLocal()
    }
}

rootProject.name = "code-obfuscation"
include(":app")
include(":code-guard")
include(":librarysample")
