package com.dorck.app.code.guard.transform

import com.android.build.api.transform.Format
import com.android.build.api.transform.TransformInvocation
import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.utils.DLogger
import com.dorck.app.code.guard.visitor.ObfuscationClassVisitor
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor

class CodeGuardTransform(
    private val extension: CodeGuardConfigExtension,
    private val project: Project
) : BaseTransform() {

    override fun getName(): String = TRANSFORM_NAME

    override fun isIncremental(): Boolean = extension.supportIncremental

    override fun onTransformBefore(transformInvocation: TransformInvocation) {
        DLogger.debug = extension.logDebug
        DLogger.info("onTransformBefore, extension: $extension")
        AppCodeGuardConfig.readConfigs()
    }

    override fun realTransform(transformInvocation: TransformInvocation) {
        // Check if ASM processing is required.
        val isAsmEnable = extension.enable && variantMatches()
        if (!isAsmEnable) {
            DLogger.error("realTransform, plugin function not enable.")
            // 不应该直接返回，需要将代码复制到输出目录，否则无法生成dex
            // return
        }
        val trsStartTime = System.currentTimeMillis()
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach {
            DLogger.info("Transform jar input size: ${it.jarInputs.size}, dir input size: ${it.directoryInputs.size}")
            it.jarInputs.forEach { jarInput ->
                // collectAndHandleJars(jarInput, transformInvocation.outputProvider, isIncremental)
                // Jars无需处理，直接拷贝过去
                val dest = transformInvocation.outputProvider.getContentLocation(jarInput.name,
                    jarInput.contentTypes, jarInput.scopes, Format.JAR)
                jarInput.file.copyRecursively(dest, true)
            }
            it.directoryInputs.forEach { dirInput ->
                if (isAsmEnable) {
                    collectAndHandleDirectories(dirInput, transformInvocation.outputProvider, isIncremental)
                } else {
                    val destDir = transformInvocation.outputProvider.getContentLocation(dirInput.name,
                        dirInput.contentTypes, dirInput.scopes, Format.DIRECTORY)
                    dirInput.file.copyRecursively(destDir, true)
                }

            }
        }

        DLogger.info("The transform time cost: ${System.currentTimeMillis() - trsStartTime}ms")
    }

    override fun createClassVisitor(api: Int, delegateClassVisitor: ClassVisitor): ClassVisitor {
        return ObfuscationClassVisitor(extension, api, delegateClassVisitor)
    }

    override fun isNeedProcessClass(clzPath: String): Boolean {
        return super.isNeedProcessClass(clzPath) && !AppCodeGuardConfig.checkExcludes(clzPath)
    }

    /**
     * If `variantConstraints` empty or matches in available variants, returns true.
     */
    private fun variantMatches(): Boolean {
        DLogger.error("variantMatches, rules: ${extension.variantConstraints}, curVariant: ${AppCodeGuardConfig.currentBuildVariant}")
        val variantRules = extension.variantConstraints
        if (variantRules.isEmpty() || variantRules.contains(AppCodeGuardConfig.currentBuildVariant)) {
            return true
        }
        return false
    }

    companion object {
        const val TRANSFORM_NAME = "CodeGuardTransform"
    }
}