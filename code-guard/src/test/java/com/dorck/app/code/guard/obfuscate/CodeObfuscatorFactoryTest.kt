package com.dorck.app.code.guard.obfuscate

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import org.junit.Assert.*
import org.junit.Test
import java.io.File

/**
 * Unit tests for CodeObfuscatorFactory.
 */
class CodeObfuscatorFactoryTest {

    @Test
    fun `getCodeObfuscator returns RandomCodeObfuscator when no dict file`() {
        val extension = CodeGuardConfigExtension().apply {
            obfuscationDict = ""
        }
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        assertTrue(obfuscator is RandomCodeObfuscator)
    }

    @Test
    fun `getCodeObfuscator returns RandomCodeObfuscator for non-existent dict`() {
        val extension = CodeGuardConfigExtension().apply {
            obfuscationDict = "/non/existent/path.json"
        }
        val obfuscator = CodeObfuscatorFactory.getCodeObfuscator(extension)
        assertTrue(obfuscator is RandomCodeObfuscator)
    }

    @Test
    fun `checkFileIfExist returns false for non-existent file`() {
        assertFalse(CodeObfuscatorFactory.checkFileIfExist("/non/existent/file.json"))
    }

    @Test
    fun `checkFileIfExist returns true for existing file`() {
        val tempFile = File.createTempFile("test", ".json")
        try {
            assertTrue(CodeObfuscatorFactory.checkFileIfExist(tempFile.absolutePath))
        } finally {
            tempFile.delete()
        }
    }
}
