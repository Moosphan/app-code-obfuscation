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

    // ==================== AGP 版本判断（issue #16） ====================

    @Test
    fun `isAgp8OrHigher returns false for AGP 7x`() {
        assertFalse(Agp8Compat.isAgp8OrHigher("7.2.2"))
        assertFalse(Agp8Compat.isAgp8OrHigher("7.0.0"))
        assertFalse(Agp8Compat.isAgp8OrHigher("7.4.2"))
    }

    @Test
    fun `isAgp8OrHigher returns true for AGP 8x`() {
        assertTrue(Agp8Compat.isAgp8OrHigher("8.0.0"))
        assertTrue(Agp8Compat.isAgp8OrHigher("8.2.0"))
        assertTrue(Agp8Compat.isAgp8OrHigher("8.7.0"))
    }

    @Test
    fun `isAgp8OrHigher handles invalid version gracefully`() {
        assertFalse(Agp8Compat.isAgp8OrHigher(""))
        assertFalse(Agp8Compat.isAgp8OrHigher("unknown"))
        assertFalse(Agp8Compat.isAgp8OrHigher("abc.1.0"))
    }
}
