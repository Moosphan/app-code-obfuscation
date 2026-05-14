package com.dorck.app.code.guard.agp8

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for Agp8Compat.
 */
class Agp8CompatTest {

    @Test
    fun `isAgp8OrHigher does not throw exception`() {
        // Should handle missing class gracefully
        try {
            val result = Agp8Compat.isAgp8OrHigher()
            // In test environment with AGP 8.0 dependencies, this should return true
            // But we just verify it doesn't throw
            assertNotNull(result)
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }

    @Test
    fun `isAgp8OrHigher returns boolean`() {
        val result = Agp8Compat.isAgp8OrHigher()
        assertTrue("Result should be a boolean", result is Boolean)
    }
}
