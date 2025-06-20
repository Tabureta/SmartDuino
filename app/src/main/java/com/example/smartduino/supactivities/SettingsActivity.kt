package com.example.smartduino.supactivities

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.smartduino.R
import com.example.smartduino.databinding.ActivitySettingsBinding
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация binding
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка EdgeToEdge
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Инициализация SharedPreferences
        sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        // Настройка Toolbar
        setupToolbar()

        // Загрузка текущих настроек
        loadSettings()

        // Обработчики Switch
        setupSwitches()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish() // Закрываем активность при нажатии на кнопку назад
        }
    }

    private fun loadSettings() {
        // Загружаем сохранённую тему (по умолчанию - светлая)
        val isDarkTheme = sharedPref.getBoolean(PREF_DARK_THEME, false)
        binding.darkThemeSwitch.isChecked = isDarkTheme

        // Загружаем другие настройки
        binding.notificationsSwitch.isChecked = sharedPref.getBoolean(PREF_NOTIFICATIONS, true)
        binding.soundSwitch.isChecked = sharedPref.getBoolean(PREF_SOUND, true)
        binding.testConnectionSwitch.isChecked = sharedPref.getBoolean(PREF_TEST_CONNECTION, true)
    }

    private fun setupSwitches() {
        // Обработчик переключения темы
        binding.darkThemeSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(PREF_DARK_THEME, isChecked).apply()
            applyTheme(isChecked)
        }

        // Обработчики других Switch
        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(PREF_NOTIFICATIONS, isChecked).apply()
        }

        binding.soundSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(PREF_SOUND, isChecked).apply()
        }

        binding.testConnectionSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPref.edit().putBoolean(PREF_TEST_CONNECTION, isChecked).apply()
        }
    }

    private fun applyTheme(isDarkTheme: Boolean) {
        val mode = if (isDarkTheme) {
            AppCompatDelegate.MODE_NIGHT_YES
            Log.d("Theme","Темная тема")
        } else {
            AppCompatDelegate.MODE_NIGHT_NO
            Log.d("Theme","Светлая тема")
        }
        AppCompatDelegate.setDefaultNightMode(mode)

    }

    companion object {
        private const val PREF_DARK_THEME = "dark_theme"
        private const val PREF_NOTIFICATIONS = "notifications"
        private const val PREF_SOUND = "sound"
        private const val PREF_TEST_CONNECTION = "test_connection"
    }
}