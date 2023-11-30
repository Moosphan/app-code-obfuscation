package com.dorck.app.code.guard.task

import com.dorck.app.code.guard.utils.KLogger
import com.dorck.app.code.guard.utils.getPackageName
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

class GenRandomClassTask() : DefaultTask() {
    @OutputDirectory
    var outputDir: File? = null

    @TaskAction
    fun generateClass() {
        // How to get namespace?
        val packageName = project.getPackageName() ?: "com.x.y.z"
        // Note: Need to keep this generated class in proguard rules.
        // TODO: 支持可配置[类名、方法数、包名]
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
        KLogger.info("[$className.java] generated succeed: ${outputFile.absolutePath}")
    }
}