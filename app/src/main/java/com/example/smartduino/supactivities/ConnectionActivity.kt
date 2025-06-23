package com.example.smartduino.supactivities

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartduino.MainActivity
import com.example.smartduino.R
import com.example.smartduino.entities.Device
import com.example.smartduino.ObjectBox
import com.example.smartduino.ObjectBox.store
import com.example.smartduino.bottomdialog.DeviceFragment
import com.example.smartduino.databinding.ActivityConnectionBinding
import com.google.android.material.button.MaterialButton
import io.objectbox.kotlin.boxFor
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

class ConnectionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DEVICE_TYPE = "extra_device_type"
        const val DEVICE_TYPE_HUB = "HUB"
        const val DEVICE_TYPE_NODE = "NODE"
        const val DEVICE_TYPE_THERMOSTAT = "THERMOSTAT"
        const val DEVICE_TYPE_RELAY = "RELAY"
        const val DEVICE_TYPE_CURTAIN = "CURTAIN"
        const val PREF_TEST_CONNECTION = "test_connection"

        // BLE константы
        private const val BLE_DEVICE_NAME = "ESP32_Hub"
        private val WIFI_SERVICE_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
        private val SSID_CHAR_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac")
        private val PASS_CHAR_UUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad")

        // SSID сервисов
        private const val SERVICE_ID_THERMOSTAT = "000002"
        private const val SERVICE_ID_RELAY = "000003"
        private const val SERVICE_ID_CURTAIN = "000004"
    }

    private lateinit var binding: ActivityConnectionBinding
    private lateinit var waveAnimationHelper: WaveAnimationHelper
    private lateinit var centerButton: Button
    private lateinit var deviceType: String
    private lateinit var sharedPref: SharedPreferences

    // BLE компоненты
    private var bleScanner: BluetoothLeScanner? = null
    private var bluetoothGatt: BluetoothGatt? = null
    private var scanCallback: ScanCallback? = null
    private var gattCallback: BluetoothGattCallback? = null

    // HTTP клиент
    private val okHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE) ?: DEVICE_TYPE_NODE
        sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setupUI()
        setupBluetooth()
    }

    private fun setupUI() {
        setupToolbar()
        setupWaveAnimation()
        setupClickListeners()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = when (deviceType) {
                DEVICE_TYPE_HUB -> "Подключение хаба"
                else -> "Подключение устройства"
            }
            setDisplayHomeAsUpEnabled(true)
        }
        binding.toolbar.setNavigationOnClickListener { finish() }
    }

    private fun setupWaveAnimation() {
        waveAnimationHelper = WaveAnimationHelper(
            container = binding.waveContainer,
            waveDrawableRes = R.drawable.wave_circle,
        )
    }

    private fun setupClickListeners() {
        binding.centerButton.setOnClickListener {
            if (waveAnimationHelper.isAnimating) {
                stopDiscovery()
            } else {
                startDiscovery()
            }
        }
    }

    private fun setupBluetooth() {
        if (deviceType == DEVICE_TYPE_HUB) {
            val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            bleScanner = bluetoothManager.adapter.bluetoothLeScanner

            gattCallback = object : BluetoothGattCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
                override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        gatt.discoverServices()
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        showError("Соединение потеряно")
                    }
                }

                override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                    if (status == BluetoothGatt.GATT_SUCCESS) {
                        showWifiConfigDialog(gatt)
                    }
                }
            }

            scanCallback = object : ScanCallback() {
                @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    if (result.device?.name == BLE_DEVICE_NAME) {
                        bleScanner?.stopScan(this)
                        result.device.connectGatt(this@ConnectionActivity, false, gattCallback!!)
                    }
                }
            }
        }
    }

    private fun startDiscovery() {
        waveAnimationHelper.startWaveAnimation()
        binding.statusText.text = "Поиск устройств..."
        binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.primary))

        if (isTestModeEnabled()) {
            createTestDevice()
            return
        }

        when (deviceType) {
            DEVICE_TYPE_HUB -> startBleScan()
            else -> sendHttpDiscoverRequest()
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun stopDiscovery() {
        waveAnimationHelper.stopWaveAnimation()
        binding.statusText.text = "Готов к подключению"
        binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.on_primary_container))

        bleScanner?.stopScan(scanCallback)
        bluetoothGatt?.disconnect()
    }

    // Регион: BLE подключение хаба
    private fun startBleScan() {
        if (!checkBluetoothPermissions()) {
            requestBluetoothPermissions()
            return
        }

        try {
            val filter = ScanFilter.Builder()
                .setDeviceName(BLE_DEVICE_NAME)
                .build()

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            scanCallback?.let { bleScanner?.startScan(listOf(filter), settings, it) }

            Handler(Looper.getMainLooper()).postDelayed({
                bleScanner?.stopScan(scanCallback)
                showError("Хаб не найден")
            }, 15000)

        } catch (e: SecurityException) {
            showError("Нет разрешений Bluetooth")
        }
    }

    private fun showWifiConfigDialog(gatt: BluetoothGatt) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_wifi_input, null)
        val etSsid = dialogView.findViewById<EditText>(R.id.ssidInput)
        val etPassword = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Настройка Wi-Fi")
            .setView(dialogView)
            .setPositiveButton("Подключить") { _, _ ->
                sendWifiCredentials(gatt, etSsid.text.toString(), etPassword.text.toString())
            }
            .setNegativeButton("Отмена") { _, _ -> stopDiscovery() }
            .setOnDismissListener { stopDiscovery() }
            .show()
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendWifiCredentials(gatt: BluetoothGatt, ssid: String, password: String) {
        try {
            val service = gatt.getService(WIFI_SERVICE_UUID) ?: throw Exception("Service not found")
            val ssidChar = service.getCharacteristic(SSID_CHAR_UUID) ?: throw Exception("SSID char not found")
            val passChar = service.getCharacteristic(PASS_CHAR_UUID) ?: throw Exception("Password char not found")

            ssidChar.value = ssid.toByteArray()
            passChar.value = password.toByteArray()

            gatt.writeCharacteristic(ssidChar)
            gatt.writeCharacteristic(passChar)

            createDevice("Smart Hub", DEVICE_TYPE_HUB)
            showSuccess("Хаб успешно подключен!")

        } catch (e: Exception) {
            showError("Ошибка настройки: ${e.message}")
        }
    }
    // Конец региона BLE

    // Регион: HTTP подключение устройств
    private fun sendHttpDiscoverRequest() {
        val request = Request.Builder()
            .url("http://192.168.1.49/discover")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showError("Ошибка сети: ${e.message}")
                    stopDiscovery()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        showError("Ошибка сервера: ${response.code}")
                        stopDiscovery()
                    }
                    return
                }

                try {
                    val responseData = response.body?.string() ?: ""
                    parseAndCreateDevices(responseData)
                } catch (e: Exception) {
                    runOnUiThread {
                        showError("Ошибка данных: ${e.message}")
                        stopDiscovery()
                    }
                }
            }
        })
    }

    private fun parseAndCreateDevices(response: String) {
        val devices = mutableListOf<DiscoveredDevice>()
        var currentDevice: DiscoveredDevice? = null

        response.split("\n").forEach { line ->
            when {
                line.contains("Found devices:") -> return@forEach
                line.matches(Regex("\\d+\\. .+")) -> {
                    currentDevice?.let { devices.add(it) }
                    currentDevice = DiscoveredDevice(name = line.substringAfter('.').trim())
                }
                line.contains("Services:") -> {
                    currentDevice?.services = line.substringAfter("Services:").trim()
                }
            }
        }
        currentDevice?.let { devices.add(it) }

        runOnUiThread {
            if (devices.isEmpty()) {
                showError("Устройства не найдены")
                return@runOnUiThread
            }

            devices.forEach { device ->
                val type = when {
                    device.services?.contains(SERVICE_ID_THERMOSTAT) == true -> DEVICE_TYPE_THERMOSTAT
                    device.services?.contains(SERVICE_ID_RELAY) == true -> DEVICE_TYPE_RELAY
                    device.services?.contains(SERVICE_ID_CURTAIN) == true -> DEVICE_TYPE_CURTAIN
                    else -> DEVICE_TYPE_NODE
                }
                createDevice(device.name, type)
            }

            showSuccess("Найдено ${devices.size} устройств")
            stopDiscovery()
        }
    }
    // Конец региона HTTP

    private fun createDevice(name: String, type: String) {
        val device = Device(
            name = name,
            type = type,
        )

        store.boxFor(Device::class.java).put(device)
        Log.i("Device", "Created: ${device.name} (${device.type})")
    }

    private fun createTestDevice() {
        val testType = if (deviceType == DEVICE_TYPE_HUB) DEVICE_TYPE_HUB else DEVICE_TYPE_NODE
        createDevice("Тестовое устройство", testType)
        showSuccess("Тестовое устройство создано")
        stopDiscovery()
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            binding.statusText.text = message
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.error))
        }
    }

    private fun showSuccess(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            binding.statusText.text = message
            binding.statusText.setTextColor(ContextCompat.getColor(this, R.color.primary))
        }
    }

    private fun isTestModeEnabled() = sharedPref.getBoolean(PREF_TEST_CONNECTION, false)

    private fun checkBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            101
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        stopDiscovery()
        okHttpClient.dispatcher.cancelAll()
        bluetoothGatt?.close()
    }

    data class DiscoveredDevice(
        val name: String,
        var services: String? = null
    )
}