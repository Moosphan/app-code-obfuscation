package com.dorck.app.code.guard.transform

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import org.gradle.api.Project
import org.objectweb.asm.ClassVisitor

class CodeGuardTransform(
    private val extension: CodeGuardConfigExtension,
    private val project: Project
) : BaseTransform() {

    override fun getName(): String = TAG

    override fun isIncremental(): Boolean = extension.supportIncremental

    override fun createClassVisitor(api: Int, delegateClassVisitor: ClassVisitor): ClassVisitor {
        TODO("Not yet implemented")
    }

    companion object {
        private const val TAG = "CodeGuardTransform"
    }
}