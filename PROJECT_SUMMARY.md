# app-code-obfuscation 项目总结文档

## 一、项目概览

| 属性 | 说明 |
|------|------|
| **项目名称** | app-code-obfuscation (CodeGuardPlugin) |
| **作者** | Moosphan / Dorck (moosphon@gmail.com) |
| **许可证** | Apache 2.0 |
| **当前版本** | 0.1.4-beta (不稳定版) |
| **Gradle 插件 ID** | `cn.dorck.code.guarder` |
| **Maven 坐标** | `cn.dorck:code-guard-plugin` |

## 二、项目定位

这是一个 **Android Gradle 插件**，用于在**编译期**对代码进行混淆保护。与 ProGuard/R8 的代码压缩优化不同，本插件的核心思路是：

> 在编译后的 `.class` 文件中**插入无意义的噪声字节码**（随机字段、随机方法、随机方法调用），从而显著增加逆向工程的难度。

插件基于 **ASM 字节码操作框架**（7.0 版本），通过 Android Transform API 在编译流程中拦截并修改 class 文件。

## 三、项目结构

```
app-code-obfuscation/
├── README.md                  # 中文文档
├── README_en.md               # 英文文档
├── advance_readme.md          # 自定义混淆字典格式规范
├── build.gradle.kts           # 根构建文件
├── settings.gradle.kts        # 模块配置
├── art/                       # README 截图资源
├── app/                       # 示例 Android 应用模块
│   ├── build.gradle.kts       # 插件使用示例配置
│   ├── code_obfuscate_config.json  # 自定义字典配置示例
│   └── src/main/              # 示例源码
├── code-guard/                # Gradle 插件核心模块
│   ├── build.gradle.kts       # 插件构建 + Maven/Gradle 发布配置
│   └── src/main/java/com/dorck/app/code/guard/
│       ├── CodeGuardPlugin.kt           # 插件入口
│       ├── config/                      # 全局配置单例
│       ├── extension/                   # DSL 扩展配置类
│       ├── obfuscate/                   # 混淆策略层
│       ├── transform/                   # Android Transform 集成
│       ├── visitor/                     # ASM 字节码访问器
│       ├── task/                        # Gradle 任务（代码生成）
│       └── utils/                       # 工具类
└── librarysample/             # 示例 Android 库模块
```

## 四、技术栈

| 技术 | 版本 |
|------|------|
| Kotlin | 1.5.21 |
| Android Gradle Plugin (AGP) | 7.2.x |
| ASM 字节码框架 | 7.0 (ASM5) |
| 构建工具 | Gradle Kotlin DSL |
| 发布目标 | Sonatype OSSRH + Gradle Plugin Portal |

## 五、核心架构

### 5.1 模块职责划分

```
┌─────────────────────────────────────────────────────────┐
│                    CodeGuardPlugin                       │
│                     (插件入口)                            │
├─────────────────────────────────────────────────────────┤
│  Extension        │  Config          │  Transform        │
│  (DSL 配置)       │  (全局配置单例)    │  (字节码拦截)      │
├─────────────────────────────────────────────────────────┤
│              Obfuscation Strategy Layer                  │
│  ┌──────────────────┐  ┌──────────────────┐            │
│  │ RandomCodeObfusc. │  │ CustomCodeObfusc.│            │
│  │ (随机混淆策略)     │  │ (自定义字典策略)  │            │
│  └──────────────────┘  └──────────────────┘            │
├─────────────────────────────────────────────────────────┤
│  ASM Visitors       │  Code Generation    │  Utils       │
│  (字节码注入)        │  (辅助类生成)       │  (工具集)     │
└─────────────────────────────────────────────────────────┘
```

### 5.2 关键组件说明

#### (1) 插件入口 — `CodeGuardPlugin.kt`
- 实现 `Plugin<Project>` 接口
- 注册 DSL 扩展 (`codeGuard { ... }`)
- 注册 `CodeGuardTransform`
- 注册 `GenRandomClassTask`（在 preBuild 之前运行）
- 注册 `BuildListener` 处理构建失败时的清理

#### (2) DSL 扩展 — `CodeGuardConfigExtension.kt`
用户在 `build.gradle.kts` 中通过以下方式配置：
```kotlin
codeGuard {
    enable = true
    processingPackages = hashSetOf("com.example.myapp")
    maxMethodCount = 8
    minMethodCount = 2
    maxFieldCount = 10
    methodObfuscateEnable = true
    variantConstraints = hashSetOf("release")
    excludeRules = listOf("com.example.exclude.**")
}
```

#### (3) 混淆策略层
采用**策略模式**，通过工厂类 `CodeObfuscatorFactory` 创建：

| 策略 | 说明 | 状态 |
|------|------|------|
| `RandomCodeObfuscator` | 随机生成字段名、方法名、调用目标 | 已实现 |
| `CustomCodeObfuscator` | 基于用户自定义 JSON 字典 | **TODO（未实现）** |

