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
        const val DEVICE_TYPE_NODE = "node"
        const val PREF_TEST_CONNECTION = "test_connection"
        const val DEVICE_TYPE_THERMOSTAT = "THERMOSTAT"
        const val DEVICE_TYPE_RELAY = "RELAY"
        const val DEVICE_TYPE_CURTAIN = "CURTAIN"
    }
    private lateinit var waveAnimationHelper: WaveAnimationHelper
    private lateinit var deviceType: String
    private lateinit var sharedPref: SharedPreferences

    // BLE компоненты
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    // UUID сервисов и характеристик
    private val wifiServiceUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ab")
    private val ssidUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ac")
    private val passUUID = UUID.fromString("12345678-1234-1234-1234-1234567890ad")

    // HTTP клиент
    private val okHttpClient = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        deviceType = intent.getStringExtra(EXTRA_DEVICE_TYPE) ?: DEVICE_TYPE_NODE
        sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)

        setupToolbar()

        // Инициализация BLE только для хаба и не в тестовом режиме
        if (deviceType == DEVICE_TYPE_HUB && !isTestModeEnabled()) {
            bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
            bleScanner = bluetoothAdapter.bluetoothLeScanner
        }

        waveAnimationHelper = WaveAnimationHelper(
            container = findViewById(R.id.waveContainer),
            waveDrawableRes = R.drawable.wave_circle
        )

        val centerButton = findViewById<MaterialButton>(R.id.centerButton)
        centerButton.setOnClickListener {
            if (waveAnimationHelper.isAnimating) {
                waveAnimationHelper.stopWaveAnimation()
            } else {
                waveAnimationHelper.startWaveAnimation()
                if (isTestModeEnabled()) {
                    createTestDevice()
                } else {
                    if (deviceType == DEVICE_TYPE_NODE) {
                        sendDiscoverRequest()
                    } else if (deviceType == DEVICE_TYPE_HUB) {
                        checkPermissionsAndScan()
                    }
                }
            }
        }
    }
    private fun determineDeviceType(ssid: String): String {
        return when {
            ssid.contains("000002") -> DEVICE_TYPE_THERMOSTAT
            ssid.contains("000003") -> DEVICE_TYPE_RELAY
            ssid.contains("000004") -> DEVICE_TYPE_CURTAIN
            else -> DEVICE_TYPE_NODE // default type if no pattern matches
        }
    }

    private fun sendDiscoverRequest() {
        val request = Request.Builder()
            .url("http://192.168.1.49/discover")
            .build()

        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@ConnectionActivity, "Ошибка подключения: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.d("ИНтернт", e.message.toString())
                    waveAnimationHelper.stopWaveAnimation()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(this@ConnectionActivity, "Ошибка сервера: ${response.code}", Toast.LENGTH_SHORT).show()
                        waveAnimationHelper.stopWaveAnimation()
                    }
                    return
                }

                try {
                    val responseData = response.body?.string() ?: ""
                    Log.d("HTTP Response", responseData)

                    // Парсим текстовый ответ
                    val devices = parseTextResponse(responseData)

                    runOnUiThread {
                        if (devices.isNotEmpty()) {
                            for (device in devices) {
                                Log.d("devices", device.services.toString())
                                // Определяем тип устройства на основе SSID (если доступен)
                                val deviceType = if (device.services != null) {
                                    when {
                                        device.services!!.contains("Temperature/Humidity") -> DEVICE_TYPE_THERMOSTAT
                                        device.services!!.contains("00000030") -> DEVICE_TYPE_RELAY
                                        device.services!!.contains("00000040") -> DEVICE_TYPE_CURTAIN
                                        else -> DEVICE_TYPE_NODE
                                    }
                                } else {
                                    DEVICE_TYPE_NODE // По умолчанию, если SSID не доступен
                                }
                                createRealDevice(device.name, deviceType)
                            }
                            Toast.makeText(
                                this@ConnectionActivity,
                                "Найдено ${devices.size} устройств",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                this@ConnectionActivity,
                                "Устройства не найдены",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        waveAnimationHelper.stopWaveAnimation()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ConnectionActivity,
                            "Ошибка обработки ответа: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        Log.e("HTTP Error", "Ошибка при обработке ответа", e)
                        waveAnimationHelper.stopWaveAnimation()
                    }
                }
            }
        })
    }

    private fun parseTextResponse(response: String): List<DeviceInfo> {
        val devices = mutableListOf<DeviceInfo>()
        val lines = response.split("\n")

        var currentDevice: DeviceInfo? = null

        for (line in lines) {
            when {
                line.trim().startsWith("Found devices:") -> continue
                line.trim().matches(Regex("\\d+\\. .+")) -> {
                    // Новая строка с устройством (например: "1. Sensor-1")
                    currentDevice?.let { devices.add(it) }
                    val name = line.substring(line.indexOf('.') + 1).trim()
                    currentDevice = DeviceInfo(name)
                }
                line.trim().startsWith("Address:") -> {
                    // Адрес устройства (можно сохранить если нужно)
                    currentDevice?.address = line.substringAfter("Address:").trim()
                }
                line.trim().startsWith("RSSI:") -> {
                    // Уровень сигнала (можно сохранить если нужно)
                    currentDevice?.rssi = line.substringAfter("RSSI:").trim()
                }
                line.trim().startsWith("Services:") -> {
                    // Сервисы устройства (можно сохранить если нужно)
                    currentDevice?.services = line.substringAfter("Services:").trim()
                }
            }
        }

        currentDevice?.let { devices.add(it) }
        return devices
    }

    data class DeviceInfo(
        val name: String,
        var address: String? = null,
        var rssi: String? = null,
        var services: String? = null
    )

    private fun isTestModeEnabled(): Boolean {
        Log.d("isTestModeEnabled", sharedPref.getBoolean(PREF_TEST_CONNECTION, false).toString())
        return sharedPref.getBoolean(PREF_TEST_CONNECTION, false)
    }

    private fun setupToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Устанавливаем заголовок в зависимости от типа устройства
        val title = when (deviceType) {
            DEVICE_TYPE_HUB -> "Подключение хаба"
            DEVICE_TYPE_NODE -> "Подключение устройства"
            else -> "Подключение"
        }
        supportActionBar?.title = title

        // Обработка нажатия на стрелку назад
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun createTestDevice() {
        val device = Device(
            name = "Test ${deviceType.uppercase()}",
            type = deviceType
        )

        store.boxFor<Device>().put(device)

        Toast.makeText(
            this,
            "Тестовое устройство создано (${device.name})",
            Toast.LENGTH_SHORT
        ).show()

        waveAnimationHelper.stopWaveAnimation()
        showDeviceFragment(device.id)
    }

    private fun checkPermissionsAndScan() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 100)
        } else {
            startBleScan()
        }
    }

    private fun startBleScan() {
        try {
            val filter = ScanFilter.Builder()
                .setDeviceName("ESP32_Hub")
                .build()
            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
                .build()

            Log.d("BLE", "Начинаем сканирование...")
            bleScanner.startScan(listOf(filter), settings, scanCallback)

            // Остановка сканирования через 10 секунд
            Handler(Looper.getMainLooper()).postDelayed({
                bleScanner.stopScan(scanCallback)
                Log.d("BLE", "Сканирование остановлено по таймауту")
            }, 10000)
        } catch (e: SecurityException) {
            Log.e("BLE", "Ошибка разрешений: ${e.message}")
            Toast.makeText(this, "Нужны разрешения Bluetooth", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("BLE", "Ошибка сканирования: ${e.message}")
            Toast.makeText(this, "Ошибка сканирования", Toast.LENGTH_SHORT).show()
        }
    }

    private val scanCallback = object : ScanCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            bleScanner.stopScan(this)
            result.device.connectGatt(this@ConnectionActivity, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            runOnUiThread {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d("BLE", "Успешно подключено к устройству")
                        bluetoothGatt = gatt
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.e("BLE", "Соединение разорвано, статус: $status")
                        Toast.makeText(this@ConnectionActivity, "Соединение потеряно", Toast.LENGTH_SHORT).show()
                        gatt.close()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Сервисы найдены: ${gatt.services.size}")
                    gatt.services.forEach { service ->
                        Log.d("BLE", "Service: ${service.uuid}")
                        service.characteristics.forEach { char ->
                            Log.d("BLE", "Characteristic: ${char.uuid}")
                        }
                    }

                    if (deviceType == DEVICE_TYPE_HUB) {
                        showFullConfigDialog(gatt)
                    }

                } else {
                    Log.e("BLE", "Ошибка при поиске сервисов: $status")
                    Toast.makeText(this@ConnectionActivity, "Ошибка при поиске сервисов", Toast.LENGTH_SHORT).show()
                }
            }
        }


        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            runOnUiThread {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    Log.d("BLE", "Данные успешно записаны в характеристику")
                } else {
                    Log.e("BLE", "Ошибка записи в характеристику: $status")
                }
            }
        }
    }

    private fun showFullConfigDialog(gatt: BluetoothGatt) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_wifi_input, null)
        val ssidInput = dialogView.findViewById<EditText>(R.id.ssidInput)
        val passInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Настройка подключения")
            .setView(dialogView)
            .setPositiveButton("Отправить") { _, _ ->
                val ssid = ssidInput.text.toString()
                val password = passInput.text.toString()
                val deviceName = "ESP32_HUB"

                // Determine device type based on SSID
                val deviceType = determineDeviceType(ssid)

                sendCharacteristic(gatt, wifiServiceUUID, ssidUUID, ssid)
                sendCharacteristic(gatt, wifiServiceUUID, passUUID, password)

                // Create device with determined type
                createRealDevice(deviceName, DEVICE_TYPE_HUB)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }


    private fun createRealDevice(name: String, type: String) {
        val device = when (type) {
            DEVICE_TYPE_HUB -> Device(name = name, type = type)
            DEVICE_TYPE_THERMOSTAT -> Device(name = "Thermostat $name", type = type)
            DEVICE_TYPE_RELAY -> Device(name = "Relay $name", type = type)
            DEVICE_TYPE_CURTAIN -> Device(name = "Curtain $name", type = type)
            else -> Device(name = "Node $name", type = DEVICE_TYPE_NODE)
        }

        store.boxFor<Device>().put(device)
        Toast.makeText(this, "Устройство ${device.name} создано", Toast.LENGTH_SHORT).show()
        waveAnimationHelper.stopWaveAnimation()
        showDeviceFragment(device.id)
    }

    private fun showDeviceFragment(deviceId: Long) {
        DeviceFragment.newInstance(deviceId).show(
            supportFragmentManager,
            "device_fragment"
        )
        Log.d("id", deviceId.toString())
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    private fun sendCharacteristic(gatt: BluetoothGatt, serviceUUID: UUID, charUUID: UUID, value: String) {
        val service = gatt.getService(serviceUUID) ?: return
        val characteristic = service.getCharacteristic(charUUID) ?: return
        characteristic.setValue(value)
        gatt.writeCharacteristic(characteristic)
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    override fun onDestroy() {
        super.onDestroy()
        waveAnimationHelper.stopWaveAnimation()
        bluetoothGatt?.close()
    }
}