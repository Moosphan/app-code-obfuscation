package com.dorck.app.code.guard.utils

import com.dorck.app.code.guard.obfuscate.MethodEntity
import com.dorck.app.code.guard.obfuscate.SimpleClassEntity
import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.Opcodes

/**
 * Unit tests for CodeGenerator.
 */
class CodeGeneratorTest {

    @Test
    fun `generateJavaClass creates valid class`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.example",
            className = "TestClass",
            methods = listOf(
                MethodEntity("testMethod", "()V", Opcodes.ACC_PUBLIC, isStatic = true)
            )
        )

        val code = CodeGenerator.generateJavaClass(classEntity)

        assertTrue(code.contains("package com.example;"))
        assertTrue(code.contains("public class TestClass"))
        assertTrue(code.contains("public static void testMethod()"))
    }

    @Test
    fun `generateJavaClass handles multiple methods`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.test",
            className = "MultiMethod",
            methods = listOf(
                MethodEntity("method1", "()V", Opcodes.ACC_PUBLIC, isStatic = true),
                MethodEntity("method2", "(I)V", Opcodes.ACC_PRIVATE, isStatic = false),
                MethodEntity("method3", "(Ljava/lang/String;)V", Opcodes.ACC_PROTECTED, isStatic = true)
            )
        )

        val code = CodeGenerator.generateJavaClass(classEntity)

        assertTrue(code.contains("public static void method1()"))
        assertTrue(code.contains("private void method2(int param0)"))
        assertTrue(code.contains("protected static void method3(java.lang.String param0)"))
    }

    @Test
    fun `generateJavaClass handles various parameter types`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.test",
            className = "ParamTypes",
            methods = listOf(
                MethodEntity("withInt", "(I)V", Opcodes.ACC_PUBLIC, isStatic = true),
                MethodEntity("withString", "(Ljava/lang/String;)V", Opcodes.ACC_PUBLIC, isStatic = true),
                MethodEntity("withBool", "(Z)V", Opcodes.ACC_PUBLIC, isStatic = true),
                MethodEntity("withMulti", "(IZLjava/lang/String;)V", Opcodes.ACC_PUBLIC, isStatic = true)
            )
        )

        val code = CodeGenerator.generateJavaClass(classEntity)

        assertTrue(code.contains("void withInt(int param0)"))
        assertTrue(code.contains("void withString(java.lang.String param0)"))
        assertTrue(code.contains("void withBool(boolean param0)"))
        assertTrue(code.contains("void withMulti(int param0, boolean param1, java.lang.String param2)"))
    }

    @Test
    fun `generateJavaClass with empty methods list`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.test",
            className = "EmptyClass",
            methods = emptyList()
        )

        val code = CodeGenerator.generateJavaClass(classEntity)

        assertTrue(code.contains("package com.test;"))
        assertTrue(code.contains("public class EmptyClass"))
        assertTrue(code.contains("}"))
    }

    @Test
    fun `generateJavaClass preserves access modifiers`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.test",
            className = "AccessTest",
            methods = listOf(
                MethodEntity("publicMethod", "()V", Opcodes.ACC_PUBLIC, isStatic = true),
                MethodEntity("privateMethod", "()V", Opcodes.ACC_PRIVATE, isStatic = true),
                MethodEntity("protectedMethod", "()V", Opcodes.ACC_PROTECTED, isStatic = true),
                MethodEntity("defaultMethod", "()V", 0, isStatic = true)
            )
        )

        val code = CodeGenerator.generateJavaClass(classEntity)

        assertTrue(code.contains("public static void publicMethod()"))
        assertTrue(code.contains("private static void privateMethod()"))
        assertTrue(code.contains("protected static void protectedMethod()"))
        assertTrue(code.contains("static void defaultMethod()"))
    }
}
