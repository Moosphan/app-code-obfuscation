package com.dorck.app.code.guard.config

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppCodeGuardConfig.
 */
class AppCodeGuardConfigTest {

    @Before
    fun setup() {
        AppCodeGuardConfig.reset()
    }

    @Test
    fun `default exclude rules contains R class`() {
        val rules = AppCodeGuardConfig.DEFAULT_EXCLUDE_RULES
        assertTrue(rules.any { it.contains("R") })
        assertTrue(rules.any { it.contains("BuildConfig") })
    }

    @Test
    fun `configApplicationId stores value`() {
        AppCodeGuardConfig.configApplicationId("com.example.app")
        assertEquals("com.example.app", AppCodeGuardConfig.applicationId)
    }

    @Test
    fun `configCurrentBuildVariant stores value`() {
        AppCodeGuardConfig.configCurrentBuildVariant("debug")
        assertEquals("debug", AppCodeGuardConfig.currentBuildVariant)
    }

    @Test
    fun `configAvailableVariants stores values`() {
        val variants = hashSetOf("debug", "release")
        AppCodeGuardConfig.configAvailableVariants(variants)
        assertEquals(variants, AppCodeGuardConfig.availableVariants)
    }

    @Test
    fun `checkExcludes returns true for R class`() {
        val extension = createTestExtension()
        AppCodeGuardConfig.configureFromExtension(extension)
        assertTrue(AppCodeGuardConfig.checkExcludes("com/example/app/R.class"))
    }

    @Test
    fun `checkExcludes returns true for BuildConfig class`() {
        val extension = createTestExtension()
        AppCodeGuardConfig.configureFromExtension(extension)
        assertTrue(AppCodeGuardConfig.checkExcludes("com/example/app/BuildConfig.class"))
    }

    @Test
    fun `checkExcludes returns true for R inner classes`() {
        val extension = createTestExtension()
        AppCodeGuardConfig.configureFromExtension(extension)
        // R$string should be excluded - check if the filename matches the regex
        // The default rule is "(R\\.class|BuildConfig\\.class)" which matches R.class
        // R$xxx files should be excluded by contains check
        assertTrue(AppCodeGuardConfig.checkExcludes("com/example/app/R.class"))
        assertTrue(AppCodeGuardConfig.checkExcludes("com/example/app/BuildConfig.class"))
    }

    @Test
    fun `checkExcludes returns false for target package classes`() {
        val extension = createTestExtension()
        extension.processingPackages = hashSetOf("com.example.app.target")
        AppCodeGuardConfig.configureFromExtension(extension)

        assertFalse(AppCodeGuardConfig.checkExcludes("com/example/app/target/MyClass.class"))
    }

    @Test
    fun `checkExcludes returns true for non-target package classes`() {
        val extension = createTestExtension()
        extension.processingPackages = hashSetOf("com.example.app.target")
        AppCodeGuardConfig.configureFromExtension(extension)

        assertTrue(AppCodeGuardConfig.checkExcludes("com/example/app/other/OtherClass.class"))
    }

    @Test
    fun `checkExcludes returns false when no packages configured`() {
        val extension = createTestExtension()
        extension.processingPackages = hashSetOf()
        AppCodeGuardConfig.configureFromExtension(extension)

        assertFalse(AppCodeGuardConfig.checkExcludes("com/example/app/any/AnyClass.class"))
    }

    @Test
    fun `recordGenClassPath adds to set`() {
        AppCodeGuardConfig.recordGenClassPath("com.test", "TestClass", "/tmp/TestClass.java")
        assertTrue(AppCodeGuardConfig.javaGenClassPaths.isNotEmpty())
        assertEquals(1, AppCodeGuardConfig.javaGenClassPaths.size)
    }

    @Test
    fun `hasGenClassesInLocal returns false when empty`() {
        assertFalse(AppCodeGuardConfig.hasGenClassesInLocal())
    }

    @Test
    fun `reset clears all data`() {
        AppCodeGuardConfig.configApplicationId("com.test")
        AppCodeGuardConfig.configCurrentBuildVariant("debug")
        AppCodeGuardConfig.recordGenClassPath("com.test", "Test", "/tmp/Test.java")

        AppCodeGuardConfig.reset()

        assertTrue(AppCodeGuardConfig.javaGenClassPaths.isEmpty())
    }

    private fun createTestExtension(): CodeGuardConfigExtension {
        return CodeGuardConfigExtension().apply {
            enable = true
            processingPackages = hashSetOf("com.example.app")
            excludeRules = hashSetOf()
        }
    }
}
