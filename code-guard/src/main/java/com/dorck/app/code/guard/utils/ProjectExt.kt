package com.dorck.app.code.guard.utils

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.api.BaseVariant
import org.gradle.api.Project
import org.gradle.kotlin.dsl.get
import java.io.File

/**
 * Obtain [AppExtension] or [LibraryExtension] according to current module type.
 */
fun Project.android(): BaseExtension =
    extensions.findByType(AppExtension::class.java)
        ?: extensions.findByType(LibraryExtension::class.java)
        ?: throw IllegalStateException("Unsupported extension type in this module: $name")

/**
 * Get package name from `applicationId`
 */
fun Project.getPackageName(): String? =
    android().defaultConfig.applicationId ?: android().namespace

fun Project.extractPackageFromSourceSet(): String? {
    val sourceSet = android().sourceSets
    val main = sourceSet["main"]
    return main.java.srcDirs.single().relativeTo(main.java.srcDirs.first()).path.replace(File.separator, ".")
}

fun Project.handleEachVariant(block: (variant: BaseVariant) -> Unit) {
    val app: AppExtension? = extensions.findByType(AppExtension::class.java)
    app?.let {
        DLogger.error("handleEachVariant, app type.")
        app.applicationVariants.forEach {
            block(it)
        }
        return
    }
    val library: LibraryExtension? = extensions.findByType(LibraryExtension::class.java)
    library?.let {
        DLogger.error("handleEachVariant, library type.")
        library.libraryVariants.forEach {
            block(it)
        }
        return
    }
    throw IllegalStateException("Unsupported variant operation in this module: $name")
}
