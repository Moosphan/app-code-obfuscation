package com.dorck.app.code.guard.visitor

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.obfuscate.FieldEntity
import com.dorck.app.code.guard.obfuscate.MethodEntity
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes

class ObfuscationClassVisitor(private val extension: CodeGuardConfigExtension, api: Int, visitor: ClassVisitor): ClassVisitor(api, visitor) {
    private var isAbsClz = false
    private var className = ""
    private val mClassFields: MutableList<FieldEntity> = mutableListOf()
    private val mClassMethods: MutableList<MethodEntity> = mutableListOf()
    private var mCurInsertedField: FieldEntity? = null
    private var mMaxFieldsSize: Int = if (extension.isAutoAdapted) 0 else extension.maxFieldCount
    private var mMaxMethodsSize: Int = if (extension.isAutoAdapted) 0 else extension.maxMethodCount

    @Volatile
    private var mFieldInsertionCount: Int = 0
    @Volatile
    private var mMethodInsertionCount: Int = 0


    override fun visitField(
        access: Int,
        name: String,
        descriptor: String,
        signature: String?,
        value: Any?
    ): FieldVisitor? {
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        // Insert at random index(始终保证插入变量数目少于配置的数量上限).
        if (obfuscator.shouldInsertElement() && mFieldInsertionCount < mMaxFieldsSize) {
            val randomField = obfuscator.nextFiled()
            // Ignore existing fields with the same name.
            if (!isFieldExist(randomField.name, randomField.type)) {
                // Start insert field.
                mCurInsertedField = randomField
                mFieldInsertionCount++
                mClassFields.add(randomField)
                return super.visitField(
                    randomField.access,
                    randomField.name,
                    randomField.type,
                    null,
                    null
                )
            }
        }
        // 保证原有变量可以正常访问
        mClassFields.add(FieldEntity(name, access, descriptor, false))
        updateFieldsLimitCount()
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitEnd() {
        super.visitEnd()
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
        // 如果设置跳过抽象类，则直接返回
        if (isAbsClz && extension.isSkipAbsClass) {
            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
        // Start insert empty methods in cur class.
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        if (obfuscator.shouldInsertElement()) {
            val randomMethod = obfuscator.nextMethod()
            // 检查是否存在同名的方法，避免重复插入
            if (!isMethodExist(randomMethod.name, randomMethod.desc)) {
                // Insert random method.
                return cv.visitMethod(
                    randomMethod.access,
                    randomMethod.name,
                    randomMethod.desc,
                    null,
                    null
                )
            }
        }
        // 注意插入的方法不需要执行函数内的代码插入
        val curMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return ObfuscationMethodVisitor(extension.maxCodeLineCount, extension.isAutoAdapted, obfuscator, api, curMethodVisitor)
    }

    private fun updateFieldsLimitCount() {
        // Real-time update of `maxFieldsSize` is only required in adaptive mode
        if (!extension.isAutoAdapted) {
            return
        }
        val originFields = mClassFields.filter { !it.isInserted }
        mMaxFieldsSize = (originFields.size * DEFAULT_ADAPTIVE_COUNT_RATIO).toInt()
    }

    private fun isFieldExist(name: String, descriptor: String): Boolean {
        if (mClassFields.isEmpty()) {
            return false
        }
        mClassFields.forEach {
            if (name == it.name && descriptor == it.type) {
                return true
            }
        }
        return false
    }

    private fun isMethodExist(name: String, descriptor: String): Boolean {
        if (mClassMethods.isEmpty()) {
            return false
        }
        mClassMethods.forEach {
            if (name == it.name && descriptor == it.desc) {
                return true
            }
        }
        return false
    }

    companion object {
        // Ratio of inserted variables or methods to existing variables or methods in the current class.
        private const val DEFAULT_ADAPTIVE_COUNT_RATIO = 0.5
    }
}