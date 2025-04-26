package com.example.smartduino

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.viewpager2.widget.ViewPager2
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.entities.Room
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.objectbox.kotlin.boxFor


class MainActivity : AppCompatActivity(), AddRoomFragment.OnRoomAddedListener {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupEdgeToEdge()


        // Инициализация ObjectBox
        ObjectBox.init(this)

        setupUI()
        setupViewPagerAndTabs()
    }

    private fun setupEdgeToEdge() {
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    private fun setupUI() {
        // Приветствие пользователя
        val sharedPreferences = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        findViewById<TextView>(R.id.textWelcome).text =
            "Привет, ${sharedPreferences.getString("user_name", "")}!"

        // Настройка Toolbar
        setSupportActionBar(findViewById(R.id.toolbar))

        // Кнопка добавления комнаты
        findViewById<View>(R.id.button_add).setOnClickListener {
            showAddRoomDialog()
        }
    }

    private fun showAddRoomDialog() {
        BottomSheetDialog().apply {
            show(supportFragmentManager, "BottomSheetDialogSheet")
        }
    }

    private fun setupViewPagerAndTabs() {
        val rooms = store.boxFor<Room>().all
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        viewPager.adapter = ViewPagerAdapter(
            this,
            rooms.map { RoomFragment.newInstance(it.id) }
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = rooms[position].name
        }.attach()
    }

    override fun onRoomAdded() {
        // Обновляем ViewPager при добавлении новой комнаты
        setupViewPagerAndTabs()
    }

    override fun onDestroy() {
        super.onDestroy()
        // При необходимости закрыть BoxStore
        // store.close()
    }
}