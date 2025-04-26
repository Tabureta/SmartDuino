package com.example.smartduino

enum class DeviceType(val displayName: String, val iconResId: Int) {
    LIGHT("Лампа", R.drawable.add),
    THERMOSTAT("Термостат", R.drawable.add),
    TV("Телевизор", R.drawable.add),
    AIR_CONDITIONER("Кондиционер", R.drawable.add),
    SECURITY_CAMERA("Камера", R.drawable.add),
    SPEAKER("Колонка", R.drawable.add),
    SOCKET("Розетка", R.drawable.add),
    CURTAIN("Шторы", R.drawable.add),
    VACUUM("Робот-пылесос", R.drawable.add),
    HUMIDIFIER("Увлажнитель", R.drawable.add);

    companion object {
        fun fromString(value: String): DeviceType? {
            return values().find { it.name.equals(value, ignoreCase = true) }
        }
    }
}