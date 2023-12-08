package com.dorck.app.code.guard

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.task.GenRandomClassTask
import com.dorck.app.code.guard.transform.CodeGuardTransform
import com.dorck.app.code.guard.utils.IOUtils
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
        val curBuildVariant = extractBuildVariant(project)
        KLogger.error("get current build variant: $curBuildVariant")
        AppCodeGuardConfig.configCurrentBuildVariant(curBuildVariant)
        // Note: The plugin extension only initialized after `project.afterEvaluate` has been called, so we could not check configs here.
        // Recommended to use project properties.
        project.afterEvaluate {
            // Initialize global configs after extension successfully created.
            AppCodeGuardConfig.configureFromExtension(extension)
            // Default code generation strategy.
            if (!extension.enable || CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)) {
                project.logger.info("CodeGuardPlugin is not enabled or never use default strategy.")
                return@afterEvaluate
            }
            // 基于preBuild任务时机来插入源码到指定`src/main/java`下，便于混淆代码参与到compile阶段
            val variants = HashSet<String>()
            project.android().applicationVariants.forEach { variant ->
                // 收集用户配置的所有变体
                variants.add(variant.name)
                AppCodeGuardConfig.configAvailableVariants(variants)
//                // 如果当前构建的变体和目前不是一个则跳过本次执行
//                if (curBuildVariant != variant.name) {
//                    return@forEach
//                }
                // 如果用户配置了变体约束，需要根据变体判断是否执行
                val variantRules = extension.excludeRules
                if (variantRules.isNotEmpty() && !variantRules.contains(variant.name) || curBuildVariant != variant.name) {
                    return@afterEvaluate
                }
                val preBuildTask = variant.preBuildProvider.get()
//                val preBuildTask = project.tasks.getByName("preBuild")
                logMessage("Found preBuild task: ${preBuildTask.name}")
                val createTaskName = "gen${variant.name.capitalize()}JavaTempClassTask"
                var existGenTask = project.tasks.findByName(createTaskName)
                if (existGenTask == null) {
                    existGenTask = project.tasks.create(createTaskName, GenRandomClassTask::class.java)
                    AppCodeGuardConfig.configJavaCodeGenMainDir(getGenClassBaseOutputDir(project))
                    val outputDir = createGenClassOutputMainDir()
                    // Configure java main output dir.
                    existGenTask.outputDir = outputDir
                }
                preBuildTask.dependsOn(existGenTask)
                // 编译完成后需要将混淆类从源码目录删除(在compile之后)
                /*val compileJavaTask = variant.javaCompileProvider.get()
                logMessage("Found compile task: ${compileJavaTask.name}")*/
                // Note: 需要确保 Transform 处理完后再删除
//                val transformTask = project.tasks.getByName("package${variant.name.capitalize()}")
                val transformTask = project.tasks.getByName("transformClassesWith${CodeGuardTransform.TRANSFORM_NAME}For${variant.name.capitalize()}")
                transformTask.doLast {
                    val isPackageExist = AppCodeGuardConfig.isPkgExist ?: false
                    logMessage("Start delete generated class, pkg exist: $isPackageExist, genPkgName: ${AppCodeGuardConfig.genClassPkgName}")
                    deleteGenClass(isPackageExist)
                }
            }

        }
        project.android().registerTransform(methodTraceTransform)
    }

    private fun createGenClassOutputMainDir(): File {
        val path = AppCodeGuardConfig.javaCodeGenMainDir
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun deleteGenClass(isPkgExist: Boolean) {
        // Note: 如果包名之前不存在，需要将创建的包目录也一并删除(获取子包名的第一个目录)
        if (isPkgExist) {
            val classPath = AppCodeGuardConfig.javaCodeGenPath
            val genClassFile = File(classPath)
            if (genClassFile.exists()) {
                genClassFile.delete()
            }
            KLogger.error("deleteGenClass, path: $classPath")
        } else {
            val deleteDir = getDeleteDir()
            KLogger.error("deleteGenClass, dir: $deleteDir")
            val genClassDir = File(deleteDir)
            if (genClassDir.exists()) {
                IOUtils.deleteDirectory(genClassDir)
            }
        }
    }

    private fun getDeleteDir(): String {
        val mainDir = AppCodeGuardConfig.javaCodeGenMainDir
        val applicationId = AppCodeGuardConfig.applicationId
        val genPkg = AppCodeGuardConfig.genClassPkgName
        val temp = genPkg.replace(applicationId, "")
        val baseDir = applicationId + "." + temp.split(".")[1]
        return mainDir + baseDir.replace(".", "/") + "/"
    }

    private fun extractBuildVariant(project: Project): String {
        val taskRequests = project.gradle.startParameter.taskRequests
        var buildVariant: String = ""
        taskRequests.forEach {  taskExecutionRequest ->
            taskExecutionRequest?.run {
                args.forEach {
                    if (!it.isNullOrEmpty()) {
                        if (it.contains("Debug")) {
                            buildVariant = "Debug"
                        } else if (it.contains("Release")) {
                            buildVariant = "Release"
                        }
                    }
                }
            }
        }
        return buildVariant
    }

    private fun getGenClassBaseOutputDir(project: Project): String =
        project.projectDir.absolutePath + "/src/main/java/"
    //project.the<SourceSetContainer>().getByName("main").allJava.sourceDirectories.singleFile.absolutePath

    private fun logMessage(message: String) {
        KLogger.error("[CodeGuardPlugin] >>> $message")
    }
}