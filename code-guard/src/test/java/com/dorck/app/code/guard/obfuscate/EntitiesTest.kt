package com.dorck.app.code.guard.obfuscate

import org.junit.Assert.*
import org.junit.Test
import org.objectweb.asm.Opcodes

/**
 * Unit tests for data entities (FieldEntity, MethodEntity, etc.).
 */
class EntitiesTest {

    @Test
    fun `FieldEntity creation with correct values`() {
        val field = FieldEntity(
            name = "testField",
            access = Opcodes.ACC_PRIVATE,
            type = "I"
        )
        assertEquals("testField", field.name)
        assertEquals(Opcodes.ACC_PRIVATE, field.access)
        assertEquals("I", field.type)
        assertTrue(field.isInserted) // default is true
    }

    @Test
    fun `FieldEntity with isInserted false`() {
        val field = FieldEntity(
            name = "originalField",
            access = Opcodes.ACC_PUBLIC,
            type = "Ljava/lang/String;",
            isInserted = false
        )
        assertFalse(field.isInserted)
    }

    @Test
    fun `FieldEntity toString returns JSON`() {
        val field = FieldEntity("test", Opcodes.ACC_PUBLIC, "I")
        val json = field.toString()
        assertTrue(json.contains("test"))
        assertTrue(json.contains("I"))
    }

    @Test
    fun `MethodEntity creation with default values`() {
        val method = MethodEntity(
            name = "testMethod",
            desc = "()V"
        )
        assertEquals("testMethod", method.name)
        assertEquals("()V", method.desc)
        assertEquals(Opcodes.ACC_PRIVATE, method.access) // default
        assertEquals("", method.className) // default
        assertFalse(method.isStatic) // default
        assertTrue(method.fromInsert) // default
    }

    @Test
    fun `MethodEntity creation with custom values`() {
        val method = MethodEntity(
            name = "staticMethod",
            desc = "(I)V",
            access = Opcodes.ACC_PUBLIC,
            className = "com/example/Helper",
            isStatic = true,
            fromInsert = false
        )
        assertEquals("staticMethod", method.name)
        assertEquals("(I)V", method.desc)
        assertEquals(Opcodes.ACC_PUBLIC, method.access)
        assertEquals("com/example/Helper", method.className)
        assertTrue(method.isStatic)
        assertFalse(method.fromInsert)
    }

    @Test
    fun `MethodEntity toString returns JSON`() {
        val method = MethodEntity("test", "()V")
        val json = method.toString()
        assertTrue(json.contains("test"))
        assertTrue(json.contains("()V"))
    }

    @Test
    fun `SimpleClassEntity creation`() {
        val methods = listOf(
            MethodEntity("m1", "()V"),
            MethodEntity("m2", "(I)V")
        )
        val classEntity = SimpleClassEntity(
            pkgName = "com.example",
            className = "TestClass",
            methods = methods
        )
        assertEquals("com.example", classEntity.pkgName)
        assertEquals("TestClass", classEntity.className)
        assertEquals(2, classEntity.methods.size)
        assertTrue(classEntity.isJava) // default is true
    }

    @Test
    fun `SimpleClassEntity with isJava false`() {
        val classEntity = SimpleClassEntity(
            pkgName = "com.example",
            className = "KtClass",
            methods = emptyList(),
            isJava = false
        )
        assertFalse(classEntity.isJava)
    }

    @Test
    fun `ParameterEntity creation`() {
        val param = ParameterEntity(type = "int", name = "param0")
        assertEquals("int", param.type)
        assertEquals("param0", param.name)
    }
}
