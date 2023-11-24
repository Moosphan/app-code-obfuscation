package com.dorck.app.code.guard.config

import com.google.gson.Gson
import org.objectweb.asm.Opcodes

data class FieldEntity(
    val name: String,
    val access: Int,
    val type: String
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
    val desc: String
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}