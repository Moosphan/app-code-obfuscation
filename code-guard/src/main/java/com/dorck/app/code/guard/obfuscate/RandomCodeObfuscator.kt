package com.dorck.app.code.guard.obfuscate

import com.dorck.app.code.guard.config.AppCodeGuardConfig
import com.dorck.app.code.guard.utils.DLogger
import org.objectweb.asm.Opcodes

/**
 * Used to randomly generate obfuscated codes.
 */
object RandomCodeObfuscator: AbsCodeObfuscator() {
    private const val CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"
    // The max chars length of filed or method name.
    private const val FIELD_NAME_MAX_LEN = 5 // 增加随机概率，防止变量名冲突
    private const val METHOD_NAME_MAX_LEN = 2
    private const val FIELD_NAME_PREFIX = "v"
    private const val METHOD_NAME_PREFIX = "m"
    private const val BASE_NAME_PREFIX = ""
    private val LOWERCASE_LETTERS = ('a'..'z').toList()
    private val CAPITAL_LETTERS = ('A'..'Z').toList()

    private val ACCESS_TYPES = arrayListOf(Opcodes.ACC_PRIVATE, Opcodes.ACC_PROTECTED, Opcodes.ACC_PUBLIC)
    private val BASIC_TYPES = arrayListOf("B", "S", "I", "J", "F", "D", "C", "Z", "Ljava/lang/String;")
    private val METHOD_PARAM_TYPES = arrayListOf("()V", "(B)V", "(S)V", "(I)V", "(J)V", "(F)V", "(D)V", "(C)V", "(Z)V", "(Ljava/lang/String;)V")
    // 用于插入代码调用的目标类(可通过插件自定义或使用默认策略)
    override var mClassEntity: SimpleClassEntity? = null
    override var mGenClassList: MutableList<SimpleClassEntity> = mutableListOf()

    override fun initialize() {
        if (AppCodeGuardConfig.isEnableCodeObfuscateInMethod) {
            // Reset data.
            resetData()
            // Generate code calling classes in batches.
            val genClassCount = AppCodeGuardConfig.genClassCount
            if (genClassCount > 0) {
                repeat(genClassCount) {
                    var packageName = randomPackageName()
                    if (AppCodeGuardConfig.genClassPkgName.isNotEmpty()) {
                        packageName = AppCodeGuardConfig.genClassPkgName
                    }
                    var clzName = randomShortClassName()
                    while (checkIfClassExist(packageName, clzName)) {
                        clzName = randomShortClassName()
                    }
                    // Note: Class name 格式需要为: 包名 + 类名
                    val classEntity = SimpleClassEntity(packageName, clzName,
                        generateMethodList(
                            convertToPathFormat("$packageName.$clzName"),
                            random.nextInt(6) + 10
                        )
                    )
                    mGenClassList.add(classEntity)
                }
                // Initialize package exist state.
                val pkgExistStates = AppCodeGuardConfig.packageExistStates
                mGenClassList.forEach {
                    AppCodeGuardConfig.genPackagePaths.add(it.pkgName)
                    pkgExistStates["${it.pkgName}.${it.className}"] = null
                }
                DLogger.error("initialize, gen pkg paths: ${AppCodeGuardConfig.genPackagePaths}")
            }
        }
    }

    private fun resetData() {
        AppCodeGuardConfig.resetGenData()
        mGenClassList.clear()
    }

    override fun nextField(): FieldEntity {
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

    override fun nextCodeCall(): MethodEntity? {
        // 从生成的类中随机获取一个方法调用
        if (mGenClassList.isEmpty()) {
            return null
        }
        val methodCalls = mutableListOf<MethodEntity>()
        mGenClassList.forEach {
            methodCalls.addAll(it.methods)
        }
        return methodCalls[random.nextInt(methodCalls.size)]
    }

    /**
     * 随机生成属性名 or 方法名
     */
    fun generateRandomName(prefix: String = BASE_NAME_PREFIX, maxLength: Int = 6): String {
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
     * 生成随机的类名，format: 两位字符(大写+小写)
     */
    private fun randomShortClassName(): String {
        val first = CAPITAL_LETTERS.random()
        val second = LOWERCASE_LETTERS.random()
        return "$first$second"
    }

    /**
     * 生成随机包名，format: applicationId + x.y.z
     */
    private fun randomPackageName(): String {
        val packageDepth = (2..4).random() // 随机选择包深度，可以根据需要调整范围
        val alphabet = ('a'..'z').toList()
        val subPkg = List(packageDepth) { alphabet.random().toString() }.joinToString(".")
        return "${AppCodeGuardConfig.applicationId}.$subPkg"
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

    /**
     * 支持多参数的随机方法签名
     * @param range 参数数量随机变化范围
     */
    private fun genMultiRandomDescriptor(range: Int = 5): String {
        val paramCount = random.nextInt(range)
        val desc = StringBuilder("(")
        repeat(paramCount) {
            desc.append(BASIC_TYPES[random.nextInt(BASIC_TYPES.size)])
        }
        desc.append(")V")
        return desc.toString()
    }

    /**
     * 随机生成指定数量的空方法
     * Note: [className]需要拼接为全路径限定名 e.g, `androidx/appcompat/app/AppCompatActivity`
     */
    private fun generateMethodList(className: String = "", count: Int = 3): List<MethodEntity> {
        val genMethodList = mutableListOf<MethodEntity>()
        repeat(count) {
            // 注意这个类生成的方法必须是 public static 类型的
            val name = generateRandomName(maxLength = 3)
            val desc = genMultiRandomDescriptor()
            // 方法去重处理，防止类编译出错
            if (isMethodAlreadyExist(name, desc, genMethodList)) {
                return@repeat
            }
            val methodEntity = MethodEntity(
                name,
                desc,
                className = className,
                access = Opcodes.ACC_PUBLIC,
                isStatic = true
            )
            genMethodList.add(methodEntity)
        }
        return genMethodList
    }

    private fun isMethodAlreadyExist(name: String, desc: String, methodList: List<MethodEntity>): Boolean {
        if (methodList.isEmpty()) {
            return false
        }
        methodList.forEach {
            if (name == it.name && desc == it.desc) {
                return true
            }
        }
        return false
    }

    private fun checkIfClassExist(packageName: String, className: String): Boolean {
        if (mGenClassList.isEmpty()) {
            return false
        }
        mGenClassList.forEach {
            if (it.className == className && it.pkgName == packageName) {
                return true
            }
        }
        return false
    }

    fun convertToPathFormat(className: String): String {
        return className.replace('.', '/')
    }
}