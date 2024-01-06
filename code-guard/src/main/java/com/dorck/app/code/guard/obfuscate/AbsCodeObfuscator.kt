package com.dorck.app.code.guard.obfuscate

import java.util.Random

abstract class AbsCodeObfuscator : IAppCodeObfuscator {
    protected val random = Random()
    protected abstract var mClassEntity: SimpleClassEntity?
    protected abstract var mGenClassList: MutableList<SimpleClassEntity>

    /**
     * 通过随机布尔值决定是否需要插入
     */
    override fun shouldInsertElement(): Boolean {
        return /*random.nextBoolean()*/ true
    }

    fun getCurClassEntity(): SimpleClassEntity? = mClassEntity

    fun getGenClassEntityList(): List<SimpleClassEntity> = mGenClassList
}