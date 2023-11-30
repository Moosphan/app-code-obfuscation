package com.dorck.app.code.guard.obfuscate

import com.google.gson.Gson
import org.objectweb.asm.Opcodes

data class FieldEntity(
    val name: String,
    val access: Int,
    val type: String, // Field descriptor
    val isInserted: Boolean = true
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }

    companion object {
        fun FieldEntity.from(accessString: String): Int {
            return when(accessString) {
                "public" -> Opcodes.ACC_PUBLIC
                "protected" -> Opcodes.ACC_PROTECTED
                "private" -> Opcodes.ACC_PRIVATE
                else -> 0
            }
        }
    }
}

data class MethodEntity(
    val name: String,
    val desc: String,
    val className: String = "",
    val access: Int = Opcodes.ACC_PRIVATE // 默认插入private方法
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}