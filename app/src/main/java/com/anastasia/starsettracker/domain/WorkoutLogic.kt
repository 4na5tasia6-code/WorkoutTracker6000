package com.anastasia.starsettracker.domain

import com.anastasia.starsettracker.data.db.MachineEntity
import com.anastasia.starsettracker.data.db.SessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.roundToInt

enum class DayType { A, B, C }

data class ScoreResult(val points: Int, val questCleared: Boolean, val bonusQuestReached: Boolean)

object WorkoutLogic {
    private const val BONUS_START = 11
    private const val BONUS_END = 15

    val plan = mapOf(
        DayType.A to listOf("Butt Bridge", "Leg Press", "Kickback Left", "Kickback Right", "Abduction Out"),
        DayType.B to listOf("Abduction Out", "Kickback Left", "Kickback Right", "Butt Bridge", "Abduction In"),
        DayType.C to listOf("Leg Press", "Leg Curl", "Butt Bridge", "Abduction Out", "Leg Extension")
    )

    fun dayTypeFromRotation(rotationIndex: Int): DayType = when (rotationIndex % 3) {
        0 -> DayType.A
        1 -> DayType.B
        else -> DayType.C
    }

    fun selectMachinesForDay(dayType: DayType, machines: List<MachineEntity>): List<MachineEntity> {
        val names = plan.getValue(dayType)
        return names.mapNotNull { name -> machines.find { it.name == name && it.isActive } }
    }

    fun calculateSetPoints(multiplier: Double, weight: Int, starIndex: Int): Int {
        val base = (multiplier * weight).roundToInt()
        return if (starIndex in BONUS_START..BONUS_END) (base * 1.5).roundToInt() else base
    }

    fun evaluateSession(stars: Int): ScoreResult {
        return ScoreResult(points = 0, questCleared = stars >= 10, bonusQuestReached = stars >= 15)
    }

    fun isSessionTimedOut(lastLogTimestamp: Long?, nowMs: Long): Boolean {
        if (lastLogTimestamp == null) return false
        return nowMs - lastLogTimestamp > 3 * 60 * 60 * 1000
    }

    fun localDayKey(nowMs: Long): String = Instant.ofEpochMilli(nowMs)
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
        .toString()

    fun softStreakDayCount(logDays: List<String>): Int {
        val dates = logDays.map(LocalDate::parse).toSet()
        var cur = LocalDate.now()
        var count = 0
        while (dates.contains(cur)) {
            count++
            cur = cur.minusDays(1)
        }
        return count
    }

    fun questStreakCount(sessions: List<SessionEntity>): Int {
        var streak = 0
        for (s in sessions.sortedByDescending { it.startTimestampMs }) {
            if (s.questCleared) streak++ else break
        }
        return streak
    }
}
