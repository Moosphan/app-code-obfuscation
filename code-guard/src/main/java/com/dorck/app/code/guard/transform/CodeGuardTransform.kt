package com.dorck.app.code.guard.transform

import com.android.build.api.transform.TransformInvocation
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
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
    }

    override fun realTransform(transformInvocation: TransformInvocation) {
        val trsStartTime = System.currentTimeMillis()
        val isIncremental = transformInvocation.isIncremental
        if (!isIncremental) {
            transformInvocation.outputProvider.deleteAll()
        }

        transformInvocation.inputs.forEach {
            KLogger.info("Transform jar input size: ${it.jarInputs.size}, dir input size: ${it.directoryInputs.size}")
//            it.jarInputs.forEach { jarInput ->
//                collectAndHandleJars(jarInput, transformInvocation.outputProvider, isIncremental)
//            }
            it.directoryInputs.forEach { dirInput ->
                collectAndHandleDirectories(dirInput, transformInvocation.outputProvider, isIncremental)
            }
        }

        KLogger.info("The transform time cost: ${System.currentTimeMillis() - trsStartTime}ms")
    }

    override fun createClassVisitor(api: Int, delegateClassVisitor: ClassVisitor): ClassVisitor {
        return ObfuscationClassVisitor(extension, api, delegateClassVisitor)
    }

    companion object {
        private const val TAG = "CodeGuardTransform"
    }
}