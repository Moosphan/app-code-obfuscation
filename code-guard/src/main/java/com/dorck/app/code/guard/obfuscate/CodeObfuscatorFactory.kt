package com.dorck.app.code.guard.obfuscate

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import java.io.File

/**
 * Create obfuscate strategy by user config.
 * @author Dorck
 * @since 2023/11/24
 */
object CodeObfuscatorFactory {
    fun getCodeObfuscator(extension: CodeGuardConfigExtension): IAppCodeObfuscator {
        val obfuscationDisc = extension.obfuscationDict
        if (obfuscationDisc.isNotEmpty() && checkFileIfExist(obfuscationDisc)) {
            return CustomCodeObfuscator
        }
        return RandomCodeObfuscator
    }

    private fun checkFileIfExist(path: String): Boolean {
        val file: File = File(path)
        return file.exists()
    }
}