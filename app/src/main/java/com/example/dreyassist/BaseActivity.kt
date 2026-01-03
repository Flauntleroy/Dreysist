package com.example.dreyassist

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.dreyassist.util.LocaleManager

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.setLocale(newBase))
    }
}
