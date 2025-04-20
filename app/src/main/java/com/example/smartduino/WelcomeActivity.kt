package com.example.smartduino

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class WelcomeActivity : AppCompatActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация SharedPreferences
        sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Проверяем, есть ли сохраненное имя
        val userName = sharedPreferences.getString("user_name", null)

        if (userName != null) {
            // Если имя уже есть, сразу переходим в MainActivity2
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Закрываем WelcomeActivity
            return
        }

        // Если имени нет, показываем экран ввода
        setContentView(R.layout.activity_welcome)

        val editTextName = findViewById<EditText>(R.id.nameEditText)
        val buttonContinue = findViewById<Button>(R.id.continueButton)

        buttonContinue.setOnClickListener {
            val name = editTextName.text.toString().trim()
            if (name.isNotEmpty()) {
                saveName(name) // Сохраняем имя
                startActivity(Intent(this, MainActivity::class.java))
                finish() // Закрываем WelcomeActivity
            } else {
                Toast.makeText(this, "Введите имя", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveName(name: String) {
        // Сохраняем имя в SharedPreferences
        with(sharedPreferences.edit()) {
            putString("user_name", name)
            apply() // или commit() для синхронного сохранения
        }
    }
}