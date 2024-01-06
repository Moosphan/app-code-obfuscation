package com.dorck.app.code.guard.config

import com.dorck.app.code.guard.extension.CodeGuardConfigExtension
import com.dorck.app.code.guard.obfuscate.CodeObfuscatorFactory
import com.dorck.app.code.guard.obfuscate.RandomCodeObfuscator
import com.dorck.app.code.guard.utils.DLogger
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
    const val DEFAULT_MAX_FIELD_COUNT = 10
    const val DEFAULT_MAX_METHOD_COUNT = 8
    const val DEFAULT_MIN_FIELD_COUNT = 5
    const val DEFAULT_MIN_METHOD_COUNT = 2

    // 默认的排除规则，需要规避掉系统使用的特殊class
    val DEFAULT_EXCLUDE_RULES = listOf("(R\\.class|BuildConfig\\.class)")

    private var mMap: ConcurrentHashMap<String, Any> = ConcurrentHashMap()
    val javaCodeGenPath: String by mMap
    val javaGenClassPaths: HashSet<GenClassData> by mMap    // 生成的类文件路径
    val javaCodeGenMainDir: String by mMap                  // src/main/java目录
    val applicationId: String by mMap                       // Module所在的applicationId或namespace

    val isUseDefaultStrategy: Boolean by mMap               // 是否使用插件默认的随机策略
    val isEnableCodeObfuscateInMethod: Boolean by mMap      // 是否启用在方法内插入混淆代码(注意监测对包体积和编译时常的影响)
    val processingPackages: HashSet<String> by mMap         // 需要插件进行混淆处理的包路径(若未配置则默认处理所有类)
    val excludeRulesList: HashSet<String> by mMap           // 排除混淆的规则集合
    val availableVariants: HashSet<String> by mMap          // 收集当前project下配置的所有变体(默认`android -> buildTypes`下所有变体都会执行)
    val currentBuildVariant: String by mMap                 // 当前用户执行的 variant (如: 执行 `assembleDebug`)

    // Class generation configs (生成供目标混淆函数内生成代码调用的类).
    val genClassCount: Int by mMap                          // 用于指定生成代码调用的目标类的数量 (可一定程度降低相似度、提高理解难度)
    // TODO: 单class情况待废弃
    val genClassName: String by mMap
    val genClassPkgName: String by mMap
    val genClassMethodCount: Int by mMap

    var isPkgExist: Boolean? = null                     // 生成类之前是否已存在该包名路径(用于防止误删项目源码)
    var packageExistStates: HashMap<String, Boolean?> = hashMapOf()
    var genPackagePaths: HashSet<String> = hashSetOf()  // 生成类的包路径

    fun configureFromExtension(extension: CodeGuardConfigExtension) {
        mMap["genClassName"] = extension.generatedClassName
        mMap["genClassPkgName"] = extension.generatedClassPkg
        mMap["genClassMethodCount"] = extension.generatedMethodCount
        mMap["genClassCount"] = extension.genClassCount
        mMap["isUseDefaultStrategy"] = !CodeObfuscatorFactory.checkFileIfExist(extension.obfuscationDict)
        mMap["isEnableCodeObfuscateInMethod"] = extension.methodObfuscateEnable
        mMap["processingPackages"] = extension.processingPackages
        mMap["excludeRulesList"] = extension.excludeRules.also {
            it.addAll(DEFAULT_EXCLUDE_RULES)
        }
        mMap["javaGenClassPaths"] = HashSet<String>()
        readConfigs()
    }

    fun configJavaCodeGenMainDir(dir: String) {
        mMap["javaCodeGenMainDir"] = dir
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

    fun configPackageExistState(isExist: Boolean) {
        isPkgExist = isExist
    }

    fun configAvailableVariants(variants: HashSet<String>) {
        mMap["availableVariants"] = variants
    }

    fun configCurrentBuildVariant(variant: String) {
        mMap["currentBuildVariant"] = variant
    }

    fun recordGenClassPath(packageName: String, className: String, absolutePath: String) {
        javaGenClassPaths.add(GenClassData(packageName, className, absolutePath))
    }

    private fun getExcludeRules(): HashSet<String> {
        return excludeRulesList
    }

    fun resetGenData() {
        genPackagePaths.clear()
        packageExistStates.clear()
    }

    /**
     * Used to detect whether the specified file path needs to be obfuscated.
     * If it is not allowed to be processed, returns true.
     */
    fun checkExcludes(filePath: String): Boolean {
        val formattedPath = filePath.replace("\\", "/")
        val fileName = File(filePath).name
        // 1.需要将生成类排除掉，防止被Transform处理产生问题
        javaGenClassPaths.forEach {
            val genClzPath = RandomCodeObfuscator.convertToPathFormat("${it.pkgName}.${it.className}")
            if (formattedPath.contains(genClzPath)) {
                DLogger.error("checkExcludes, found gen path, ignore processing: $genClzPath >> $formattedPath")
                return true
            }
        }
        // 2.是否在处理的包名清单中(前提processingPackages不为空)
        if (!isTrackInPackage(formattedPath)) {
            DLogger.error("checkExcludes, not track in package: $processingPackages, curPath: $filePath")
            return true
        }
        // 3.根据白名单规则过滤类文件
        val excludeList = getExcludeRules()
        excludeList.forEach {
            val regex = Regex(it)
            if (regex.matches(fileName)) {
                DLogger.error("checkExcludes, exclude file matches: $formattedPath")
                return true
            }
        }
        return false
    }

    private fun isTrackInPackage(clzPath: String): Boolean {
        val packageList = processingPackages
        var isTrackPackage = false
        packageList.forEach {
            val pkgName = it.replace(".", "/")
            if (clzPath.contains(pkgName)) {
                isTrackPackage = true
            }
        }
        return packageList.isEmpty() || isTrackPackage
    }

    fun readConfigs() {
        DLogger.error(" AppCodeGuardConfig read config: $mMap")
    }

    data class GenClassData(
        val pkgName: String,
        val className: String,
        val classPath: String
    )
}