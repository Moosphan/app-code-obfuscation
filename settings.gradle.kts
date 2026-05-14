pluginManagement {
    includeBuild("code-guard")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        mavenLocal()
    }
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
include(":librarysample")
