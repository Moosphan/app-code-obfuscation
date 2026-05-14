package com.dorck.app.code.guard.agp8

import com.android.build.api.instrumentation.AsmClassVisitorFactory
import com.android.build.api.instrumentation.ClassContext
import com.android.build.api.instrumentation.ClassData
import com.android.build.api.instrumentation.InstrumentationParameters
import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.utils.DLogger
import com.dorck.app.code.guard.visitor.ObfuscationClassVisitor
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes

/**
 * AGP 8.0+ AsmClassVisitorFactory implementation for code obfuscation.
 * Replaces the deprecated Transform API approach.
 * @author Dorck
 * @since 2024/05/13
 */
abstract class CodeGuardClassVisitorFactory : AsmClassVisitorFactory<CodeGuardClassVisitorFactory.Params> {

    interface Params : InstrumentationParameters {
        @get:Input
        val enable: Property<Boolean>

        @get:Input
        val processingPackages: ListProperty<String>

        @get:Input
        val excludeRules: ListProperty<String>

        @get:Input
        val maxFieldCount: Property<Int>

        @get:Input
        val maxMethodCount: Property<Int>

        @get:Input
        val minFieldCount: Property<Int>

        @get:Input
        val minMethodCount: Property<Int>

        @get:Input
        val skipAbsClass: Property<Boolean>

        @get:Input
        val methodObfuscateEnable: Property<Boolean>

        @get:Input
        val maxCodeLineCount: Property<Int>

        @get:Input
        val insertCountAutoAdapted: Property<Boolean>

        @get:Input
        val genClassCount: Property<Int>

        @get:Input
        val genClassMethodCount: Property<Int>

        @get:Input
        val generatedClassPkg: Property<String>

        @get:Input
        val generatedClassName: Property<String>

        @get:Input
        val obfuscationDict: Property<String>

        @get:Input
        val logDebug: Property<Boolean>
    }

    override fun isInstrumentable(classData: ClassData): Boolean {
        val params = parameters.get()
        if (!params.enable.get()) {
            DLogger.error("isInstrumentable, plugin disabled, skip: ${classData.className}")
            return false
        }

        val className = classData.className

        // Use params to check package scope directly (more reliable than AppCodeGuardConfig singleton)
        // Note: className uses dots (e.g., "com.example.MyClass"), not slashes
        val packages = params.processingPackages.get()
        if (packages.isNotEmpty()) {
            val matches = packages.any { pkg ->
                className.startsWith(pkg)
            }
            if (!matches) {
                DLogger.info("isInstrumentable, not in scope: $className, packages: $packages")
                return false
            }
        }

        // Check exclude rules
        val excludeRules = params.excludeRules.get()
        val simpleName = className.substringAfterLast('/')
        for (rule in excludeRules) {
            try {
                val regex = Regex(rule)
                if (regex.matches(simpleName) || className.contains(rule)) {
                    DLogger.error("isInstrumentable, excluded by rule [$rule]: $className")
                    return false
                }
            } catch (e: Exception) {
                DLogger.error("isInstrumentable, invalid regex: $rule, err: $e")
            }
        }

        DLogger.error("isInstrumentable, PASS: $className")
        return true
    }

    override fun createClassVisitor(
        classContext: ClassContext,
        nextClassVisitor: ClassVisitor
    ): ClassVisitor {
        val params = parameters.get()

        // Create extension from parameters
        val extension = createExtensionFromParams(params)

        DLogger.debug = params.logDebug.get()
        DLogger.info("createClassVisitor for: ${classContext.currentClassData.className}")

        return ObfuscationClassVisitor(extension, Opcodes.ASM9, nextClassVisitor)
    }

    private fun createExtensionFromParams(params: Params): CodeGuardConfigExtension {
        return CodeGuardConfigExtension().apply {
            enable = params.enable.get()
            isSkipAbsClass = params.skipAbsClass.get()
            maxFieldCount = params.maxFieldCount.get()
            maxMethodCount = params.maxMethodCount.get()
            minFieldCount = params.minFieldCount.get()
            minMethodCount = params.minMethodCount.get()
            methodObfuscateEnable = params.methodObfuscateEnable.get()
            maxCodeLineCount = params.maxCodeLineCount.get()
            isInsertCountAutoAdapted = params.insertCountAutoAdapted.get()
            genClassCount = params.genClassCount.get()
            generatedClassMethodCount = params.genClassMethodCount.get()
            generatedClassPkg = params.generatedClassPkg.get()
            generatedClassName = params.generatedClassName.get()
            obfuscationDict = params.obfuscationDict.get()
            processingPackages.addAll(params.processingPackages.get())
            excludeRules.addAll(params.excludeRules.get())
        }
    }
}
