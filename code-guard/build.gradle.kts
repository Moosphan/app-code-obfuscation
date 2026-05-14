plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    signing
    id("com.gradle.plugin-publish") version "1.0.0-rc-1"
    // Only used for local testing.
//    id("cn.dorck.component.publisher") version "1.0.4"
}

repositories {
    google()
    mavenCentral()
    maven(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
}

// Load and configure secrets of publication from system env.
ext["signing.keyId"] = System.getenv("GPG_KEY_ID")
ext["signing.password"] = System.getenv("GPG_PASSWORD")
ext["signing.secretKeyRingFile"] = System.getenv("GPG_SECRET_KEY_RING_FILE")
ext["gradle.publish.key"] = System.getenv("GRADLE_PUBLISH_KEY")
ext["gradle.publish.secret"] = System.getenv("GRADLE_PUBLISH_SECRET")
ext["ossrh.username"] = System.getenv("OSSRH_USERNAME")
ext["ossrh.password"] = System.getenv("OSSRH_PASSWORD")

group = PluginInfo.group
version = PluginInfo.version

/*publishOptions {
    group = PluginInfo.group
    version = PluginInfo.version
    artifactId = PluginInfo.artifactId
    description = PluginInfo.description
}*/

pluginBundle {
    website = "https://github.com/Moosphan/app-code-obfuscation"
    vcsUrl = "https://github.com/Moosphan/app-code-obfuscation.git"
    tags = listOf("code obfuscation", "proguard", "bytecode enhancement", "Apk obfuscate")
}

gradlePlugin {
    plugins {
        create(PluginInfo.name) {
            // Note: We need to make here id as same as module name, or
            // it will publish two different plugins.
            id = PluginInfo.id
            implementationClass = PluginInfo.implementationClass
            displayName = PluginInfo.displayName
            description = PluginInfo.description
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Use gradle api
    implementation(gradleApi())
    // Use kotlin-stdlib without version to be compatible with the project's Kotlin version
    compileOnly(kotlin("stdlib"))
    implementation("commons-io:commons-io:2.11.0")
    // ASM 9.0+ required for AGP 8.0+ (Opcodes.ASM9)
    compileOnly("org.ow2.asm:asm:9.4")
    compileOnly("org.ow2.asm:asm-commons:9.4")
    // AGP 8.0 for AsmClassVisitorFactory / Instrumentation API
    compileOnly("com.android.tools.build:gradle:8.0.0")
    implementation("com.android.tools:common:30.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Test dependencies
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.ow2.asm:asm:9.4")
    testImplementation("org.ow2.asm:asm-commons:9.4")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("pluMaven") {
                group = PluginInfo.group
                artifactId = PluginInfo.artifactId
                version = PluginInfo.version
                from(components["java"])

                pom {
                    name.set(PluginInfo.artifactId)
                    description.set(PluginInfo.description)
                    url.set(PluginInfo.url)

                    scm {
                        connection.set(PluginInfo.scmConnection)
                        developerConnection.set(PluginInfo.scmConnection)
                        url.set(PluginInfo.url)
                    }

                    licenses {
                        license {
                            name.set("The Apache Software License, Version 2.0")
                            url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                            distribution.set("repo")
                        }
                    }

                    developers {
                        developer {
                            name.set(PluginInfo.developer)
                            email.set(PluginInfo.email)
                        }
                    }
                }
            }
        }
        repositories {
            maven {
                val isLocal = version.toString().endsWith("LOCAL")
                val repoUrl = if (version.toString().endsWith("SNAPSHOT")) {
                    PluginInfo.SNAPSHOT_URL
                } else if (isLocal) {
                    layout.buildDirectory.dir("repos/locals").get().asFile.path
                } else {
                    PluginInfo.RELEASE_URL
                }
                url = uri(repoUrl)
                if (!isLocal) {
                    credentials {
                        username = this@afterEvaluate.ext["ossrh.username"].toString()
                        password = this@afterEvaluate.ext["ossrh.password"].toString()
                    }
                }
            }
        }
    }
}

object PluginInfo {
    const val id = "cn.dorck.code.guarder"
    const val name = "CodeGuardPlugin"
    const val group = "cn.dorck"
    const val artifactId = "code-guard-plugin"
    const val implementationClass = "com.dorck.app.code.guard.CodeGuardPlugin"
    const val version = "0.2.0-beta"
//    const val version = "0.1.1-LOCAL"
    const val displayName = "CodeGuardPlugin"
    const val description = "A plugin for code obfuscation."
    const val url = "https://github.com/Moosphan/app-code-obfuscation"
    const val scmConnection = "scm:git@github.com:Moosphan/app-code-obfuscation.git"
    const val developer = "Dorck"
    const val email = "moosphon@gmail.com"
    const val SNAPSHOT_URL = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
    const val RELEASE_URL = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
}