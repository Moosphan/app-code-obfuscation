package com.dorck.app.code.guard.obfuscate

import org.objectweb.asm.Opcodes

/**
 * Used to randomly generate obfuscated codes.
 */
object RandomCodeObfuscator: AbsCodeObfuscator() {
    private const val CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    // The max chars length of filed or method name.
    private const val FIELD_NAME_MAX_LEN = 1
    private const val METHOD_NAME_MAX_LEN = 2
    private const val FIELD_NAME_PREFIX = "v"
    private const val METHOD_NAME_PREFIX = "m"
    private const val BASE_NAME_PREFIX = ""

    private val ACCESS_TYPES = arrayListOf(Opcodes.ACC_PRIVATE, Opcodes.ACC_PROTECTED, Opcodes.ACC_PUBLIC)
    private val BASIC_TYPES = arrayListOf("B", "S", "I", "J", "F", "D", "C", "Z", "Ljava/lang/String;")
    private val METHOD_PARAM_TYPES = arrayListOf("(B)V", "(S)V", "(I)V", "(J)V", "(F)V", "(D)V", "(C)V", "(Z)V", "(Ljava/lang/String;)V")

    override fun nextFiled(): FieldEntity {
        val name = generateRandomName(maxLength = FIELD_NAME_MAX_LEN)
        val accessType = generateRandomAccess()
        val type = generateRandomType()
        return FieldEntity(name, accessType, type)
    }

    override fun nextMethod(): MethodEntity { // 目前仅支持随机生成无返回值函数
        val name = generateRandomName(maxLength = FIELD_NAME_MAX_LEN)
        val desc = generateRandomDescriptor()
        return MethodEntity(name, desc)
    }

    override fun nextCodeCall(): MethodEntity {
        // 从生成的类中随机获取一个方法调用
        TODO("Not yet implemented")
    }

    /**
     * 随机生成属性名or类名
     */
    private fun generateRandomName(prefix: String = BASE_NAME_PREFIX, maxLength: Int = 6): String {
        val sb = StringBuilder(prefix)
        for (i in 0 until maxLength - prefix.length) {
            val randomChar: Char = CHARACTERS[random.nextInt(CHARACTERS.length)]
            sb.append(randomChar)
        }
        return sb.toString()
    }

    /**
     * 随机生成数据类型
     */
    private fun generateRandomType(): String {
        return BASIC_TYPES[random.nextInt(BASIC_TYPES.size)]
    }

    /**
     * 随机生成访问权限
     */
    private fun generateRandomAccess(): Int {
        return ACCESS_TYPES[random.nextInt(ACCESS_TYPES.size)] + randomStaticAccess()
    }

    /**
     * 随机生成静态访问权限
     */
    private fun randomStaticAccess(): Int = if (random.nextBoolean()) Opcodes.ACC_STATIC else 0

    fun typeParamOpcode(type: String): Int {
        return when (type) {
            "I", "S", "B", "C", "Z" -> Opcodes.ILOAD
            "J" -> Opcodes.LLOAD
            "F" -> Opcodes.FLOAD
            "D" -> Opcodes.DLOAD
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    fun typeReturnOpcode(type: String): Int {
        return when (type) {
            "V" -> Opcodes.RETURN
            "I", "S", "B", "C", "Z" -> Opcodes.IRETURN
            "J" -> Opcodes.LRETURN
            "F" -> Opcodes.FRETURN
            "D" -> Opcodes.DRETURN
            else -> throw IllegalArgumentException("Unsupported type: $type")
        }
    }

    /**
     * Note: 目前仅支持无返回值的方法描述符
     * B: byte
     * C: char
     * D: double
     * F: float
     * I: int
     * J: long
     * S: short
     * Z: boolean
     * V: void
     * L<类名>;: 对象引用类型，使用分号结尾，如 Ljava/lang/String;
     */
    private fun generateRandomDescriptor(): String {
        return "(${BASIC_TYPES[random.nextInt(BASIC_TYPES.size)]})V"
    }
}