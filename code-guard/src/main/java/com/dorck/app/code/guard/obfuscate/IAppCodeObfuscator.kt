package com.dorck.app.code.guard.obfuscate

interface IAppCodeObfuscator {
    fun nextFiled(): FieldEntity

    fun nextMethod(): MethodEntity

    fun shouldInsertElement(): Boolean
}