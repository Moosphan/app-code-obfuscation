package com.dorck.app.code.guard.config

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

    private var mMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()
    val javaCodeGenPath: String by mMap
    val javaCodeGenDir: String by mMap

    var isUseDefaultStrategy = true     // 是否使用插件默认的随机策略
    var isEnableCodeGenInMethod = true  // 是否启用在方法内插入混淆代码(注意监测对包体积和编译时常的影响)
    var excludeRulesList = HashSet<String>()

    fun configJavaCodeGenDir(dir: String) {
        mMap["javaCodeGenDir"] = dir
    }

    fun configJavaCodeGenPath(path: String) {
        mMap["javaCodeGenPath"] = path
    }

    fun getExcludeRules(): HashSet<String> {
        excludeRulesList.addAll(DEFAULT_EXCLUDE_RULES)
        return excludeRulesList
    }

    fun checkExcludes(filePath: String): Boolean {
        val formattedPath = filePath.replace("\\", "/")
        val fileName = File(filePath).name
        val excludeList = getExcludeRules()
        excludeList.forEach {
            val regex = Regex(it)
            if (regex.matches(fileName)) {
                KLogger.error("checkExcludes, exclude file matches: $formattedPath")
                return true
            }
        }
        return false
    }
}