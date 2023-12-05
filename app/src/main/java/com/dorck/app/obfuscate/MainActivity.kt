package com.dorck.app.obfuscate

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.dorck.app.obfuscate.a.b.e.TestCall

class MainActivity : AppCompatActivity() {
    private var name: String = "sa"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)
        Log.i("MainActivity", "onCreate: $name")
    }

    private fun get(int: Int) {
        TestCall.make(0)
    }
}