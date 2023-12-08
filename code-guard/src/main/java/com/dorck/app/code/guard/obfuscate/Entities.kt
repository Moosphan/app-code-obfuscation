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
    val access: Int = Opcodes.ACC_PRIVATE, // 默认插入private方法
    val className: String = "",
    val isStatic: Boolean = false,
    val fromInsert: Boolean = true
) {
    override fun toString(): String {
        return Gson().toJson(this)
    }
}

data class ParameterEntity(val type: String, val name: String)

// 插入代码的目标类模型
data class SimpleClassEntity(
    val pkgName: String,        // 相对包名，实际上是[applicationId + pkgName]
    val className: String,
    val methods: List<MethodEntity>,
    val isJava: Boolean = true, // 目前仅支持 Java 代码
)