package com.dorck.app.code.guard.task

import com.dorck.app.code.guard.config.AppCodeGuardConfig
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
        // How to get namespace?
        val packageName = project.getPackageName() ?: "com.x.y.z"
        // Note: Need to keep this generated class in proguard rules.
        // TODO: 支持可配置[类名、方法数、包名]，根据方法配置项列表生成
        val className = "Tt"
        val classContent = """
            package $packageName;
            
            public class $className {
            
                public static void x() {
                    
                }
            
                public static void y(int i) {
                    
                }
            
                public static void z(long k) {
                    
                }
            }
        """.trimIndent()

        val outputFile = File(outputDir, "${packageName.replace('.', '/')}/$className.java")
        outputFile.parentFile.mkdirs()
        outputFile.writeText(classContent)
        AppCodeGuardConfig.configJavaCodeGenPath(outputFile.absolutePath)
        project.logger.error("[$className.java] generated succeed: ${outputFile.absolutePath}")
    }
}