package com.dorck.app.code.guard.test

import org.objectweb.asm.*
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

object InsertMethodsIntoClass {

    @JvmStatic
    fun main(args: Array<String>) {
        val className = "YourExistingClass"

        try {
            val classBytes = Files.readAllBytes(Paths.get("$className.class"))
            val newClassBytes = insertRandomMethods(classBytes)
            saveClassToFile(className + "Modified", newClassBytes)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun insertRandomMethods(classBytes: ByteArray): ByteArray {
        val classReader = ClassReader(classBytes)
        val classWriter = ClassWriter(ClassWriter.COMPUTE_MAXS)
        val classVisitor = CustomClassVisitor(Opcodes.ASM7, classWriter)

        classReader.accept(classVisitor, 0)

        return classWriter.toByteArray()
    }

    private fun saveClassToFile(className: String, classBytes: ByteArray) {
        val fileName = "$className.class"
        Files.write(Paths.get(fileName), classBytes)
        println("Modified class saved to file: $fileName")
    }

    private class CustomClassVisitor(api: Int, cv: ClassVisitor) : ClassVisitor(api, cv) {
        private val random = Random()

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? {
            val mv = cv.visitMethod(access, name, descriptor, signature, exceptions)

            // 在 visitMethod 中插入新的方法
            if (shouldInsertMethod()) {
                insertNewMethod(mv)
            }

            return mv
        }

        private fun shouldInsertMethod(): Boolean {
            // 这里可以根据需要定义插入新方法的条件，示例中是随机插入
            return random.nextBoolean()
        }

        private fun insertNewMethod(mv: MethodVisitor?) {
            mv?.let {
                it.visitCode()
                it.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;")
                it.visitLdcInsn("Hello from dynamically inserted method")
                it.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false)
                it.visitInsn(Opcodes.RETURN)
                it.visitMaxs(2, 1)
                it.visitEnd()
            }
        }
    }
}

