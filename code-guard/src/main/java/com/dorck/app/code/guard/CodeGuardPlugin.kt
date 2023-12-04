package com.dorck.app.code.guard

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.task.GenRandomClassTask
import com.dorck.app.code.guard.transform.CodeGuardTransform
import com.dorck.app.code.guard.utils.KLogger
import com.dorck.app.code.guard.utils.android
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File

/**
 * A plugin for enhancing app code security by inserting random code.
 * @author Dorck
 * @since 2023/11/23
 */
class CodeGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        logMessage("[CodeGuardPlugin] => applying..")
        val extension = project.extensions.create("codeGuard", CodeGuardConfigExtension::class.java)

        val methodTraceTransform = CodeGuardTransform(extension, project)
        // Note: The plugin extension only initialized after `project.afterEvaluate` has been called, so we could not check configs here.
        // Recommended to use project properties.
        project.afterEvaluate {
            if (!extension.enable) {
                project.logger.info("CodeGuardPlugin is not enabled.")
                return@afterEvaluate
            }
            // 基于preBuild任务时机来插入源码到指定`src/main/java`下，便于混淆代码参与到compile阶段
            project.android().applicationVariants.forEach { variant ->
                val preBuildTask = variant.preBuildProvider.get()
                logMessage("Found preBuild task: ${preBuildTask.name}")
                val createTaskName = "gen${variant.name.capitalize()}JavaClassTask"
                var existGenTask = project.tasks.findByName(createTaskName)
                if (existGenTask == null) {
                    existGenTask = project.tasks.create(createTaskName, GenRandomClassTask::class.java)
                    AppCodeGuardConfig.configJavaCodeGenDir(getGenClassOutputPath(project))
                    existGenTask.outputDir = createGenClassOutputDir()
                }
                preBuildTask.dependsOn(existGenTask)
                // 编译完成后需要将混淆类从源码目录删除(在compile之后)
                val compileJavaTask = variant.javaCompileProvider.get()
                logMessage("Found compile task: ${compileJavaTask.name}")
                compileJavaTask.doLast {
                    logMessage("Start delete generated class.")
                    deleteGenClass()
                }
            }

        }
        project.android().registerTransform(methodTraceTransform)
    }

    private fun createGenClassOutputDir(): File {
        val path = AppCodeGuardConfig.javaCodeGenDir
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun deleteGenClass() {
        val genClassPath = AppCodeGuardConfig.javaCodeGenPath
        val genClassDir = File(genClassPath)
        if (genClassDir.exists()) {
            genClassDir.delete()
        }
    }

    private fun getGenClassOutputPath(project: Project): String =
        project.projectDir.absolutePath + "/src/main/java/"
    //project.the<SourceSetContainer>().getByName("main").allJava.sourceDirectories.singleFile.absolutePath

    private fun logMessage(message: String) {
        KLogger.error("[CodeGuardPlugin] >>> $message")
    }
}