package com.dorck.app.code.guard.task

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.utils.CodeGenerator
import com.dorck.app.code.guard.utils.KLogger
import com.dorck.app.code.guard.utils.getPackageName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

abstract class GenRandomClassTask : DefaultTask() {
    @OutputDirectory
    var outputDir: File? = null

    @TaskAction
    fun generateClass() {
        project.logger.error("Start generate class...")
        val appID = project.getPackageName() ?: "com.x.y.z"
        // Inject applicationId into global configs.
        AppCodeGuardConfig.configApplicationId(appID)
        // Note: Need to keep this generated class in proguard rules.
        // TODO: 支持可配置[类名、方法数、包名]，根据方法配置项列表生成
        val extension = project.extensions.findByType(CodeGuardConfigExtension::class.java)
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension!!)
        obfuscator.initialize()
        val classEntity = obfuscator.getCurClassEntity() ?: throw IllegalStateException("The random class generation failed.")
        val className = classEntity.className
        // How to get namespace?
        val packageName = classEntity.pkgName
        val genJavaCode = CodeGenerator.generateJavaClass(classEntity)

        val outputParentDir = File(outputDir, packageName.replace('.', '/') + "/")
        // 判断生成的文件路径是否已存在(此处如果已经赋值过就跳过，防止variant下反复修改的情况)
        if (AppCodeGuardConfig.isPkgExist == null) {
            AppCodeGuardConfig.configPackageExistState(outputParentDir.exists())
        }
        KLogger.error("generateClass, is gen pkg exist: ${outputParentDir.exists()}")
        val outputFile = File(outputDir, "${packageName.replace('.', '/')}/$className.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(genJavaCode)
        project.logger.error("Random java class code generation: $genJavaCode")
        AppCodeGuardConfig.configJavaCodeGenPath(outputFile.absolutePath)
        project.logger.error("[$className.java] generated succeed: ${outputFile.absolutePath}")
    }
}