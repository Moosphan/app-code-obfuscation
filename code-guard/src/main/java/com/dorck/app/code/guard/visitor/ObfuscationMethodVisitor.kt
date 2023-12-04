package com.dorck.app.code.guard.visitor
import com.dorck.app.code.guard.obfuscate.IAppCodeObfuscator
import org.objectweb.asm.*;

class ObfuscationMethodVisitor(
    val maxCount: Int,
    val useDefault: Boolean, // If true, we will use generated `[RANDOM_NAME].java` by plugin to obfuscate.
    private val obfuscator: IAppCodeObfuscator,
    api: Int,
    mv: MethodVisitor
) : MethodVisitor(api, mv) {

    @Volatile
    private var isNeedInsertion = false

    override fun visitCode() {
        super.visitCode()
        // 随机判断是否插入混淆方法
        isNeedInsertion = shouldInsertConfusionMethod()
        if (isNeedInsertion) {
            val randomMethodCall = obfuscator.nextCodeCall()
            randomMethodCall?.let {
                val methodName = randomMethodCall.name
                val methodDesc = randomMethodCall.desc
                val ownerClz = randomMethodCall.className

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
    }

    override fun visitInsn(opcode: Int) {
        // 在方法体末尾插入 RETURN 指令，表示方法结束
        if (isNeedInsertion) {
            if (opcode == Opcodes.RETURN) {
                super.visitVarInsn(Opcodes.RETURN, 0)
            }
        }
        super.visitInsn(opcode)
    }

    override fun visitEnd() {
        // 在方法结束时插入一些清理代码

        // 调用父类的 visitEnd 方法，确保方法正确结束
        super.visitEnd()
    }

    private fun shouldInsertConfusionMethod(): Boolean {
        // 随机判断是否插入混淆方法，可以根据需求修改
//        return Math.random() < 0.5
        return false
    }
}
