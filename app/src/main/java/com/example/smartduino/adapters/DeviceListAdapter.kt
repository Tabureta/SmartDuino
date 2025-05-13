package com.example.smartduino.adapters

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

class DeviceListAdapter(
    private val devices: List<Device>,
    private val onDeviceClick: (Device) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_LIGHT = 0
        private const val TYPE_THERMOSTAT = 1
        private const val TYPE_CURTAIN = 2
        private const val TYPE_HUB = 3
    }

    override fun getItemViewType(position: Int): Int {
        return when(devices[position].type) {
            "LIGHT" -> TYPE_LIGHT
            "THERMOSTAT" -> TYPE_THERMOSTAT
            "CURTAIN" -> TYPE_CURTAIN
            "HUB" -> TYPE_HUB
            else -> TYPE_LIGHT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when(viewType) {
            TYPE_LIGHT -> {
                val view = inflater.inflate(R.layout.item_device_light, parent, false)
                LightViewHolder(view)
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
                LightViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val device = devices[position]
        when(holder) {
            is LightViewHolder -> holder.bind(device)
            is ThermostatViewHolder -> holder.bind(device)
            is CurtainViewHolder -> holder.bind(device)
            is CurtainViewHolder -> holder.bind(device)
            is HubViewHolder -> holder.bind(device)
        }
    }

    override fun getItemCount() = devices.size

    // ViewHolder для ламп
    inner class LightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(device: Device) {
            itemView.findViewById<TextView>(R.id.deviceName).text = device.name
            itemView.setOnClickListener { onDeviceClick(device) }
            itemView.findViewById<Switch>(R.id.deviceSwitch).setOnCheckedChangeListener{ _, isChecked ->

                val message = if ( itemView.findViewById<Switch>(R.id.deviceSwitch).isChecked) "Свет включен" else "Свет выключен"
                Toast.makeText(
                    itemView.context,
                    message,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // ViewHolder для термостатов
    inner class ThermostatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        fun bind(device: Device) {
            itemView.findViewById<TextView>(R.id.deviceName).text = device.name

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

        }
    }
}