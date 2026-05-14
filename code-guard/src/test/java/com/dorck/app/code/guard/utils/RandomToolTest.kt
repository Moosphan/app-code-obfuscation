package com.dorck.app.code.guard.utils

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for RandomTool utility.
 */
class RandomToolTest {

    @Test
    fun `randomNumber returns integer`() {
        val number = RandomTool.randomNumber()
        assertTrue(number is Int)
    }

    @Test
    fun `randomBool returns boolean`() {
        val bool = RandomTool.randomBool()
        assertTrue(bool is Boolean)
    }

    @Test
    fun `randomChar returns character`() {
        val char = RandomTool.randomChar()
        assertTrue(char is Char)
    }

    @Test
    fun `randomByte returns byte`() {
        val byte = RandomTool.randomByte()
        assertTrue(byte is Byte)
    }

    @Test
    fun `isHitJavaKeyWords returns true for keywords`() {
        assertTrue(RandomTool.isHitJavaKeyWords("abstract"))
        assertTrue(RandomTool.isHitJavaKeyWords("class"))
        assertTrue(RandomTool.isHitJavaKeyWords("public"))
        assertTrue(RandomTool.isHitJavaKeyWords("static"))
        assertTrue(RandomTool.isHitJavaKeyWords("void"))
        assertTrue(RandomTool.isHitJavaKeyWords("int"))
        assertTrue(RandomTool.isHitJavaKeyWords("return"))
    }

    @Test
    fun `isHitJavaKeyWords returns false for non-keywords`() {
        assertFalse(RandomTool.isHitJavaKeyWords("myVariable"))
        assertFalse(RandomTool.isHitJavaKeyWords("testMethod"))
        assertFalse(RandomTool.isHitJavaKeyWords("randomName"))
    }

    @Test
    fun `isHitJavaKeyWords is case sensitive`() {
        // Keywords are lowercase
        assertFalse(RandomTool.isHitJavaKeyWords("Abstract"))
        assertFalse(RandomTool.isHitJavaKeyWords("Class"))
        assertFalse(RandomTool.isHitJavaKeyWords("Public"))
    }
}
