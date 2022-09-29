package com.tosmo.android_filepicker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tosmo.afilepicker.FilePicker

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        FilePicker.STORAGE_PATH
    }
}