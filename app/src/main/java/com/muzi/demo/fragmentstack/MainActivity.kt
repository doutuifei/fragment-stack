package com.muzi.demo.fragmentstack

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.muzi.fragmentstack.DebugStackDelegate

class MainActivity : AppCompatActivity() {

    private val fragmentStack by lazy { DebugStackDelegate(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        fragmentStack.onPostCreate(null)
    }
}