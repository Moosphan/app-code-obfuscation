package com.dorck.app.code.guard.utils

import java.io.*
import java.nio.channels.FileChannel

object IOUtils {
    @Throws(IOException::class)
    fun toByteArray(input: InputStream): ByteArray? {
        val output = ByteArrayOutputStream()
        copy(input, output as OutputStream)
        return output.toByteArray()
    }

    @Throws(IOException::class)
    fun copy(input: InputStream, output: OutputStream): Int {
        val count = copyLarge(input, output)
        return if (count > 2147483647L) -1 else count.toInt()
    }

    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream): Long {
        return copyLarge(input, output, ByteArray(4096))
    }

    @Throws(IOException::class)
    fun copyLarge(input: InputStream, output: OutputStream, buffer: ByteArray?): Long {
        var count = 0L
        var n: Int
        val var5 = false
        while (-1 != input.read(buffer).also { n = it }) {
            output.write(buffer, 0, n)
            count += n.toLong()
        }
        return count
    }

    fun copyFile(source: File, destination: File) {
        var sourceChannel: FileChannel? = null
        var destinationChannel: FileChannel? = null
        try {
            sourceChannel = FileInputStream(source).channel
            destinationChannel = FileOutputStream(destination).channel
            destinationChannel.transferFrom(sourceChannel, 0, sourceChannel.size())
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            try {
                sourceChannel?.close()
                destinationChannel?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}