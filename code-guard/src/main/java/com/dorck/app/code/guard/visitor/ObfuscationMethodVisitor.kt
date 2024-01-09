package com.dorck.app.code.guard.visitor

import com.dorck.app.code.guard.obfuscate.IAppCodeObfuscator
import com.dorck.app.code.guard.obfuscate.MethodEntity
import com.dorck.app.code.guard.obfuscate.RandomCodeObfuscator
import com.dorck.app.code.guard.utils.DLogger
import org.objectweb.asm.*;

class ObfuscationMethodVisitor(
    private val maxCount: Int,
    // If true, plugin will auto insert specific count of codes by ins in method.
    val insertCountAutoAdapted: Boolean,
    // If is [RandomCodeObfuscator], we will use generated `[RANDOM_NAME].java` by plugin to obfuscate
    private val obfuscator: IAppCodeObfuscator,
    api: Int,
    mv: MethodVisitor
) : MethodVisitor(api, mv) {

    @Volatile
    private var isNeedInsertion = false

    private var mCurCall: MethodEntity? = null
    private var mCurInsertionCount: Int = 0

    override fun visitCode() {
        super.visitCode()
        // 随机判断是否插入混淆方法
        isNeedInsertion = shouldInsertConfusionMethod()
        if (isNeedInsertion && mCurInsertionCount < maxCount) {
            val insertCount = getRandomInsertCount()
            repeat(insertCount) {
                insertCodeCall()
            }
            mCurInsertionCount += insertCount
        }
    }

    private fun insertCodeCall() {
        val randomMethodCall = obfuscator.nextCodeCall()
        DLogger.error("visitCode >> randomMethodCall: ${randomMethodCall ?: "null"}")
        randomMethodCall?.let {
            this.mCurCall = randomMethodCall
            val methodName = randomMethodCall.name
            val methodDesc = randomMethodCall.desc
            val ownerClz = randomMethodCall.className
            // Get params descriptors from method desc.
            val paramTypes = Type.getArgumentTypes(methodDesc)
            // Handle each param default value.
            paramTypes.forEach { paramType ->
                pushDefaultConstToStack(paramType.descriptor)
            }
            // 插入混淆方法调用
            mv.visitMethodInsn(
                Opcodes.INVOKESTATIC,
                ownerClz,
                methodName,
                methodDesc,
                false
            )
        }
    }

    override fun visitInsn(opcode: Int) {
        // 如果方法访问结束时数量不够，则继续插入
        if (isNeedInsertion && mCurCall != null) {
            if (opcode == Opcodes.RETURN || opcode == Opcodes.ATHROW) {
                if (mCurInsertionCount < maxCount) {
                    repeat(maxCount - mCurInsertionCount) {
                        insertCodeCall()
                    }
                }
            }
        }
        super.visitInsn(opcode)
    }

    private fun shouldInsertConfusionMethod(): Boolean {
        // 随机判断是否插入混淆方法，可以根据需求修改
//        return Math.random() < 0.5
        return true
    }

    /**
     * For methods with parameters, default parameter values need to be pushed to the stack.
     */
    private fun pushDefaultConstToStack(descriptor: String) {
        when (descriptor) {
            "I" -> mv.visitLdcInsn(0)          // Default value for int
            "F" -> mv.visitLdcInsn(0.0f)       // Default value for float
            "D" -> mv.visitLdcInsn(0.0)        // Default value for double
            "Z" -> mv.visitLdcInsn(false)      // Default value for boolean
            "C" -> mv.visitLdcInsn('\u0000')   // Default value for char
            "B" -> mv.visitLdcInsn(0.toByte())       // Default value for byte
            "S" -> mv.visitLdcInsn(0.toShort())      // Default value for short
            "J" -> mv.visitLdcInsn(0L)         // Default value for long
            "Ljava/lang/String;" -> mv.visitLdcInsn(RandomCodeObfuscator.generateRandomName(maxLength = 2)) // Default value for String
            "" -> { // No params
                // do nothing
            }
            else -> throw IllegalArgumentException("Unsupported parameter type: $descriptor")
        }
    }

    private fun getRandomInsertCount(): Int {
        return (Math.random() * 5 + 2).toInt()
    }

}
