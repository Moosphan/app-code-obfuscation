# app-code-obfuscation
Android plug-in code obfuscation tool, based on ASM, implants meaningless code during compilation.
> Android插入式代码混淆工具，基于ASM在编译期间植入无意义代码，以提高逆向成本。

### Features
- 支持插入类的成员变量、静态变量、方法及方法体内的代码段等
- 支持灵活配置，支持自定义随机字典，作用域等
- 支持 AGP 8.0 及其以下版本的功能适配
- 支持通用ASM插件的基础库下沉，降低后续插件开发成本
