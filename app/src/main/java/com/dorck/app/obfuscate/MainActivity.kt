package com.dorck.app.obfuscate

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dorck.app.obfuscate.simple.SimpleKtClass

class MainActivity : AppCompatActivity() {
    private var name: String = "sa"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        Log.i("MainActivity", "onCreate: $name")
        get(8)
    }

    private fun get(int: Int) {
        SimpleKtClass().testMethod()
    }
}