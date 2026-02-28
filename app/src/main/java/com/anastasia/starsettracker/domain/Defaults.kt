package com.anastasia.starsettracker.domain

import com.anastasia.starsettracker.data.db.MachineEntity

object Defaults {
    val machines = listOf(
        MachineEntity(name = "Leg Curl", multiplier = 1.10, lastWeight = 50, orderIndex = 0),
        MachineEntity(name = "Leg Extension", multiplier = 1.00, lastWeight = 50, orderIndex = 1),
        MachineEntity(name = "Abduction Out", multiplier = 1.25, lastWeight = 100, orderIndex = 2),
        MachineEntity(name = "Abduction In", multiplier = 0.95, lastWeight = 100, orderIndex = 3),
        MachineEntity(name = "Butt Bridge", multiplier = 1.60, lastWeight = 100, orderIndex = 4),
        MachineEntity(name = "Kickback Left", multiplier = 1.20, lastWeight = 75, orderIndex = 5),
        MachineEntity(name = "Kickback Right", multiplier = 1.20, lastWeight = 75, orderIndex = 6),
        MachineEntity(name = "Leg Press", multiplier = 1.40, lastWeight = 90, orderIndex = 7)
    )
}
