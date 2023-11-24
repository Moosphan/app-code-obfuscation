package com.dorck.app.code.guard.visitor

import com.dorck.app.code.guard.obfuscate.CodeObfuscationConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ObfuscationClassVisitor(val extension: CodeGuardConfigExtension, api: Int, visitor: ClassVisitor): ClassVisitor(api, visitor) {
    private var isAbsClz = false
    private var className = ""

    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        // Ignore existing fields with the same name
        if (CodeObfuscationConfig.isFieldExist(name, descriptor)) {
            return null
        }
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitEnd() {
        // Add new fields to the class
        insertMultiFields()
        super.visitEnd()
    }

    private fun insertMultiFields() {
        // TODO: 增加随机性，随机取一个添加
        CodeObfuscationConfig.mFieldsList.forEach {
            val fieldVisitor = super.visitField(it.access, it.name, it.type, null, null)
            fieldVisitor?.visitEnd()
        }
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?
    ) {
        super.visit(version, access, name, signature, superName, interfaces)
        className = name ?: ""
        if ((access and Opcodes.ACC_ABSTRACT) > 0 || (access and Opcodes.ACC_INTERFACE) > 0) {
            isAbsClz = true
        }
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?
    ): MethodVisitor {
        val curMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        if (isAbsClz && extension.isSkipAbsClass) {
            return curMethodVisitor;
        }
//            return LogMethodCropVisitor(className, api, curMethodVisitor, access, name ?: "", descriptor ?: "", signature ?: "",
//                (exceptions ?: emptyArray<String>()) as Array<String>
//            )
        return curMethodVisitor
    }
}