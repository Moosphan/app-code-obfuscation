package com.dorck.app.code.guard.utils

import com.android.build.gradle.AppExtension
import org.gradle.api.Project

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