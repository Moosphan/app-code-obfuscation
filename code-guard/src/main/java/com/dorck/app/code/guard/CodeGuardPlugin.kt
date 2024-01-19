package com.dorck.app.code.guard

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.task.GenRandomClassTask
import com.dorck.app.code.guard.transform.CodeGuardTransform
import com.dorck.app.code.guard.utils.DLogger
import com.dorck.app.code.guard.utils.android
import com.dorck.app.code.guard.utils.handleEachVariant
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
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

        val codeGuardTransform = CodeGuardTransform(extension, project)
        val curBuildVariant = extractBuildVariant(project)
        project.logger.error("get current build variant: $curBuildVariant")
        AppCodeGuardConfig.configCurrentBuildVariant(curBuildVariant)
        clearGenArtifactsWhenFailed(project)
        // Note: The plugin extension only initialized after `project.afterEvaluate` has been called, so we could not check configs here.
        // Recommended to use project properties.
        project.afterEvaluate {
            DLogger.debug = extension.logDebug
            // Initialize global configs after extension successfully created.
            AppCodeGuardConfig.configureFromExtension(extension)
            // Default code generation strategy.
            if (!extension.enable || CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)) {
                project.logger.info("CodeGuardPlugin is not enabled or never use default strategy.")
                return@afterEvaluate
            }
            // Register task for deleting generated classes.
            registerClearGenTask(project)
            // 基于preBuild任务时机来插入源码到指定`src/main/java`下，便于混淆代码参与到compile阶段
            val variants = HashSet<String>()
            project.handleEachVariant { variant ->
                DLogger.error("===========handle current variant: ${variant.name}")
                // 收集用户配置的所有变体
                variants.add(variant.name)
                AppCodeGuardConfig.configAvailableVariants(variants)
                // 1.如果用户配置了变体约束，需要根据变体判断是否执行
                // 2.若与当前正在构建的variant不是同一个，则跳过执行
                val variantRules = extension.variantConstraints
                if (!isHandleOnVariant(variantRules, variant.name)) {
                    DLogger.error("variant [${variant.name}] ignore processing, current build variant: $curBuildVariant, rules: $variantRules")
                    return@handleEachVariant
                }
                DLogger.info("Pre check finished, keep processing...")
                if (!extension.methodObfuscateEnable) {
                    DLogger.error("Method obfuscate not enable, skip gen classes.")
                    return@handleEachVariant
                }
                val preBuildTask = variant.preBuildProvider.get()
                // val preBuildTask = project.tasks.getByName("preBuild")
                logMessage("Found preBuild task: ${preBuildTask.name}")
                val createTaskName = "gen${variant.name.capitalize()}JavaTempClassTask"
                var existGenTask = project.tasks.findByName(createTaskName) as GenRandomClassTask?
                if (existGenTask == null) {
                    logMessage("Start exec gen task from first creating..")
                    existGenTask = project.tasks.create(createTaskName, GenRandomClassTask::class.java) {
                        outputs.upToDateWhen { false }
                    }
                    AppCodeGuardConfig.configJavaCodeGenMainDir(getGenClassBaseOutputDir(project))
                    val outputDir = createGenClassOutputMainDir()
                    // Configure java main output dir.
                    existGenTask.outputDir = outputDir
                } else {
                    logMessage("Start exec gen task from exist cache..")
                    existGenTask.generateClass()
                }
                preBuildTask.dependsOn(existGenTask)
                // 编译完成后需要将混淆类从源码目录删除(在compile之后)
                // Note: 需要确保 Transform 处理完后再删除
                // val packageTask = project.tasks.getByName("package${variant.name.capitalize()}")
                val transformTask = project.tasks.getByName("transformClassesWith${CodeGuardTransform.TRANSFORM_NAME}For${variant.name.capitalize()}")
                transformTask.doFirst {
                    if (name.contains("Debug")) {
                        AppCodeGuardConfig.currentTransformExecVariant = DEBUG_VARIANT
                    } else if (name.contains("Release")) {
                        AppCodeGuardConfig.currentTransformExecVariant = RELEASE_VARIANT
                    }
                }
                transformTask.doLast {
                    AppCodeGuardConfig.batchDeleteGenClass() {
                        // reset processing data
                        AppCodeGuardConfig.reset()
                    }
                }
            }

        }
        project.android().registerTransform(codeGuardTransform)
    }

    private fun isHandleOnVariant(configRules: HashSet<String>, variant: String): Boolean {
        val curBuildVariant = AppCodeGuardConfig.currentBuildVariant
        return configRules.isEmpty() || (configRules.contains(variant) && curBuildVariant == variant)
    }

    private fun clearGenArtifactsWhenFailed(project: Project) {
        project.gradle.addBuildListener(object : BuildListener {
            override fun settingsEvaluated(settings: Settings) {
            }

            override fun projectsLoaded(gradle: Gradle) {
            }

            override fun projectsEvaluated(gradle: Gradle) {
            }

            override fun buildFinished(result: BuildResult) {
                if (result.failure != null && AppCodeGuardConfig.hasGenClassesInLocal() && !AppCodeGuardConfig.isClearProcessing) {
                    DLogger.error("buildFinished, need to clear gen files.")
                    AppCodeGuardConfig.batchDeleteGenClass()
                }
            }

        })
    }

    private fun registerClearGenTask(project: Project) {
        DLogger.error("registerClearGenTask...")
        project.tasks.register("clearGenClasses") {
            group = "codeGuarder"
            if (AppCodeGuardConfig.hasGenClassesInLocal()) {
                AppCodeGuardConfig.batchDeleteGenClass()
            } else {
                DLogger.error("Has no gen classes in local: ${AppCodeGuardConfig.javaGenClassPaths}")
            }
        }
    }

    private fun createGenClassOutputMainDir(): File {
        val path = AppCodeGuardConfig.javaCodeGenMainDir
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun extractBuildVariant(project: Project): String {
        val taskRequests = project.gradle.startParameter.taskRequests
        // 默认为 release 下执行 (在执行assemble情况下)
        var buildVariant: String = RELEASE_VARIANT
        taskRequests.forEach {  taskExecutionRequest ->
            taskExecutionRequest?.run {
                args.forEach {
                    if (!it.isNullOrEmpty()) {
                        if (it.contains("Debug")) {
                            buildVariant = DEBUG_VARIANT
                        } else if (it.contains("Release")) {
                            buildVariant = RELEASE_VARIANT
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
        DLogger.error("[CodeGuardPlugin] >>> $message")
    }


    companion object {
        // 如果希望处理所有变体或未显式指定具体的variant(如 `assemble` task), 则可以指定该类型
        private const val ALL_VARIANT_TYPE = "all"
        // 目前暂时仅支持系统默认的两种buildType
        private const val DEBUG_VARIANT = "debug"
        private const val RELEASE_VARIANT = "release"
    }
}