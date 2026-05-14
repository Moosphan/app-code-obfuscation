package com.dorck.app.code.guard.extension

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for CodeGuardConfigExtension.
 */
class CodeGuardConfigExtensionTest {

    @Test
    fun `default values are correct`() {
        val extension = CodeGuardConfigExtension()

        assertTrue(extension.enable)
        assertEquals("", extension.obfuscationDict)
        assertTrue(extension.processingPackages.isEmpty())
        assertTrue(extension.isSkipAbsClass)
        assertEquals(AppCodeGuardConfig.DEFAULT_MAX_METHOD_COUNT, extension.maxMethodCount)
        assertEquals(AppCodeGuardConfig.DEFAULT_MAX_FIELD_COUNT, extension.maxFieldCount)
        assertEquals(AppCodeGuardConfig.DEFAULT_MIN_METHOD_COUNT, extension.minMethodCount)
        assertEquals(AppCodeGuardConfig.DEFAULT_MIN_FIELD_COUNT, extension.minFieldCount)
        assertTrue(extension.methodObfuscateEnable)
        assertEquals(6, extension.maxCodeLineCount)
        assertTrue(extension.isInsertCountAutoAdapted)
        assertEquals("", extension.generatedClassPkg)
        assertEquals("", extension.generatedClassName)
        assertEquals(3, extension.generatedClassMethodCount)
        assertEquals(3, extension.genClassCount)
        assertTrue(extension.excludeRules.isEmpty())
        assertTrue(extension.variantConstraints.isEmpty())
        assertFalse(extension.isSkipJar)
    }

    @Test
    fun `obfuscationDict normalizes path separators`() {
        val extension = CodeGuardConfigExtension()
        extension.obfuscationDict = "C:\\Users\\test\\config.json"
        assertEquals("C:/Users/test/config.json", extension.obfuscationDict)
    }

    @Test
    fun `obfuscationDict handles forward slashes`() {
        val extension = CodeGuardConfigExtension()
        extension.obfuscationDict = "/Users/test/config.json"
        assertEquals("/Users/test/config.json", extension.obfuscationDict)
    }

    @Test
    fun `processingPackages can be modified`() {
        val extension = CodeGuardConfigExtension()
        extension.processingPackages = hashSetOf("com.example.app", "com.example.lib")

        assertEquals(2, extension.processingPackages.size)
        assertTrue(extension.processingPackages.contains("com.example.app"))
        assertTrue(extension.processingPackages.contains("com.example.lib"))
    }

    @Test
    fun `excludeRules can be modified`() {
        val extension = CodeGuardConfigExtension()
        extension.excludeRules = hashSetOf("R\\.class", "BuildConfig\\.class")

        assertEquals(2, extension.excludeRules.size)
    }

    @Test
    fun `variantConstraints can be modified`() {
        val extension = CodeGuardConfigExtension()
        extension.variantConstraints = hashSetOf("release")

        assertEquals(1, extension.variantConstraints.size)
        assertTrue(extension.variantConstraints.contains("release"))
    }

    @Test
    fun `toString returns formatted config`() {
        val extension = CodeGuardConfigExtension()
        val str = extension.toString()

        assertTrue(str.contains("enable: true"))
        assertTrue(str.contains("maxFieldCount:"))
        assertTrue(str.contains("maxMethodCount:"))
    }
}
