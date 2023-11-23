package com.dorck.app.code.guard

import com.dorck.app.code.guard.transform.CodeGuardTransform
import com.dorck.app.code.guard.utils.android
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.LogLevel

/**
 * A plugin for enhancing app code security by inserting random code.
 * @author Dorck
 * @since 2023/11/23
 */
class CodeGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.log(LogLevel.ERROR, "[CodeGuardPlugin] => applying..")
        val extension = project.extensions.create("codeGuard", CodeGuardConfigExtension::class.java)

        val methodTraceTransform = CodeGuardTransform(extension, project)
        // Note: The plugin extension only initialized after `project.afterEvaluate` has been called, so we could not check configs here.
        project.android().registerTransform(methodTraceTransform)
    }
}