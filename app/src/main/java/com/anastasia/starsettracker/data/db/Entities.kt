package com.anastasia.starsettracker.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "machines")
data class MachineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val multiplier: Double,
    val lastWeight: Int,
    val orderIndex: Int,
    val isActive: Boolean = true
)

@Entity(tableName = "set_logs")
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val workoutDayKey: String,
    val sessionId: Long,
    val machineId: Long,
    val weight: Int,
    val reps: Int?,
    val notes: String?,
    val starIndex: Int
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startTimestampMs: Long,
    val endTimestampMs: Long?,
    val workoutDayKey: String,
    val dayType: String,
    val starsEarned: Int,
    val pointsEarned: Int,
    val questCleared: Boolean,
    val rotationIndexAfter: Int,
    val isComplete: Boolean
)

@Entity(tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val id: Int = 1,
    val rotationIndex: Int,
    val lastCompletedSessionId: Long?
)
