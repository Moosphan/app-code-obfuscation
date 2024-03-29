package com.dorck.app.code.guard.obfuscate

/**
 * Use to parse code obfuscation configs from local `code_obfuscate_config_json`.
 * @author Dorck
 * @since 2023/11/24
 */
object CustomCodeObfuscator : AbsCodeObfuscator() {
    private const val SEPARATOR = "#"
    val mFieldsList = mutableListOf<FieldEntity>()
    val mMethodsList = mutableListOf<MethodEntity>()
    val mCodeCallList = mutableListOf<MethodEntity>()
    // 避免被混淆的白名单(仅支持到类级别)
    val mWhitelist = mutableListOf<String>()

    fun isFieldExist(name: String, descriptor: String): Boolean {
        if (mFieldsList.isEmpty()) {
            return false
        }
        mFieldsList.forEach {
            if (name == it.name && descriptor == it.type) {
                return true
            }
        }
        return false
    }

    override var mGenClassList: MutableList<SimpleClassEntity> = mutableListOf()

    override fun initialize() {
        // TODO: 解析json配置文件
    }

    override fun nextField(): FieldEntity {
        val randomIndex = random.nextInt(mFieldsList.size)
        return mFieldsList[randomIndex]
    }

    override fun nextMethod(): MethodEntity {
        val randomIndex = random.nextInt(mMethodsList.size)
        return mMethodsList[randomIndex]
    }

    override fun nextCodeCall(): MethodEntity {
        val randomIndex = random.nextInt(mCodeCallList.size)
        return mCodeCallList[randomIndex]
    }
}