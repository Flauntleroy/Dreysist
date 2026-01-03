package com.example.dreyassist

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.example.dreyassist.databinding.ActivityAboutBinding
import com.example.dreyassist.util.LocaleManager

class AboutActivity : BaseActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        updateLanguageButtonState()

        binding.btnLangId.setOnClickListener {
            changeLanguage("in")
        }

        binding.btnLangEn.setOnClickListener {
            changeLanguage("en")
        }
    }

    private fun changeLanguage(langCode: String) {
        if (LocaleManager.getLanguage(this) == langCode) return
        
        LocaleManager.setNewLocale(this, langCode)
        
        // Restart activity to apply changes
        val intent = intent
        finish()
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    private fun updateLanguageButtonState() {
        val currentLang = LocaleManager.getLanguage(this)
        if (currentLang == "en") {
            binding.btnLangEn.alpha = 1.0f
            binding.btnLangId.alpha = 0.5f
            binding.btnLangEn.strokeWidth = 2
            binding.btnLangId.strokeWidth = 0
        } else {
            binding.btnLangId.alpha = 1.0f
            binding.btnLangEn.alpha = 0.5f
            binding.btnLangId.strokeWidth = 2
            binding.btnLangEn.strokeWidth = 0
        }
    }
}
