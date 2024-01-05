package com.dorck.app.code.guard.utils

import com.dorck.app.code.guard.obfuscate.ParameterEntity
import com.dorck.app.code.guard.obfuscate.SimpleClassEntity
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type

/**
 * Generate java format code from [SimpleClassEntity].
 * @author Dorck & ChatGPT
 * @since 2023/12/05
 */
object CodeGenerator {

    fun generateJavaClass(classEntity: SimpleClassEntity): String {
        val codeBuilder = StringBuilder()

        // 添加包名
        codeBuilder.append("package ${classEntity.pkgName};\n\n")

        // 添加类定义
        codeBuilder.append("public class ${classEntity.className} {\n")

        // 添加方法定义
        for (method in classEntity.methods) {
            codeBuilder.append("\n    ")
            codeBuilder.append(getAccessModifier(method.access))
            if (method.isStatic) {
                codeBuilder.append("static ")
            }
            codeBuilder.append("void ${method.name}(")
            val paramList = parseMethodParams(method.desc)
            for (i in paramList.indices) {
                val parameter = paramList[i]
                codeBuilder.append("${parameter.type} ${parameter.name}")
                if (i < paramList.size - 1) {
                    codeBuilder.append(", ")
                }
            }
            codeBuilder.append(") {\n")
            codeBuilder.append("    \n")
            codeBuilder.append("    }\n")
        }

        // 添加结束花括号
        codeBuilder.append("}\n")

        return codeBuilder.toString()
    }

    private fun parseMethodParams(desc: String): List<ParameterEntity> {
        val paramTypes = Type.getArgumentTypes(desc)
        var index = 0
        val parameters = mutableListOf<ParameterEntity>()
        paramTypes.forEach { paramType ->
            val name = "param${index++}" // Generate a default parameter name
            parameters.add(ParameterEntity(parseType(paramType.descriptor), name))
        }
        return parameters
    }

    private fun parseType(paramDesc: String): String {
        DLogger.error("parseType, desc: $paramDesc")
        return when (paramDesc) {
            "V" -> "void"
            "Z" -> "boolean"
            "B" -> "byte"
            "S" -> "short"
            "C" -> "char"
            "I" -> "int"
            "J" -> "long"
            "F" -> "float"
            "D" -> "double"
            "Ljava/lang/String;" -> "java.lang.String"
            else -> throw IllegalArgumentException("Unsupported type: $paramDesc")
        }
    }

    private fun parseMethodDesc(desc: String): List<ParameterEntity> {
        val parameters = mutableListOf<ParameterEntity>()
        val descArray = desc.toCharArray()
        var currentIndex = 1 // Skip the leading '('

        while (descArray[currentIndex] != ')') {
            val pairValue = parseType(descArray, currentIndex)
            val name = "param${parameters.size + 1}" // Generate a default parameter name
            parameters.add(ParameterEntity(pairValue.first, name))
            currentIndex = pairValue.second
        }

        return parameters
    }

    private fun parseType(descArray: CharArray, currentIndex: Int): Pair<String, Int> {
        DLogger.error("parseType, desc: ${String(descArray)}")
        return when (descArray[currentIndex]) {
            'V' -> Pair("void", currentIndex + 1)
            'Z' -> Pair("boolean", currentIndex + 1)
            'B' -> Pair("byte", currentIndex + 1)
            'S' -> Pair("short", currentIndex + 1)
            'C' -> Pair("char", currentIndex + 1)
            'I' -> Pair("int", currentIndex + 1)
            'J' -> Pair("long", currentIndex + 1)
            'F' -> Pair("float", currentIndex + 1)
            'D' -> Pair("double", currentIndex + 1)
            'L' -> {
                val start = currentIndex + 1
                var end = descArray.indexOfFirst { it == ';' }
                for (i in descArray.indices) {
                    if (descArray[i] == ';' && i > start) {
                        end = i
                    }
                }
                DLogger.error("parseType, string >> [$start, $end], ${String(descArray, start, end - start).replace('/', '.')}")
                if (end == -1) {
                    throw IllegalArgumentException("Missing ';' for object type descriptor")
                }
                Pair(String(descArray, start, end - start).replace('/', '.'), end + 1)
            }
            '[' -> parseArrayType(descArray, currentIndex)
            else -> throw IllegalArgumentException("Unsupported type: ${descArray[currentIndex]}, method desc: ${String(descArray)}")
        }
    }


    private fun parseArrayType(descArray: CharArray, currentIndex: Int): Pair<String, Int> {
        var index = currentIndex
        while (descArray[index] == '[') {
            index++
        }
        val (elementType, newIndex) = parseType(descArray, index)
        return Pair("$elementType[]", newIndex)
    }

    private fun getAccessModifier(access: Int): String {
        return when {
            (access and Opcodes.ACC_PUBLIC) != 0 -> "public "
            (access and Opcodes.ACC_PROTECTED) != 0 -> "protected "
            (access and Opcodes.ACC_PRIVATE) != 0 -> "private "
            else -> ""
        }
    }

}