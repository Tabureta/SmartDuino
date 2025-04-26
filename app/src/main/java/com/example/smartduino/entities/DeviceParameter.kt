package com.example.smartduino.entities

import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToOne

@Entity
data class DeviceParameter(
    @Id var id: Long = 0,
    var key: String = "",
    var value: String = ""
) {
    lateinit var device: ToOne<Device>
}