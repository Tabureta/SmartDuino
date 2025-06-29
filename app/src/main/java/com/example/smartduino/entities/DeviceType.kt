package com.example.smartduino.entities

import com.example.smartduino.R

enum class DeviceType(val displayName: String, val iconResId: Int) {
    RELAY("Лампа", R.drawable.add),
    THERMOSTAT("Термостат", R.drawable.add),
    HUB("Хаб", R.drawable.wifi),
    CURTAIN("Шторы", R.drawable.add);

    companion object {
        fun fromString(value: String): DeviceType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}