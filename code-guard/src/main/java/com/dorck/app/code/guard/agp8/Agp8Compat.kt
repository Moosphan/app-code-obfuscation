package com.dorck.app.code.guard.agp8

import com.android.build.api.instrumentation.FramesComputationMode
import com.android.build.api.instrumentation.InstrumentationScope
import com.android.build.api.variant.AndroidComponentsExtension
import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.utils.DLogger
import org.gradle.api.Project

/**
 * AGP 8.0+ compatibility helper.
 * Uses the new Instrumentation API (AsmClassVisitorFactory) instead of the deprecated Transform API.
 * @author Dorck
 * @since 2024/05/13
 */
object Agp8Compat {

    /**
     * Check if current AGP version supports the new Instrumentation API (AGP 8.0+).
     */
    fun isAgp8OrHigher(): Boolean {
        return try {
            Class.forName("com.android.build.api.instrumentation.AsmClassVisitorFactory")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }

    /**
     * Register the AsmClassVisitorFactory for AGP 8.0+.
     */
    fun register(
        project: Project,
        extension: CodeGuardConfigExtension
    ) {
        val androidComponents = project.extensions.findByType(AndroidComponentsExtension::class.java)
        if (androidComponents == null) {
            DLogger.error("Agp8Compat: AndroidComponentsExtension not found, cannot register AsmClassVisitorFactory.")
            return
        }

        androidComponents.onVariants { variant ->
            DLogger.info("Agp8Compat: Registering AsmClassVisitorFactory for variant: ${variant.name}")

            // Check variant constraints
            val variantRules = extension.variantConstraints
            if (variantRules.isNotEmpty() && !variantRules.contains(variant.name) && !variantRules.contains("all")) {
                DLogger.info("Agp8Compat: Variant ${variant.name} not in constraints, skipping.")
                return@onVariants
            }

            variant.instrumentation.transformClassesWith(
                CodeGuardClassVisitorFactory::class.java,
                InstrumentationScope.ALL
            ) { params ->
                params.enable.set(extension.enable)
                params.processingPackages.set(extension.processingPackages.toList())
                params.excludeRules.set(
                    (extension.excludeRules + AppCodeGuardConfig.DEFAULT_EXCLUDE_RULES).toList()
                )
                params.maxFieldCount.set(extension.maxFieldCount)
                params.maxMethodCount.set(extension.maxMethodCount)
                params.minFieldCount.set(extension.minFieldCount)
                params.minMethodCount.set(extension.minMethodCount)
                params.skipAbsClass.set(extension.isSkipAbsClass)
                params.methodObfuscateEnable.set(extension.methodObfuscateEnable)
                params.maxCodeLineCount.set(extension.maxCodeLineCount)
                params.insertCountAutoAdapted.set(extension.isInsertCountAutoAdapted)
                params.genClassCount.set(extension.genClassCount)
                params.genClassMethodCount.set(extension.generatedClassMethodCount)
                params.generatedClassPkg.set(extension.generatedClassPkg)
                params.generatedClassName.set(extension.generatedClassName)
                params.obfuscationDict.set(extension.obfuscationDict)
                params.logDebug.set(extension.logDebug)
            }

            variant.instrumentation.setAsmFramesComputationMode(
                FramesComputationMode.COMPUTE_FRAMES_FOR_INSTRUMENTED_METHODS
            )
        }

        DLogger.error("Agp8Compat: AsmClassVisitorFactory registered successfully.")
    }
}
