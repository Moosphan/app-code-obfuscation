package com.dorck.app.code.guard.utils

import com.android.build.gradle.AppExtension
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import java.io.File

/**
 * Obtain app module(com.android.application) like this:
 * ```
 * android {
 *  buildTypes {}
 *  defaultConfig {}
 * }
 * ```
 */
fun Project.android(): AppExtension =
    extensions.getByType(AppExtension::class.java)

/**
 * Get package name from `applicationId`
 */
fun Project.getPackageName(): String? =
    android().defaultConfig.applicationId

fun Project.extractPackageFromSourceSet(): String? {
    val sourceSet = the<SourceSetContainer>()
    val main = sourceSet["main"]
    return main.allJava.single().parentFile.relativeTo(main.java.srcDirs.first()).path.replace(File.separator, ".")
}
