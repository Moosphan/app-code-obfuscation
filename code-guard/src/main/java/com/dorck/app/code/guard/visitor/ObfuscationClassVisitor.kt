package com.dorck.app.code.guard.visitor

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.obfuscate.FieldEntity
import com.dorck.app.code.guard.obfuscate.IAppCodeObfuscator
import com.dorck.app.code.guard.obfuscate.MethodEntity
import com.dorck.app.code.guard.utils.KLogger
import org.gradle.kotlin.dsl.provideDelegate
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
    private val mMaxFieldsSize: Int by lazy { extension.maxFieldCount }
    private val mMaxMethodsSize: Int by lazy { extension.maxMethodCount }

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
        KLogger.error("visitField, mMaxFieldsSize: $mMaxFieldsSize")
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        // Insert at random index(始终保证插入变量数目少于配置的数量上限).
        if (obfuscator.shouldInsertElement() && mFieldInsertionCount <= mMaxFieldsSize) {
            updateCurFieldList(access, name, descriptor)
            // 保证历史属性能被正常保留
            super.visitField(access, name, descriptor, signature, value)
            return insertRandomField(obfuscator)
        }
        // 保证原有变量可以正常访问
        updateCurFieldList(access, name, descriptor)
        return super.visitField(access, name, descriptor, signature, value)
    }

    override fun visitEnd() {
        KLogger.error("visitEnd, current insertion count: $mFieldInsertionCount")
        // 如果插入数量不足需要补齐
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        if (mFieldInsertionCount <= mMaxFieldsSize) {
            repeat(mMaxFieldsSize - mFieldInsertionCount) {
                insertRandomField(obfuscator)
            }
        }
        if (mMethodInsertionCount <= mMaxMethodsSize) {
            repeat(mMaxMethodsSize - mMethodInsertionCount) {
                insertRandomMethod(obfuscator)
            }
        }
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
    ): MethodVisitor? {
//        return super.visitMethod(access, name, descriptor, signature, exceptions)
//        // 如果设置跳过抽象类或者构造函数，则直接返回
        if ((isAbsClz && extension.isSkipAbsClass) || isConstructor(name!!, descriptor!!)) {
            return super.visitMethod(access, name, descriptor, signature, exceptions)
        }
        KLogger.error("visitMethod >> $name => $descriptor")
        // Start insert empty methods in cur class.
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        if (obfuscator.shouldInsertElement() && mMethodInsertionCount < mMaxMethodsSize) {
            // 保证原有函数可用
            val defaultMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
            return insertRandomMethod(obfuscator) ?: defaultMethodVisitor
        }
        // 注意插入的方法不需要执行函数内的代码插入
        mClassMethods.add(MethodEntity(name, descriptor, access))
        val curMethodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
        return ObfuscationMethodVisitor(extension.maxCodeLineCount, extension.isAutoAdapted, obfuscator, api, curMethodVisitor)
    }

    private fun insertRandomField(obfuscator: IAppCodeObfuscator): FieldVisitor? {
        KLogger.error("insertRandomField >> start insert field, progress: [${mFieldInsertionCount+1}/$mMaxFieldsSize]")
        val randomField = obfuscator.nextFiled()
        // Ignore existing fields with the same name.
        if (!isFieldExist(randomField.name, randomField.type)) {
            KLogger.error("Start to insert random field: $randomField")
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
        return null
    }

    private fun insertRandomMethod(obfuscator: IAppCodeObfuscator): MethodVisitor? {
        KLogger.error("visitMethod >> start insert method, progress: [${mMethodInsertionCount+1}/$mMaxMethodsSize]")
        val randomMethod = obfuscator.nextMethod()
        // 检查是否存在同名的方法，避免重复插入
        if (!isMethodExist(randomMethod.name, randomMethod.desc)) {
            KLogger.error("Start to insert random method: $randomMethod")
            mMethodInsertionCount++
            mClassMethods.add(randomMethod)
            // Insert random method.
            // FIXME 12/04: Absent Code attribute in method that is not native or abstract
            val insertMethodVisitor = cv.visitMethod(
                randomMethod.access,
                randomMethod.name,
                randomMethod.desc,
                null,
                null
            )
            insertMethodVisitor.visitCode()
            insertMethodVisitor.visitInsn(Opcodes.RETURN)
            insertMethodVisitor.visitMaxs(1, 1) // 设置栈的最大深度和局部变量的最大索引
            insertMethodVisitor.visitEnd()
            KLogger.error("Finished insert random method: ${randomMethod.name}")
            return insertMethodVisitor
        }
        return null
    }

    private fun updateCurFieldList(access: Int, name: String, descriptor: String) {
        mClassFields.add(FieldEntity(name, access, descriptor, false))
        updateFieldsLimitCount()
    }

    private fun updateFieldsLimitCount() {
        // Real-time update of `maxFieldsSize` is only required in adaptive mode
        if (!extension.isAutoAdapted) {
            return
        }
        val originFields = mClassFields.filter { !it.isInserted }
        var newCount = (originFields.size * DEFAULT_ADAPTIVE_COUNT_RATIO).toInt()
        if (newCount < AppCodeGuardConfig.MIN_FIELD_COUNT) {
            newCount = AppCodeGuardConfig.MAX_FIELD_COUNT
        }
//        mMaxFieldsSize = newCount
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

    private fun isConstructor(methodName: String, methodDescriptor: String): Boolean {
        // 构造函数的名称与类名相同，而且没有返回类型（使用 'V' 表示 void）
        return methodName == "<init>" && methodDescriptor.endsWith(")V")
    }

    companion object {
        // Ratio of inserted variables or methods to existing variables or methods in the current class.
        private const val DEFAULT_ADAPTIVE_COUNT_RATIO = 0.5
    }
}