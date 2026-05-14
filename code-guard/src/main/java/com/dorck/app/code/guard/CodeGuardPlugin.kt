package com.dorck.app.code.guard

import com.dorck.app.code.guard.agp8.Agp8Compat
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
 * Supports both AGP 7.x (Transform API) and AGP 8.0+ (Instrumentation API).
 * @author Dorck
 * @since 2023/11/23
 */
class CodeGuardPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        logMessage("[CodeGuardPlugin] => applying..")
        val extension = project.extensions.create("codeGuard", CodeGuardConfigExtension::class.java)

        val curBuildVariant = extractBuildVariant(project)
        project.logger.error("get current build variant: $curBuildVariant")
        AppCodeGuardConfig.configCurrentBuildVariant(curBuildVariant)
        clearGenArtifactsWhenFailed(project)

        if (Agp8Compat.isAgp8OrHigher()) {
            // AGP 8.0+ approach: use AsmClassVisitorFactory (Instrumentation API)
            logMessage("Detected AGP 8.0+, using AsmClassVisitorFactory approach.")
            registerAgp8Approach(project, extension)
        } else {
            // AGP 7.x approach: use Transform API (deprecated in AGP 8.0)
            logMessage("Detected AGP 7.x, using Transform API approach.")
            registerTransformApproach(project, extension)
        }
    }

    // ==================== AGP 8.0+ Approach ====================

    private fun registerAgp8Approach(project: Project, extension: CodeGuardConfigExtension) {
        // Register AsmClassVisitorFactory (must be called before afterEvaluate)
        Agp8Compat.register(project, extension)

        // Note: The plugin extension only initialized after `project.afterEvaluate` has been called.
        project.afterEvaluate {
            DLogger.debug = extension.logDebug
            AppCodeGuardConfig.configureFromExtension(extension)

            if (!extension.enable || CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)) {
                project.logger.info("CodeGuardPlugin is not enabled or never use default strategy.")
                return@afterEvaluate
            }

            registerClearGenTask(project)
            registerReadConfigTask(project)

            // 将生成目录添加到源码集，避免污染 src/main/java
            val genDir = File(project.buildDir, "generated/codeguard/java")
            project.android().sourceSets.getByName("main").java.srcDir(genDir)

            // Register GenRandomClassTask for each variant
            val variants = HashSet<String>()
            project.handleEachVariant { variant ->
                DLogger.error("===========handle current variant: ${variant.name}")
                variants.add(variant.name)
                AppCodeGuardConfig.configAvailableVariants(variants)

                val variantRules = extension.variantConstraints
                if (!isHandleOnVariant(variantRules, variant.name)) {
                    DLogger.error("variant [${variant.name}] ignore processing, current build variant: ${AppCodeGuardConfig.currentBuildVariant}, rules: $variantRules")
                    return@handleEachVariant
                }

                DLogger.info("Pre check finished, keep processing...")

                if (!extension.methodObfuscateEnable) {
                    DLogger.error("Method obfuscate not enable, skip gen classes.")
                    return@handleEachVariant
                }

                val preBuildTask = variant.preBuildProvider.get()
                logMessage("Found preBuild task: ${preBuildTask.name}")

                val createTaskName = "gen${variant.name.capitalize()}JavaTempClassTask"
                var existGenTask = project.tasks.findByName(createTaskName) as GenRandomClassTask?
                if (existGenTask == null) {
                    logMessage("Start exec gen task from first creating..")
                    existGenTask = project.tasks.create(createTaskName, GenRandomClassTask::class.java) {
                        outputs.upToDateWhen { false }
                    }
                    AppCodeGuardConfig.configJavaCodeGenMainDir(getGenClassBaseOutputDir(project))
                    val outputDir = createGenClassOutputMainDir(project)
                    existGenTask.outputDir = outputDir
                } else {
                    logMessage("Start exec gen task from exist cache..")
                    existGenTask.generateClass()
                }
                preBuildTask.dependsOn(existGenTask)
            }
        }
    }

    // ==================== AGP 7.x Approach (Transform API) ====================

    private fun registerTransformApproach(project: Project, extension: CodeGuardConfigExtension) {
        val codeGuardTransform = CodeGuardTransform(extension, project)
        val curBuildVariant = AppCodeGuardConfig.currentBuildVariant
        project.afterEvaluate {
            DLogger.debug = extension.logDebug
            AppCodeGuardConfig.configureFromExtension(extension)

            if (!extension.enable || CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)) {
                project.logger.info("CodeGuardPlugin is not enabled or never use default strategy.")
                return@afterEvaluate
            }

            registerClearGenTask(project)
            registerReadConfigTask(project)

            val variants = HashSet<String>()
            project.handleEachVariant { variant ->
                DLogger.error("===========handle current variant: ${variant.name}")
                variants.add(variant.name)
                AppCodeGuardConfig.configAvailableVariants(variants)

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
                logMessage("Found preBuild task: ${preBuildTask.name}")

                val createTaskName = "gen${variant.name.capitalize()}JavaTempClassTask"
                var existGenTask = project.tasks.findByName(createTaskName) as GenRandomClassTask?
                if (existGenTask == null) {
                    logMessage("Start exec gen task from first creating..")
                    existGenTask = project.tasks.create(createTaskName, GenRandomClassTask::class.java) {
                        outputs.upToDateWhen { false }
                    }
                    AppCodeGuardConfig.configJavaCodeGenMainDir(getGenClassBaseOutputDir(project))
                    val outputDir = createGenClassOutputMainDir(project)
                    existGenTask.outputDir = outputDir
                } else {
                    logMessage("Start exec gen task from exist cache..")
                    existGenTask.generateClass()
                }
                preBuildTask.dependsOn(existGenTask)

                val transformTask = project.tasks.getByName("transformClassesWith${CodeGuardTransform.TRANSFORM_NAME}For${variant.name.capitalize()}")
                transformTask.doFirst {
                    if (name.contains("Debug")) {
                        AppCodeGuardConfig.currentTransformExecVariant = DEBUG_VARIANT
                    } else if (name.contains("Release")) {
                        AppCodeGuardConfig.currentTransformExecVariant = RELEASE_VARIANT
                    }
                }
                transformTask.doLast {
                    DLogger.error("Print configs before clear gen artifacts: ${AppCodeGuardConfig.getAllConfigs()}")
                    AppCodeGuardConfig.batchDeleteGenClass() {
                        AppCodeGuardConfig.reset()
                    }
                }
            }
        }
        project.android().registerTransform(codeGuardTransform)
    }

    // ==================== Common Methods ====================

    private fun isHandleOnVariant(configRules: HashSet<String>, variant: String): Boolean {
        val curBuildVariant = AppCodeGuardConfig.currentBuildVariant
        return configRules.isEmpty() || (configRules.contains(variant) && curBuildVariant == variant)
    }

    private fun clearGenArtifactsWhenFailed(project: Project) {
        project.gradle.addBuildListener(object : BuildListener {
            override fun settingsEvaluated(settings: Settings) {}

            override fun projectsLoaded(gradle: Gradle) {}

            override fun projectsEvaluated(gradle: Gradle) {}

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

    private fun registerReadConfigTask(project: Project) {
        DLogger.error("registerReadConfigTask...")
        project.tasks.register("readGuarderConfigs") {
            group = "codeGuarder"
            project.logger.error("[CodeGuardPlugin] >> read configs: ${AppCodeGuardConfig.getAllConfigs()}")
        }
    }

    private fun createGenClassOutputMainDir(project: Project): File {
        // 生成到 build 目录下，避免污染源码目录
        val dir = File(project.buildDir, "generated/codeguard/java")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun extractBuildVariant(project: Project): String {
        val taskRequests = project.gradle.startParameter.taskRequests
        var buildVariant: String = RELEASE_VARIANT
        taskRequests.forEach { taskExecutionRequest ->
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
        File(project.buildDir, "generated/codeguard/java").absolutePath

    private fun logMessage(message: String) {
        DLogger.error("[CodeGuardPlugin] >>> $message")
    }

    companion object {
        private const val ALL_VARIANT_TYPE = "all"
        private const val DEBUG_VARIANT = "debug"
        private const val RELEASE_VARIANT = "release"
    }
}
