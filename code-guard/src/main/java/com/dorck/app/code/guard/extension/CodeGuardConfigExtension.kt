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
        get() = field?.replace("\\", "/") // Note: Fix path bugs on windows.
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

    override fun toString(): String {
        return """
            {
                enable: $enable,
                mapperFile: $mapper,
                obfuscationDictionary: $obfuscationDict,
                supportIncremental: $supportIncremental,
                processingPackages: $processingPackages,
                isSkipAbsClass: $isSkipAbsClass,
                isSkipJarFilesProcessing: $isSkipJar
                isAutoAdapted: $isInsertCountAutoAdapted,
                maxFieldCount: $maxFieldCount,
                maxMethodCount: $maxMethodCount,
                minMethodCount: $minMethodCount,
                minFieldCount: $minFieldCount,
                methodObfuscateEnable: $methodObfuscateEnable,
                maxCodeLineCount: $maxCodeLineCount,
                generatedClassPkg: $generatedClassPkg,
                generatedClassName: $generatedClassName,
                generatedMethodCount: $generatedClassMethodCount,
                genClassCount: $genClassCount,
                excludeRules: $excludeRules,
                variantConstraints: $variantConstraints,
            }
        """.trimIndent()
    }
}