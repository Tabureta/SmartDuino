package com.example.smartduino

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.objectbox.BoxStore

lateinit var boxStore: BoxStore

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        // Получаем SharedPreferences
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)

        // Получаем сохранённое имя (если нет - пустая строка или значение по умолчанию)
        val userName = sharedPreferences.getString("user_name", "")

        // Находим TextView и устанавливаем текст
        val textViewGreeting = findViewById<TextView>(R.id.textWelcome) // Замените на ваш ID
        textViewGreeting.text = "Привет, $userName!" // Или любой другой формат

        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar) // Устанавливаем Toolbar как ActionBar

        val buttonAdd = findViewById<View>(R.id.button_add)
        buttonAdd.setOnClickListener {
            // Логика для добавления новой комнаты
            val bottomSheet = BottomSheetDialog()
            bottomSheet.show(supportFragmentManager, "ModalBottomSheet")
        }

        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        val fragments = listOf(RoomFragment())

        val adapter = ViewPagerAdapter(this, fragments)
        viewPager.adapter = adapter

        // Связываем TabLayout с ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = "Tab ${position + 1}" // Устанавливаем текст для вкладки
        }.attach()
    }

    override fun onDestroy() {
        super.onDestroy()
        boxStore.close() // Закрытие boxStore при уничтожении активности
    }
}