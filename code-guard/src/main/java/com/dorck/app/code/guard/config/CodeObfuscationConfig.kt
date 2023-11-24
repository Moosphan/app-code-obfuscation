package com.dorck.app.code.guard.config

import com.dorck.app.code.guard.config.FieldEntity.Companion.getRealAccessType

/**
 * Use to parse code obfuscation configs from local `code_obfuscate_config_json`.
 * @author Dorck
 * @since 2023/11/24
 */
object CodeObfuscationConfig {
    private const val SEPARATOR = "#"
    val mFieldsList = mutableListOf<FieldEntity>()
    val mMethodsList = mutableListOf<MethodEntity>()
    val mWhitelist = mutableListOf<String>()

    fun isFieldExist(name: String, type: Int): Boolean {
        if (mFieldsList.isEmpty()) {
            return false
        }
        mFieldsList.forEach {
            if (name == it.name && type == it.getRealAccessType()) {
                return true
            }
        }
        return false
    }
}