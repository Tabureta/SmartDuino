package com.example.smartduino.entities

import com.example.smartduino.R

enum class DeviceType(val displayName: String, val iconResId: Int) {
    LIGHT("Лампа", R.drawable.add),
    THERMOSTAT("Термостат", R.drawable.add),
    CURTAIN("Шторы", R.drawable.add);

    companion object {
        fun fromString(value: String): DeviceType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}