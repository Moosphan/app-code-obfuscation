package com.dorck.app.code.guard.obfuscate

interface IAppCodeObfuscator {
    fun nextFiled(): FieldEntity

    fun nextMethod(): MethodEntity

    // 以函数调用方式来插入代码
    fun nextCodeCall(): MethodEntity

    fun shouldInsertElement(): Boolean
}