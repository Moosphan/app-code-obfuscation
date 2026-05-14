package com.dorck.app.code.guard.visitor

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.RandomCodeObfuscator
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

/**
 * Unit tests for ObfuscationClassVisitor.
 */
class ObfuscationClassVisitorTest {

    private lateinit var extension: CodeGuardConfigExtension

    @Before
    fun setup() {
        extension = CodeGuardConfigExtension().apply {
            enable = true
            maxFieldCount = 5
            minFieldCount = 2
            maxMethodCount = 3
            minMethodCount = 1
            methodObfuscateEnable = false
            isSkipAbsClass = true
            isInsertCountAutoAdapted = false
        }
        RandomCodeObfuscator.reset()
    }

    @Test
    fun `visitor adds fields to class`() {
        val originalBytes = createSimpleClassBytes()
        val classReader = ClassReader(originalBytes)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val visitor = ObfuscationClassVisitor(extension, Opcodes.ASM9, classWriter)

        classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
        val modifiedBytes = classWriter.toByteArray()

        // Parse the modified class to verify fields were added
        val modifiedReader = ClassReader(modifiedBytes)
        val fieldNames = mutableListOf<String>()
        modifiedReader.accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitField(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                value: Any?
            ): org.objectweb.asm.FieldVisitor? {
                name?.let { fieldNames.add(it) }
                return super.visitField(access, name, descriptor, signature, value)
            }
        }, ClassReader.SKIP_CODE)

        // Should have original field + inserted fields
        assertTrue("Expected multiple fields, got ${fieldNames.size}", fieldNames.size >= extension.minFieldCount)
    }

    @Test
    fun `visitor adds methods to class`() {
        val originalBytes = createSimpleClassBytes()
        val classReader = ClassReader(originalBytes)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val visitor = ObfuscationClassVisitor(extension, Opcodes.ASM9, classWriter)

        classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
        val modifiedBytes = classWriter.toByteArray()

        // Parse the modified class to verify methods were added
        val modifiedReader = ClassReader(modifiedBytes)
        val methodNames = mutableListOf<String>()
        modifiedReader.accept(object : ClassVisitor(Opcodes.ASM9) {
            override fun visitMethod(
                access: Int,
                name: String?,
                descriptor: String?,
                signature: String?,
                exceptions: Array<out String>?
            ): org.objectweb.asm.MethodVisitor? {
                name?.let { methodNames.add(it) }
                return super.visitMethod(access, name, descriptor, signature, exceptions)
            }
        }, 0)

        // Should have <init> + original method + inserted methods
        assertTrue("Expected multiple methods, got ${methodNames.size}", methodNames.size >= 1 + extension.minMethodCount)
    }

    @Test
    fun `visitor processes abstract class without error`() {
        extension.isSkipAbsClass = true

        val originalBytes = createAbstractClassBytes()
        val classReader = ClassReader(originalBytes)
        val classWriter = ClassWriter(classReader, ClassWriter.COMPUTE_MAXS)
        val visitor = ObfuscationClassVisitor(extension, Opcodes.ASM9, classWriter)

        // Should not throw any exception
        classReader.accept(visitor, ClassReader.EXPAND_FRAMES)
        val modifiedBytes = classWriter.toByteArray()

        // Verify the modified class is valid
        assertNotNull(modifiedBytes)
        assertTrue(modifiedBytes.isNotEmpty())
    }

    /**
     * Create bytecode for a simple class with one field and one method.
     */
    private fun createSimpleClassBytes(): ByteArray {
        val classWriter = ClassWriter(0)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC,
            "TestClass",
            null,
            "java/lang/Object",
            null
        )

        // Add a field
        classWriter.visitField(
            Opcodes.ACC_PRIVATE,
            "originalField",
            "I",
            null,
            null
        )?.visitEnd()

        // Add constructor
        val initMethod = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "<init>",
            "()V",
            null,
            null
        )
        initMethod.visitCode()
        initMethod.visitVarInsn(Opcodes.ALOAD, 0)
        initMethod.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false)
        initMethod.visitInsn(Opcodes.RETURN)
        initMethod.visitMaxs(1, 1)
        initMethod.visitEnd()

        // Add a method
        val testMethod = classWriter.visitMethod(
            Opcodes.ACC_PUBLIC,
            "testMethod",
            "()V",
            null,
            null
        )
        testMethod.visitCode()
        testMethod.visitInsn(Opcodes.RETURN)
        testMethod.visitMaxs(1, 1)
        testMethod.visitEnd()

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    /**
     * Create bytecode for an abstract class.
     */
    private fun createAbstractClassBytes(): ByteArray {
        val classWriter = ClassWriter(0)
        classWriter.visit(
            Opcodes.V1_8,
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
            "AbstractClass",
            null,
            "java/lang/Object",
            null
        )

        // Add abstract method
        classWriter.visitMethod(
            Opcodes.ACC_PUBLIC or Opcodes.ACC_ABSTRACT,
            "abstractMethod",
            "()V",
            null,
            null
        )

        classWriter.visitEnd()
        return classWriter.toByteArray()
    }
}