#### (4) ASM 字节码访问器
- `ObfuscationClassVisitor`：类级别访问器，负责插入随机字段和方法
- `ObfuscationMethodVisitor`：方法级别访问器，负责在方法体中注入 `INVOKESTATIC` 调用

#### (5) Transform 集成
- `BaseTransform`：处理目录输入和 JAR 输入，支持增量构建
- `CodeGuardTransform`：检查插件启用状态和构建变体，过滤目标类

## 六、工作流程

```
  构建开始
     │
     ▼
┌─────────────────────────────────┐
│ 1. GenRandomClassTask 执行       │
│    生成 N 个随机辅助 Java 类      │
│    写入 src/main/java/           │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 2. Java 编译                    │
│    辅助类与业务代码一起编译       │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 3. CodeGuardTransform 执行      │
│    遍历所有 .class 文件          │
│    对每个非排除类进行字节码注入：  │
│    - 插入随机字段 (2-10个)       │
│    - 插入随机方法 (2-8个)        │
│    - 方法体中注入无意义调用       │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│ 4. 清理阶段                     │
│    删除 src/main/java/ 中        │
│    生成的辅助类源文件             │
└──────────────┬──────────────────┘
               │
               ▼
        构建完成
```

## 七、混淆效果

混淆前（反编译结果清晰可见）：
```java
public class SimpleKtClass {
    public void doSomething() {
        System.out.println("Hello");
    }
}
```

混淆后（反编译结果充满噪声）：
```java
public class SimpleKtClass {
    public int vKmL;
    private String vPxN;
    public boolean vQrT;

    public void vAbC() { }
    private void vDef(int a, String b) { }

    public void doSomething() {
        Ab.xyz(42, true);
        System.out.println("Hello");
        Cd.abc("random", 0);
    }
}
```

## 八、可配置参数一览

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `enable` | Boolean | `true` | 插件总开关 |
| `obfuscationDict` | String | `""` | 自定义混淆字典 JSON 路径 |
| `processingPackages` | Set | 空集 | 目标混淆包路径 |
| `isSkipAbsClass` | Boolean | `true` | 是否跳过抽象类 |
| `maxMethodCount` | Int | `8` | 每个类最大插入方法数 |
| `minMethodCount` | Int | `2` | 每个类最小插入方法数 |
| `maxFieldCount` | Int | `10` | 每个类最大插入字段数 |
| `minFieldCount` | Int | `5` | 每个类最小插入字段数 |
| `methodObfuscateEnable` | Boolean | `false` | 是否启用方法体代码注入 |
| `maxCodeLineCount` | Int | `6` | 每个方法最大插入代码行数 |
| `isInsertCountAutoAdapted` | Boolean | `false` | 根据类大小自适应调整插入数量 |
| `genClassCount` | Int | `3` | 生成辅助类数量 |
| `generatedClassMethodCount` | Int | `3` | 每个辅助类的方法数量 |
| `excludeRules` | List | 空列表 | 白名单规则（正则） |
| `variantConstraints` | Set | 空集 | 处理的构建变体（如 "release"） |
| `isSkipJar` | Boolean | `true` | 是否跳过第三方 JAR |

## 九、已知问题

| 问题 | 说明 |
|------|------|
| **多变体 ClassNotFound** | 多个变体同时 `assemble` 时可能出现类找不到的错误 |
| **installRelease 二次崩溃** | 由于 dex 缓存未清除，第二次 `installRelease` 可能崩溃 |
| **Java 关键字冲突** | 生成的随机名称可能与 Java 关键字冲突（已部分修复） |
| **AGP 兼容性** | 仅支持 AGP 7.x，不兼容 AGP 8.0+（Transform API 已移除） |
| **自定义字典未实现** | `CustomCodeObfuscator` 的 `initialize()` 方法仍为 TODO |

## 十、核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| ASM (ow2) | 7.0 | 字节码操作框架 |
| Android Gradle Plugin | 7.2.1 | Transform API 提供者 |
| commons-io | - | 文件 IO 工具 |
| Gson | - | JSON 解析（自定义字典） |

## 十一、总结

**app-code-obfuscation** 是一个轻量级的 Android 代码保护插件，通过在编译期注入无意义的噪声字节码来增加反编译后的阅读难度。其核心设计理念是"**增加攻击者的认知负担**"，而非传统的代码加密或字符串加密方式。

项目当前处于 **beta 阶段**（v0.1.4-beta），主要局限在于：
1. 仅支持 AGP 7.x，需要迁移到新的 Instrumentation API 以兼容 AGP 8.0+
2. 自定义混淆字典功能尚未完成
3. 存在多变体构建时的稳定性问题

适用场景：对代码保护有基本需求、且项目仍使用 AGP 7.x 的 Android 应用。
