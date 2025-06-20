package com.example.smartduino

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.adapters.ViewPagerAdapter
import com.example.smartduino.bottomdialog.AddMenuFragment
import com.example.smartduino.bottomdialog.DeviceFragment
import com.example.smartduino.entities.Device
import com.example.smartduino.entities.Room
import com.example.smartduino.interfaces.OnDeviceChangeListener
import com.example.smartduino.interfaces.OnRoomAddedListener
import com.example.smartduino.supactivities.SettingsActivity
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import io.objectbox.kotlin.boxFor


class MainActivity : AppCompatActivity(), OnRoomAddedListener, OnDeviceChangeListener {
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setupEdgeToEdge()
        setTheme(R.style.Theme_SmartDuino_Dark);
        // Инициализация ObjectBox
        ObjectBox.init(this)

        val devices = store.boxFor<Device>().all
        devices.forEach { device ->
            Log.d("DEVICE_INFO", """
        ID: ${device.id}
        Name: ${device.name}
        Type: ${device.type}
        Room: ${device.room.target?.name ?: "No room"}
        Parameters count: ${device.parameters.size}
    """.trimIndent())
        }

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
        findViewById<View>(R.id.button_settings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun showAddRoomDialog() {
        AddMenuFragment().apply {
            show(supportFragmentManager, "BottomSheetDialogSheet")
        }
    }

    private fun setupViewPagerAndTabs() {
        val rooms = store.boxFor<Room>().all
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)

        // Создаем список фрагментов: сначала "Все", затем комнаты
        val fragments = mutableListOf<Fragment>().apply {
            add(AllDevicesFragment.newInstance()) // Фрагмент "Все"
            addAll(rooms.map { RoomFragment.newInstance(it.id) })
        }

        viewPager.adapter = ViewPagerAdapter(
            this,
            fragments
        )

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Все" // Первая вкладка
                else -> rooms[position - 1].name // Остальные комнаты (смещение на -1)
            }
        }.attach()

        // Обработка долгого нажатия (только для комнат, не для вкладки "Все")
        for (i in 1 until tabLayout.tabCount) {
            tabLayout.getTabAt(i)?.view?.setOnLongClickListener {
                showDeleteRoomDialog(rooms[i - 1].id) // Смещение на -1
                true
            }
        }
    }

    private fun showDeleteRoomDialog(roomId: Long) {
        AlertDialog.Builder(this)
            .setTitle("Удалить комнату?")
            .setMessage("Все устройства в этой комнате будут перемещены в «Без комнаты».")
            .setPositiveButton("Удалить") { _, _ ->
                deleteRoom(roomId)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun deleteRoom(roomId: Long) {
        val roomBox = store.boxFor<Room>()
        val room = roomBox.get(roomId) ?: return

        store.runInTx {
            // Отвязываем устройства
            room.devices.forEach { it.room.target = null }
            roomBox.remove(roomId)
        }

        setupViewPagerAndTabs()
    }

    override fun onRoomAdded() {
        // Обновляем ViewPager при добавлении новой комнаты
        setupViewPagerAndTabs()
    }
    override fun onDeviceChanged() {
        Toast.makeText(this, "Device changed", Toast.LENGTH_SHORT).show()
        setupViewPagerAndTabs()
    }

    override fun onDestroy() {
        super.onDestroy()
        // При необходимости закрыть BoxStore
        store.close()
    }


}