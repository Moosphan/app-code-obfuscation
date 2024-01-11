package com.dorck.app.code.guard.utils

import java.util.Random

object RandomTool {
    private val mRandom = Random()
    private const val RANDOM_NUM_BOUND = 60
    private val LOWERCASE_LETTERS = ('a'..'z').toList()

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
}