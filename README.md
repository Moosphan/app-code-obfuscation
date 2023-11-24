# app-code-obfuscation
Android plug-in code obfuscation tool, based on ASM, implants meaningless bytecode during compilation.
> Android插入式代码混淆工具，基于ASM在编译期间植入无意义字节码，以提高逆向成本。

### Features
- 在Proguard优化class字节码之后执行，从而避免被混淆工具优化
- 支持插入类的成员变量、静态变量、方法及方法体内的代码段等
- 支持自定义随机代码字典，增强定制性
- 支持插入作用域控制（类级别、包级别和所有类）
- 支持 AGP 8.0 及其以下版本的功能适配
- 支持通用ASM插件的基础库下沉，降低后续插件开发成本

### Todo List
