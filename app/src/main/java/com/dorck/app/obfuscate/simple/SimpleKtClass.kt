package com.dorck.app.obfuscate.simple

import android.util.Log

@SimpleTestAnnotation
class SimpleKtClass {

    fun testMethod() {
        Log.i("SimpleKtClass", "testMethod")
    }

    fun testMethod2() {
        Log.i("SimpleKtClass", "testMethod2")
    }
}