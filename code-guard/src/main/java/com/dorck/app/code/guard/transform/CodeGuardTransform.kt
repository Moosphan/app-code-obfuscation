package com.dorck.app.code.guard.transform

import com.android.build.api.transform.Format
import com.android.build.api.transform.TransformInvocation
import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.utils.IOUtils
import com.dorck.app.code.guard.utils.KLogger
import com.dorck.app.code.guard.visitor.ObfuscationClassVisitor
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor

class CodeGuardTransform(
    private val extension: CodeGuardConfigExtension,
    private val project: Project
) : BaseTransform() {

    override fun getName(): String = TAG

    override fun isIncremental(): Boolean = extension.supportIncremental

    override fun onTransformBefore(transformInvocation: TransformInvocation) {
        KLogger.debug = extension.logDebug
        KLogger.info("onTransformBefore, extension: $extension")
        AppCodeGuardConfig.readConfigs()
    }

    override fun realTransform(transformInvocation: TransformInvocation) {
        if (!extension.enable) {
            return
        }
        val trsStartTime = System.currentTimeMillis()
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach {
            KLogger.info("Transform jar input size: ${it.jarInputs.size}, dir input size: ${it.directoryInputs.size}")
            it.jarInputs.forEach { jarInput ->
                // collectAndHandleJars(jarInput, transformInvocation.outputProvider, isIncremental)
                // Jars无需处理，直接拷贝过去
                val dest = transformInvocation.outputProvider.getContentLocation(jarInput.name, jarInput.contentTypes, jarInput.scopes, Format.JAR)
                IOUtils.copyFile(jarInput.file, dest)
            }
            it.directoryInputs.forEach { dirInput ->
                collectAndHandleDirectories(dirInput, transformInvocation.outputProvider, isIncremental)
            }
        }

        KLogger.info("The transform time cost: ${System.currentTimeMillis() - trsStartTime}ms")
    }

    override fun createClassVisitor(api: Int, delegateClassVisitor: ClassVisitor): ClassVisitor {
        return ObfuscationClassVisitor(extension, api, delegateClassVisitor)
    }

    override fun isNeedProcessClass(clzPath: String): Boolean {
        return super.isNeedProcessClass(clzPath) && !AppCodeGuardConfig.checkExcludes(clzPath)
    }

    companion object {
        private const val TAG = "CodeGuardTransform"
    }
}