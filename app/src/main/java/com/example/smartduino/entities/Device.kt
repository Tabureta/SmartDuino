package com.example.smartduino.entities

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany
import io.objectbox.relation.ToOne

@Entity
data class Device(
    @Id var id: Long = 0,
    var name: String = "",
    var type: String = "",
    var ipAddress: String? = null,
    var macAddress: String? = null
) {
    @Backlink(to = "device")
    lateinit var parameters: ToMany<DeviceParameter>

    lateinit var room: ToOne<Room>
}