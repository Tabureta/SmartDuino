package com.example.smartduino.adapters


import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.smartduino.R
import com.example.smartduino.entities.Device
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException


class DeviceListAdapter(
    private val devices: List<Device>,
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RELAY = 0
        private const val TYPE_THERMOSTAT = 1
        private const val TYPE_CURTAIN = 2
        private const val TYPE_HUB = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(devices[position].type) {
            "RELAY" -> TYPE_RELAY
            "THERMOSTAT" -> TYPE_THERMOSTAT
            "CURTAIN" -> TYPE_CURTAIN
            "HUB" -> TYPE_HUB
            else -> TYPE_RELAY
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_RELAY -> {
                val view = inflater.inflate(R.layout.item_device_light, parent, false)
                RelayViewHolder(view)
            }
            TYPE_THERMOSTAT -> {
                val view = inflater.inflate(R.layout.item_device_thermostat, parent, false)
                ThermostatViewHolder(view)
            }
            TYPE_CURTAIN -> {
                val view = inflater.inflate(R.layout.item_device_curtain, parent, false)
                CurtainViewHolder(view)
            }
            TYPE_HUB -> {
                val view = inflater.inflate(R.layout.item_device_hub, parent, false)
                HubViewHolder(view)
            }
            else -> {
                // Дефолтный вариант (например, для неизвестных типов)
                val view = inflater.inflate(R.layout.item_device_light, parent, false)
                RelayViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val device = devices[position]
        when(holder) {
            is RelayViewHolder -> holder.bind(device)
            is ThermostatViewHolder -> holder.bind(device)
            is CurtainViewHolder -> holder.bind(device)
            is HubViewHolder -> holder.bind(device)
        }
    }

    override fun getItemCount() = devices.size

    // ViewHolder для ламп
    inner class RelayViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val deviceSwitch: Switch = itemView.findViewById(R.id.deviceSwitch)
        private val okHttpClient = OkHttpClient()

        fun bind(device: Device) {
            deviceName.text = device.name
            deviceSwitch.setOnCheckedChangeListener(null) // Сначала удаляем старый listener

            // Устанавливаем начальное состояние (если нужно)
            // deviceSwitch.isChecked = device.currentState

            deviceSwitch.setOnCheckedChangeListener { _, isChecked ->
                val command = if (isChecked) "relay_on" else "relay_off"
                sendRelayCommand(command, device)

                val message = if (isChecked) "Реле включено" else "Реле выключено"
                Toast.makeText(itemView.context, message, Toast.LENGTH_SHORT).show()
            }

            itemView.setOnClickListener { onDeviceClick(device) }
        }

        private fun sendRelayCommand(command: String, device: Device) {
            val request = Request.Builder()
                .url("http://192.168.1.49/?cmd=$command")
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    itemView.post {
                        Toast.makeText(
                            itemView.context,
                            "Ошибка отправки команды: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Возвращаем Switch в предыдущее состояние при ошибке
                        deviceSwitch.isChecked = !deviceSwitch.isChecked
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        itemView.post {
                            Toast.makeText(
                                itemView.context,
                                "Ошибка сервера: ${response.code}",
                                Toast.LENGTH_SHORT
                            ).show()
                            deviceSwitch.isChecked = !deviceSwitch.isChecked
                        }
                    }
                }
            })
        }
    }

    // ViewHolder для термостатов
    inner class ThermostatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val tempValue: TextView = itemView.findViewById(R.id.temperature)
        private val humidValue: TextView = itemView.findViewById(R.id.humidity)
        private val okHttpClient = OkHttpClient()
        private val handler = Handler(Looper.getMainLooper())
        private var updateRunnable: Runnable? = null
        private var updateInterval = 5000L // 5 секунд

        fun bind(device: Device) {
            deviceName.text = device.name
            itemView.setOnClickListener { onDeviceClick(device) }

            tempValue.text = "--°C"
            humidValue.text = "--%"
            startPeriodicUpdates(device)
        }

        private fun startPeriodicUpdates(device: Device) {
            stopPeriodicUpdates()
            updateRunnable = object : Runnable {
                override fun run() {
                    fetchTemperature(device)
                    fetchHumidity(device)
                    handler.postDelayed(this, updateInterval)
                }
            }
            handler.post(updateRunnable!!)
        }

        private fun stopPeriodicUpdates() {
            updateRunnable?.let {
                handler.removeCallbacks(it)
                updateRunnable = null
            }
        }

        private fun fetchTemperature(device: Device) {
            val request = Request.Builder()
                .url("http://192.168.1.49/?cmd=get_temp")
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Thermostat", "Ошибка получения температуры", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { temp ->
                            itemView.post { // Используем post для обновления UI
                                tempValue.text = "$temp°C"
                            }
                        }
                    }
                }
            })
        }

        private fun fetchHumidity(device: Device) {
            val request = Request.Builder()
                .url("http://192.168.1.49/?cmd=get_humid")
                .build()

            okHttpClient.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("Thermostat", "Ошибка получения влажности", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { humid ->
                            itemView.post { // Используем post для обновления UI
                                humidValue.text = "$humid%"
                            }
                        }
                    }
                }
            })
        }

        fun unbind() {
            stopPeriodicUpdates()
        }
    }

    inner class CurtainViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(device: Device) {
            itemView.findViewById<TextView>(R.id.deviceName).text = device.name
            itemView.findViewById<SeekBar>(R.id.blindsSeekBar).setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {

                }
                override fun onStartTrackingTouch(seekBar: SeekBar) {
                    // Обязательная реализация (можно оставить пустой)
                    Toast.makeText(
                        itemView.context,
                        "Начали регулировать положение",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                override fun onStopTrackingTouch(seekBar: SeekBar) {
                    val position = seekBar.progress
                    Toast.makeText(
                        itemView.context,
                        "Установлено положение: $position%",
                        Toast.LENGTH_SHORT
                    ).show()

                }
            })
        }
    }

    inner class HubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(device: Device) {
            itemView.findViewById<TextView>(R.id.deviceName).text = device.name
            itemView.setOnClickListener { onDeviceClick(device) }
        }
    }
}