package com.example.smartduino.entities

import io.objectbox.annotation.Backlink
import io.objectbox.annotation.Entity
import io.objectbox.annotation.Id
import io.objectbox.relation.ToMany

@Entity
data class Room(
    @Id var id: Long = 0,
    var name: String = ""
) {
    @Backlink(to = "room")
    lateinit var devices: ToMany<Device>
}
