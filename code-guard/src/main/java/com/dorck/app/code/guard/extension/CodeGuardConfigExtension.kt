package com.dorck.app.code.guard.extension

import com.dorck.app.code.guard.config.AppCodeGuardConfig

/**
 * The extension class for configuring plugin.
 * Note: We need to make sure this class is open to the outside world.
 * @author Dorck
 * @since 2023/11/23
 */
open class CodeGuardConfigExtension: BasePluginExtension() {
    // Mapper rules.
    var mapper: String = ""
    // Used to configure your own code obfuscation dictionary
    var obfuscationDict: String = ""
        get() = field.replace("\\", "/") // Note: Fix path bugs on windows.
    // Configure the package paths for which you want to enhance obfuscation.
    var processingPackages: HashSet<String> = HashSet()
    // Whether to skip obfuscate abstract classes.
    var isSkipAbsClass: Boolean = true
    // Maximum number of methods in a class.
    var maxMethodCount: Int = AppCodeGuardConfig.DEFAULT_MAX_METHOD_COUNT
    // Maximum number of fields in a class.
    var maxFieldCount: Int = AppCodeGuardConfig.DEFAULT_MAX_FIELD_COUNT
    // Minimum number of inserted methods in a class.
    var minMethodCount: Int = AppCodeGuardConfig.DEFAULT_MIN_METHOD_COUNT
    // Minimum number of inserted fields in a class.
    var minFieldCount: Int = AppCodeGuardConfig.DEFAULT_MIN_FIELD_COUNT
    // Whether to enable method obfuscation.
    var methodObfuscateEnable: Boolean = true
    // The maximum number of lines of code allowed to be inserted within a method.
    var maxCodeLineCount: Int = 6
    // Whether to enable automatic adaptation.
    // If enable, plugin will automatically generate a acceptable number of methods and fields
    // based on the specific circumstances of the current class.
    var isInsertCountAutoAdapted: Boolean = true
    // Generated java class package name for code call.
    var generatedClassPkg: String = ""
    // Generated java class name for code call.
    var generatedClassName: String = ""
    // Number of random methods generated in java class.
    var generatedClassMethodCount: Int = 3
    // Number of generated classes.
    var genClassCount: Int = 3
    // Exclude rules which you don't want to obfuscate.
    var excludeRules: HashSet<String> = HashSet() // TODO 12/09 包含白名单职责
    // Specify a collection of variants for obfuscated execution.
    // E.g, `release`, if empty, it will execute obfuscation in all variants.
    var variantConstraints: HashSet<String> = HashSet()
    // 是否处理 Jar
    var isSkipJar: Boolean = false

    // ==================== 兼容别名（deprecated aliases） ====================
    // 历史版本/文档/toString 输出中使用的属性名与真实属性名不一致（issue #17），
    // 这里提供别名 getter/setter 以保证旧配置依然可用，同时保持真实属性名一致。

    /** 别名 of [mapper]（旧配置使用 `mapperFile`） */
    var mapperFile: String
        get() = mapper
        set(value) { mapper = value }

    /** 别名 of [obfuscationDict]（旧配置使用 `obfuscationDictionary`） */
    var obfuscationDictionary: String
        get() = obfuscationDict
        set(value) { obfuscationDict = value }

    /** 别名 of [isInsertCountAutoAdapted]（旧配置使用 `isAutoAdapted`） */
    var isAutoAdapted: Boolean
        get() = isInsertCountAutoAdapted
        set(value) { isInsertCountAutoAdapted = value }

    /** 别名 of [isSkipJar]（旧配置/README 使用 `isSkipJars` 或 `isSkipJarFilesProcessing`） */
    var isSkipJars: Boolean
        get() = isSkipJar
        set(value) { isSkipJar = value }

    /** 别名 of [isSkipJar]（旧 toString 输出使用 `isSkipJarFilesProcessing`） */
    var isSkipJarFilesProcessing: Boolean
        get() = isSkipJar
        set(value) { isSkipJar = value }

    /** 别名 of [generatedClassMethodCount]（文档/toString 输出使用 `generatedMethodCount`） */
    var generatedMethodCount: Int
        get() = generatedClassMethodCount
        set(value) { generatedClassMethodCount = value }

    override fun toString(): String {
        return """
            {
                enable: $enable,
                mapper: $mapper,
                obfuscationDict: $obfuscationDict,
                supportIncremental: $supportIncremental,
                processingPackages: $processingPackages,
                isSkipAbsClass: $isSkipAbsClass,
                isSkipJar: $isSkipJar
                isInsertCountAutoAdapted: $isInsertCountAutoAdapted,
                maxFieldCount: $maxFieldCount,
                maxMethodCount: $maxMethodCount,
                minMethodCount: $minMethodCount,
                minFieldCount: $minFieldCount,
                methodObfuscateEnable: $methodObfuscateEnable,
                maxCodeLineCount: $maxCodeLineCount,
                generatedClassPkg: $generatedClassPkg,
                generatedClassName: $generatedClassName,
                generatedClassMethodCount: $generatedClassMethodCount,
                genClassCount: $genClassCount,
                excludeRules: $excludeRules,
                variantConstraints: $variantConstraints,
            }
        """.trimIndent()
    }
}