package com.example.smartduino.supactivities

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.smartduino.R
import com.google.android.material.button.MaterialButton
import java.util.*

class ConnectionActivity : AppCompatActivity() {

    private val waveViews = mutableListOf<View>()
    private val handler = Handler(Looper.getMainLooper())
    private var isAnimating = false

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bleScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null

    private val serviceUUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb") // пример UUID
    private val characteristicUUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection)

        bluetoothAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        bleScanner = bluetoothAdapter.bluetoothLeScanner

        val centerButton = findViewById<MaterialButton>(R.id.centerButton)
        centerButton.setOnClickListener {
            if (isAnimating) {
                stopWaveAnimation()
            } else {
                startWaveAnimation()
                checkPermissionsAndScan()
            }
        }
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
        val filter = ScanFilter.Builder()
            .setDeviceName("ESP32_BLE")
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            bleScanner.stopScan(this)
            result.device.connectGatt(this@ConnectionActivity, false, gattCallback)
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            runOnUiThread {
                showWiFiInputDialog(gatt)
            }
        }
    }

    private fun showWiFiInputDialog(gatt: BluetoothGatt) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_wifi_input, null)
        val ssidInput = dialogView.findViewById<EditText>(R.id.ssidInput)
        val passInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Подключение к Wi-Fi")
            .setView(dialogView)
            .setPositiveButton("Отправить") { _, _ ->
                val ssid = ssidInput.text.toString()
                val password = passInput.text.toString()
                sendWiFiCredentials(gatt, ssid, password)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun sendWiFiCredentials(gatt: BluetoothGatt, ssid: String, password: String) {
        val service = gatt.getService(serviceUUID) ?: return
        val characteristic = service.getCharacteristic(characteristicUUID) ?: return
        val credentials = "$ssid|$password"
        characteristic.value = credentials.toByteArray()
        gatt.writeCharacteristic(characteristic)
    }

    private fun startWaveAnimation() {
        isAnimating = true
        handler.post(waveRunnable)
    }

    private fun stopWaveAnimation() {
        isAnimating = false
        handler.removeCallbacks(waveRunnable)
        clearWaves()
    }

    private val waveRunnable = object : Runnable {
        override fun run() {
            createWave()
            if (isAnimating) {
                handler.postDelayed(this, 800)
            }
        }
    }

    private fun createWave() {
        val waveView = ImageView(this).apply {
            setImageResource(R.drawable.wave_circle)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply { gravity = Gravity.CENTER }
            scaleX = 0.2f
            scaleY = 0.2f
            alpha = 0.8f
        }

        val container = findViewById<FrameLayout>(R.id.waveContainer)
        container.addView(waveView)
        waveViews.add(waveView)

        waveView.animate()
            .scaleX(4f)
            .scaleY(4f)
            .alpha(0f)
            .setDuration(1000)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                container.removeView(waveView)
                waveViews.remove(waveView)
            }
            .start()
    }

    private fun clearWaves() {
        val container = findViewById<FrameLayout>(R.id.waveContainer)
        waveViews.forEach { container.removeView(it) }
        waveViews.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopWaveAnimation()
        bluetoothGatt?.close()
    }
}
