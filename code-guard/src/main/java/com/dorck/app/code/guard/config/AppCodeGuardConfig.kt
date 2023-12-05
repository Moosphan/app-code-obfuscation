package com.dorck.app.code.guard.config

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.obfuscate.RandomCodeObfuscator
import com.dorck.app.code.guard.utils.KLogger
import org.gradle.kotlin.dsl.provideDelegate
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Use to configure general props from extension.
 *
 * @author Dorck
 * @since 2023/12/04
 */
object AppCodeGuardConfig {
    const val MAX_FIELD_COUNT = 10
    const val MAX_METHOD_COUNT = 8
    const val MIN_FIELD_COUNT = 5
    const val MIN_METHOD_COUNT = 2

    // 默认的排除规则，需要规避掉系统使用的特殊class
    val DEFAULT_EXCLUDE_RULES = listOf("(R\\.class|BuildConfig\\.class)")

    private var mMap: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
    val javaCodeGenPath: String by mMap
    val javaCodeGenDir: String by mMap
    val applicationId: String by mMap
    // Class generation config(生成供目标混淆函数内生成代码调用的类).
    val genClassName: String by mMap
    val genClassPkgName: String by mMap
    val genClassMethodCount: Int by mMap

    val isUseDefaultStrategy: Boolean by mMap           // 是否使用插件默认的随机策略
    val isEnableCodeObfuscateInMethod: Boolean by mMap  // 是否启用在方法内插入混淆代码(注意监测对包体积和编译时常的影响)
    val excludeRulesList: HashSet<String> by mMap

    fun configureFromExtension(extension: CodeGuardConfigExtension) {
        mMap["genClassName"] = extension.generatedClassName
        mMap["genClassPkgName"] = extension.generatedClassPkg
        mMap["genClassMethodCount"] = extension.generatedMethodCount
        mMap["isUseDefaultStrategy"] = !CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)
        mMap["isEnableCodeObfuscateInMethod"] = extension.methodObfuscateEnable
        mMap["excludeRulesList"] = extension.excludeRules
        readConfigs()
    }

    fun configJavaCodeGenDir(dir: String) {
        mMap["javaCodeGenDir"] = dir
    }

    fun configJavaCodeGenPath(path: String) {
        mMap["javaCodeGenPath"] = path
    }

    fun configApplicationId(appId: String) {
        mMap["applicationId"] = appId
    }

    fun configGenClassName(className: String) {
        mMap["genClassName"] = className
    }

    fun configGenClassPkgName(pkg: String) {
        mMap["genClassPkgName"] = pkg
    }

    fun configGenClassMethodCount(methodCount: Int) {
        mMap["genClassMethodCount"] = methodCount
    }

    fun getExcludeRules(): HashSet<String> {
        excludeRulesList.addAll(DEFAULT_EXCLUDE_RULES)
        return excludeRulesList
    }

    fun checkExcludes(filePath: String): Boolean {
        val formattedPath = filePath.replace("\\", "/")
        val fileName = File(filePath).name
        // 根据白名单规则过滤类文件
        val excludeList = getExcludeRules()
        excludeList.forEach {
            val regex = Regex(it)
            val genClzPath = RandomCodeObfuscator.convertToPathFormat("$genClassPkgName.$genClassName")
            KLogger.error("checkExcludes, cur gen class path: $genClzPath")
            // Note: 需要将生成类也排除掉，防止被Transform处理产生问题
            if (regex.matches(fileName) || formattedPath.contains(genClzPath)) {
                KLogger.error("checkExcludes, exclude file matches: $formattedPath")
                return true
            }
        }
        return false
    }

    fun readConfigs() {
        KLogger.error(" AppCodeGuardConfig read config: $mMap")
    }
}