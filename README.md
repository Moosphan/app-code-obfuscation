# app-code-obfuscation
Android plug-in code obfuscation tool, based on ASM, implants meaningless bytecode during compilation.
> Android插入式代码混淆工具，基于ASM在编译期间植入无意义字节码，以提高逆向成本。

### 特性
- 在Proguard优化class字节码之后执行，从而避免被混淆工具优化
- 支持插入类的成员变量、静态变量、方法及方法体内的代码段等
- 支持自定义随机代码字典，增强定制性
- 支持插入作用域控制（类级别、包级别和所有类）
- 支持 AGP 8.0 及其以下版本的功能适配
- 支持通用ASM插件的基础库下沉，降低后续插件开发成本

### 快速使用
// TODO

### 当前进展
// TODO：放到 `GitHub >> Projects` 中管理
- [X] 实现默认内置的类混淆基本功能
- [X] 实现方法内的随机代码混淆
- [ ] 提供更加灵活的配置项（类、方法、代码块等混淆配置）
- [ ] 混淆范围细化到函数级别
- [ ] 多线程并行执行，优化混淆速度

### 配置项

| 可配置项             | 说明                                       | 类型           |
| -------------------- |------------------------------------------| -------------- |
| `maxMethodCount`     | 类中允许插入方法的数量上限                            | `int`          |
| `maxFieldCount`      | 类中允许插入变量的数量上限                            | `int`          |
| `isAutoAdapted`      | 是否根据当前类或方法的具体情况自动适配插入的方法或变量的数量           | `boolean`      |
| `processingPackages` | 需要混淆处理的包路径（若未设置，则默认所有路径）                 | `List<String>` |
| `isSkipJars`         | 是否跳过第三方 jar 的混淆增强（默认为 `true`）            | `boolean`      |
| `isSkipAbsClass`     | 是否跳过抽象类的混淆增强（默认为 `true`）                 | `boolean`      |
| `obfuscationDict`    | 自定义的混淆代码字典文件，可自行配置插入的代码和离散程度（格式参照下方详细介绍） | `String`       |

### 混淆字典格式说明

如果你希望使用自定义的混淆规则，那么可尝试创建混淆字典文件，格式如下：

```json
{
  "fields": [
    "a#public#Ljava/lang/String;",
    "b#private#Ljava/lang/String;",
    "c#protected#Ljava/lang/String;",
    "d#private#Ljava/lang/String;",
    "e#public#Ljava/lang/String;",
    "f#protected#Ljava/lang/String;",
    "g#public#Ljava/lang/String;",
    "h#private#Ljava/lang/String;",
    "i#protected#Ljava/lang/String;",
    "j#private#Ljava/lang/String;",
    "k#private#Ljava/lang/String;",
    "l#private#Ljava/lang/String;"
  ],
  "methods": [
    "com/example/util/LogUtil#v#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#v#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
    "com/example/util/LogUtil#d#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#d#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
    "com/example/util/LogUtil#i#(Ljava/lang/String;Ljava/lang/String;)V",
    "com/example/util/LogUtil#i#(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V"
  ],
  "codeBlock": [
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#x#()V",
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#y#(I)V",
    "com/dorck/app/obfuscate/obfuscate/FunctionInnerObfuscatorSample#z#(J)V"
  ],
  "whitelist": [
    "com/example/SampleActivity"
  ]
}
```

下面对内部配置和格式进行解释：

- **fields**：即插入到类中的属性信息，由 `#` 分隔，从左到右依次代表属性名、属性访问修饰符类型、属性的类型（目前仅支持基本类型和 `String` 类型）
- **methods**：即插入到类中的方法信息，由 `#` 分隔，从左到右依次代表方法名、方法描述符（目前仅支持无值返回，即 `void` 类型）
- **codeBlock**：针对于方法级别，即在方法中随机插入若干外部指定类的静态方法调用。主要注意以下规则：
  - 这个类可以自己在项目中自由创建，为防止开启proguard代码混淆后被优化，需要在 `proguard-rules.pro` 中 **keep** 住该类；
  - 类中仅可以包含 **`public static void`** 修饰的方法，不能有返回值；
  - 该类中方法参数只能是无参或者单个参数，且参数类型仅支持基本数据类型和 `String` 类型；
  - 插件执行方法内代码混淆时的随机性取决于该类中定义方法的随机性（如你可以定义足够多的无意义方法）
- **whiteList**：白名单配置，可以确保白名单内的类不会被插入混淆代码

基于以上规则，你就可以自由插入自己的定制代码了。如果你没有在 `app/build.gradle` 中设置 `obfuscationDict`，则默认由插件来自动帮你生成随机代码。

当然，目前该配置灵活度不是那么高，后续会慢慢优化，如果您能够提供一些好的建议。