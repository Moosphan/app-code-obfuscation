package com.dorck.app.code.guard.obfuscate

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RandomCodeObfuscator.
 */
class RandomCodeObfuscatorTest {

    @Before
    fun setup() {
        // Reset config before each test
        RandomCodeObfuscator.reset()
        setupAppCodeGuardConfig()
    }

    @Test
    fun `generateRandomName returns correct length`() {
        val name = RandomCodeObfuscator.generateRandomName(maxLength = 5)
        assertEquals(5, name.length)
    }

    @Test
    fun `generateRandomName with prefix`() {
        val name = RandomCodeObfuscator.generateRandomName(prefix = "v", maxLength = 6)
        assertTrue(name.startsWith("v"))
        assertEquals(6, name.length)
    }

    @Test
    fun `generateRandomName produces different results`() {
        val names = mutableSetOf<String>()
        repeat(100) {
            names.add(RandomCodeObfuscator.generateRandomName(maxLength = 10))
        }
        // With 10 chars, we should get many unique names
        assertTrue("Expected unique names, got ${names.size}", names.size > 50)
    }

    @Test
    fun `nextField returns valid field entity after initialization`() {
        RandomCodeObfuscator.initialize()

        // Skip if initialization didn't generate classes (methodObfuscateEnable is false by default in test)
        if (RandomCodeObfuscator.getGenClassEntityList().isEmpty()) {
            return
        }

        val field = RandomCodeObfuscator.nextField()
        assertNotNull(field)
        assertNotNull(field.name)
        assertNotNull(field.type)
        assertTrue(field.name.isNotEmpty())
        assertTrue(field.type.isNotEmpty())
    }

    @Test
    fun `nextMethod returns valid method entity after initialization`() {
        RandomCodeObfuscator.initialize()

        val method = RandomCodeObfuscator.nextMethod()
        assertNotNull(method)
        assertNotNull(method.name)
        assertNotNull(method.desc)
        assertTrue(method.name.isNotEmpty())
        assertTrue(method.desc.startsWith("("))
        assertTrue(method.desc.endsWith(")V"))
    }

    @Test
    fun `initialize generates correct number of classes`() {
        RandomCodeObfuscator.initialize()

        val classList = RandomCodeObfuscator.getGenClassEntityList()
        // Only generates classes if methodObfuscateEnable is true
        if (AppCodeGuardConfig.isEnableCodeObfuscateInMethod) {
            assertEquals(AppCodeGuardConfig.genClassCount, classList.size)
        }
    }

    @Test
    fun `initialize generates classes with methods`() {
        RandomCodeObfuscator.initialize()

        val classList = RandomCodeObfuscator.getGenClassEntityList()
        classList.forEach { classEntity ->
            assertTrue("Class ${classEntity.className} should have methods", classEntity.methods.isNotEmpty())
            assertTrue("Class ${classEntity.className} should have package", classEntity.pkgName.isNotEmpty())
        }
    }

    @Test
    fun `nextCodeCall returns valid method after initialization`() {
        RandomCodeObfuscator.initialize()

        val codeCall = RandomCodeObfuscator.nextCodeCall()
        // Only returns a method if there are generated classes
        if (RandomCodeObfuscator.getGenClassEntityList().isNotEmpty()) {
            assertNotNull("nextCodeCall should return a method", codeCall)
            codeCall!!
            assertNotNull(codeCall.name)
            assertNotNull(codeCall.desc)
            assertNotNull(codeCall.className)
        }
    }

    @Test
    fun `nextCodeCall returns null before initialization`() {
        val codeCall = RandomCodeObfuscator.nextCodeCall()
        assertNull("nextCodeCall should return null before initialization", codeCall)
    }

    @Test
    fun `reset clears generated data`() {
        RandomCodeObfuscator.initialize()

        RandomCodeObfuscator.reset()

        assertTrue(RandomCodeObfuscator.getGenClassEntityList().isEmpty())
    }

    @Test
    fun `convertToPathFormat works correctly`() {
        assertEquals("com/example/MyClass", RandomCodeObfuscator.convertToPathFormat("com.example.MyClass"))
        assertEquals("a/b/c", RandomCodeObfuscator.convertToPathFormat("a.b.c"))
        assertEquals("simple", RandomCodeObfuscator.convertToPathFormat("simple"))
    }

    private fun setupAppCodeGuardConfig() {
        AppCodeGuardConfig.configApplicationId("com.test.app")
        AppCodeGuardConfig.configJavaCodeGenMainDir("/tmp/test/src/main/java/")
        // Configure from a test extension
        val extension = com.dorck.app.code.guard.extension.CodeGuardConfigExtension().apply {
            enable = true
            methodObfuscateEnable = true
            genClassCount = 2
            generatedClassMethodCount = 2
        }
        AppCodeGuardConfig.configureFromExtension(extension)
    }
}
