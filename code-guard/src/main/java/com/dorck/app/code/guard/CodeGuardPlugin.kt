package com.dorck.app.code.guard

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.task.GenRandomClassTask
import com.dorck.app.code.guard.transform.CodeGuardTransform
import com.dorck.app.code.guard.utils.IOUtils
import com.dorck.app.code.guard.utils.DLogger
import com.dorck.app.code.guard.utils.android
import com.dorck.app.code.guard.utils.handleEachVariant
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

        val codeGuardTransform = CodeGuardTransform(extension, project)
        val curBuildVariant = extractBuildVariant(project)
        DLogger.error("get current build variant: $curBuildVariant")
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
            project.handleEachVariant { variant ->
                DLogger.error("===========handle current variant: ${variant.name}")
                // 收集用户配置的所有变体
                variants.add(variant.name)
                AppCodeGuardConfig.configAvailableVariants(variants)
                // 1.如果用户配置了变体约束，需要根据变体判断是否执行
                // 2.若与当前正在构建的variant不是同一个，则跳过执行
                val variantRules = extension.variantConstraints
                if (variantRules.isNotEmpty() && !variantRules.contains(variant.name) || curBuildVariant != variant.name) {
                    DLogger.error("variant [${variant.name}] ignore processing, current build variant: $curBuildVariant, rules: $variantRules")
                    return@handleEachVariant
                }
                DLogger.info("Pre check finished, keep processing...")
                val preBuildTask = variant.preBuildProvider.get()
                // val preBuildTask = project.tasks.getByName("preBuild")
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
                // Note: 需要确保 Transform 处理完后再删除
                // val packageTask = project.tasks.getByName("package${variant.name.capitalize()}")
                val transformTask = project.tasks.getByName("transformClassesWith${CodeGuardTransform.TRANSFORM_NAME}For${variant.name.capitalize()}")
                transformTask.doLast {
//                    val isPackageExist = AppCodeGuardConfig.isPkgExist ?: false
//                    logMessage("Start delete generated class, pkg exist: $isPackageExist, genPkgName: ${AppCodeGuardConfig.genClassPkgName}")
//                    deleteGenClass(isPackageExist)
                    batchDeleteGenClass()
                }
            }

        }
        project.android().registerTransform(codeGuardTransform)
    }

    private fun createGenClassOutputMainDir(): File {
        val path = AppCodeGuardConfig.javaCodeGenMainDir
        val dir = File(path)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /*private fun deleteGenClass(isPkgExist: Boolean) {
        // Note: 如果包名之前不存在，需要将创建的包目录也一并删除(获取子包名的第一个目录)
        if (isPkgExist) {
            val classPath = AppCodeGuardConfig.javaCodeGenPath
            val genClassFile = File(classPath)
            if (genClassFile.exists()) {
                genClassFile.delete()
            }
            DLogger.error("deleteGenClass, path: $classPath")
        } else {
            val deleteDir = getDeleteDir()
            DLogger.error("deleteGenClass, dir: $deleteDir")
            val genClassDir = File(deleteDir)
            if (genClassDir.exists()) {
                IOUtils.deleteDirectory(genClassDir)
            }
        }
    }*/

    private fun batchDeleteGenClass() {
        // Note: 如果包名之前不存在，需要将创建的包目录也一并删除(获取子包名的第一个目录)
        val genClassPaths = AppCodeGuardConfig.javaGenClassPaths
        DLogger.info("batchDeleteGenClass, gen classes: $genClassPaths")
        genClassPaths.forEach {
            val key = extractPackageAndClassName(it.classPath)
            val pgkExist = AppCodeGuardConfig.packageExistStates[key] ?: false
            DLogger.error("batchDeleteGenClass, key => $key is exist: $pgkExist")
            deleteGenClass(pgkExist, it)
        }
    }

    private fun deleteGenClass(isPkgExist: Boolean, classBean: AppCodeGuardConfig.GenClassData) {
        // Note: 如果包名之前不存在，需要将创建的包目录也一并删除(获取子包名的第一个目录)
        if (isPkgExist) {
            val genClassFile = File(classBean.classPath)
            if (genClassFile.exists()) {
                genClassFile.delete()
            }
            DLogger.error("deleteGenClass, path: ${classBean.classPath}")
        } else {
            val deleteDir = getDeleteDir(classBean.pkgName)
            val genClassDir = File(deleteDir)
            if (genClassDir.exists()) {
                IOUtils.deleteDirectory(genClassDir)
            }
            DLogger.error("deleteGenClass dir succeed: $deleteDir")
        }
    }

    private fun getDeleteDir(classPkgName: String): String {
        val mainDir = AppCodeGuardConfig.javaCodeGenMainDir
        val applicationId = AppCodeGuardConfig.applicationId
        val temp = classPkgName.replace(applicationId, "")
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

    private fun extractPackageAndClassName(filePath: String): String? {
        val file = File(filePath)

        if (!file.exists() || !file.isFile) {
            return null
        }

        val srcMainJava = "src${File.separator}main${File.separator}java"
        val srcMainJavaIndex = filePath.indexOf(srcMainJava)

        if (srcMainJavaIndex == -1) {
            return null
        }

        val packagePath =
            filePath.substring(srcMainJavaIndex + srcMainJava.length + 1, filePath.length - 5)

        return packagePath.replace(File.separator, ".")
    }


    companion object {
        // 目前暂时仅支持系统默认的两种buildType
        private const val DEBUG_VARIANT = "debug"
        private const val RELEASE_VARIANT = "release"
    }
}