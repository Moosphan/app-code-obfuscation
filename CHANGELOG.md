# Changelog

本项目所有重要的版本变更都会记录在此文件中。

格式基于 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.0.0/)，版本号遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)。

## [0.3.1-beta] - 2026-09-01

### 修复

- **修复 AGP 7 构建时生成类未打包问题（issue [#19](https://github.com/Moosphan/app-code-obfuscation/issues/19)）**
  - 抽出 `GeneratedSourceDirectory` 统一管理生成类目录（`generated/codeguard/java`），确保 AGP 7（Transform API）构建时生成的垃圾代码类能正确参与编译并打包进 APK。

## [0.3.0-beta] - 2026-08-18

### 修复

- **修复 AGP 7.x 集成崩溃（issue [#16](https://github.com/Moosphan/app-code-obfuscation/issues/16)）**
  - `Agp8Compat.isAgp8OrHigher()` 原先通过检测 `AsmClassVisitorFactory` 类是否存在来判断 AGP 8+，但该类自 AGP 7.0 起就已存在，导致 AGP 7.2.2 等 7.x 版本被误判为 AGP 8+，进入 Instrumentation API 分支并调用 `AndroidComponentsExtension.onVariants` 的 Kotlin 默认参数合成方法 `onVariants$default`，而该方法在 AGP 7.x 上不存在，最终抛出 `NoSuchMethodError`。
  - 现在改为直接读取 `com.android.Version.ANDROID_GRADLE_PLUGIN_VERSION`，按 AGP 主版本号（>= 8）进行判断，AGP 7.x 会正确回退到 Transform API 分支。

- **修复 AGP 8.7.0 / Gradle 8.9 集成报错（issue [#17](https://github.com/Moosphan/app-code-obfuscation/issues/17)）**
  - 配置扩展 `CodeGuardConfigExtension` 的属性名与文档 / `toString()` 输出不一致，导致按文档配置时报 `Could not set unknown property`。已补充以下向后兼容别名，同时修正 `toString()` 输出真实属性名：
    - `generatedMethodCount`（别名）↔ `generatedClassMethodCount`（真实属性）
    - `isAutoAdapted`（别名）↔ `isInsertCountAutoAdapted`（真实属性）
    - `isSkipJarFilesProcessing` / `isSkipJars`（别名）↔ `isSkipJar`（真实属性）
    - `mapperFile`（别名）↔ `mapper`（真实属性）
    - `obfuscationDictionary`（别名）↔ `obfuscationDict`（真实属性）
  - 修复 AGP 8 分支运行时 `StackOverflowError`：`CodeGuardClassVisitorFactory.isInstrumentable()` 未排除插件自身生成的垃圾代码类，导致这些生成类被二次插桩、方法互相调用形成无限递归。新增 `AppCodeGuardConfig.isGeneratedClass()` 并在 `isInstrumentable()` 中跳过生成类。
  - 修复 AGP 8 分支构建后生成的垃圾代码不自动清理的问题：为 `transform<Variant>ClassesWithAsm` 任务注册 `doLast` 清理逻辑，并修复 `AppCodeGuardConfig` 中两处路径 Bug：
    - `getDeleteDir()` 拼接目录时缺少路径分隔符（产生 `.../codeguard/javacom/...` 的错误路径）；
    - `batchDeleteGenClass()` 依赖 `src/main/java` 路径反推 key，而生成类已迁移到 `build/generated` 目录，导致 key 为 null、删除失败。现在直接使用记录的 `pkgName.className` 作为 key。

### 其他

- 补充单元测试：新增 14 个测试用例，覆盖属性别名映射、生成类识别、AGP 版本判断等场景，测试总数由 61 提升至 75，全部通过。

## [0.2.0-beta] - 2024/05/13

### 新增

- 支持 AGP 8.0+（`AsmClassVisitorFactory` / Instrumentation API）
- AGP 7.x / 8.0+ 双版本自动检测和适配
- 完整的单元测试覆盖（61 个测试用例）
