package com.dorck.app.code.guard.utils

import java.util.Random

object RandomTool {
    private val mRandom = Random()
    private const val RANDOM_NUM_BOUND = 60
    private val LOWERCASE_LETTERS = ('a'..'z').toList()
    private val JAVA_KEY_WORDS = hashSetOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "false", "final", "finally", "float", "for", "goto", "if",
        "implements", "import", "instanceof", "int", "interface", "long", "native",
        "new", "null", "package", "private", "protected", "public", "return",
        "short", "static", "strictfp", "super", "switch", "synchronized", "this",
        "throw", "throws", "transient", "true", "try", "void", "volatile", "while"
    )

    fun randomNumber(): Int {
        return mRandom.nextInt(RANDOM_NUM_BOUND)
    }

    fun randomBool(): Boolean {
        return mRandom.nextBoolean()
    }

    fun randomChar(): Char {
        return LOWERCASE_LETTERS[mRandom.nextInt(LOWERCASE_LETTERS.size)]
    }

    fun randomByte(): Byte {
        return mRandom.nextInt(Byte.MAX_VALUE.toInt()).toByte()
    }

    fun isHitJavaKeyWords(word: String): Boolean {
        return JAVA_KEY_WORDS.contains(word)
    }
}