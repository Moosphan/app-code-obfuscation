package com.dorck.app.code.guard.extension

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
    var obfuscationDict: String? = ""
        get() = field?.replace("\\", "/") // Note: Fix path bugs on windows.
    // Configure the package paths for which you want to enhance obfuscation.
    var processingPackages: List<String> = emptyList()
    // Whether to skip obfuscate jars.
    var isSkipJars: Boolean = true
    // Whether to skip obfuscate abstract classes.
    var isSkipAbsClass: Boolean = false
    // Maximum number of methods in a class.
    var maxMethodCount: Int = Int.MAX_VALUE
    // Maximum number of fields in a class.
    var maxFieldCount: Int = Int.MAX_VALUE
    // Whether to enable automatic adaptation.
    // If enable, plugin will automatically generate a acceptable number of methods and fields
    // based on the specific circumstances of the current class.
    var isAutoAdapted: Boolean = true

    override fun toString(): String {
        return """
            {
                enable: $enable,
                mapperFile: $mapper,
                obfuscationDictionary: $obfuscationDict,
                supportIncremental: $supportIncremental,
                processingPackages: $processingPackages,
                isSkipJars: $isSkipJars,
                isSkipAbsClass: $isSkipAbsClass,
                isAutoAdapted: $isAutoAdapted,
                maxFieldCount: $maxFieldCount,
                maxMethodCount: $maxMethodCount
            }
        """.trimIndent()
    }
}