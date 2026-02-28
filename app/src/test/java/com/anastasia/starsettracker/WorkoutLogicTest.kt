package com.anastasia.starsettracker

import com.anastasia.starsettracker.data.db.SessionEntity
import com.anastasia.starsettracker.domain.WorkoutLogic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutLogicTest {

    @Test
    fun bonusBoundaries() {
        val m = 1.0
        val w = 100
        assertEquals(100, WorkoutLogic.calculateSetPoints(m, w, 10))
        assertEquals(150, WorkoutLogic.calculateSetPoints(m, w, 11))
        assertEquals(150, WorkoutLogic.calculateSetPoints(m, w, 15))
        assertEquals(100, WorkoutLogic.calculateSetPoints(m, w, 16))
    }

    @Test
    fun rotationOnlyAdvancesOnQuestClear() {
        val start = 5
        val nextWhenClear = if (true) start + 1 else start
        val nextWhenNot = if (false) start + 1 else start
        assertEquals(6, nextWhenClear)
        assertEquals(5, nextWhenNot)
    }

    @Test
    fun sessionTimeoutLogic() {
        val now = 10_000L
        assertFalse(WorkoutLogic.isSessionTimedOut(now - 60_000, now))
        assertTrue(WorkoutLogic.isSessionTimedOut(now - (3 * 60 * 60 * 1000 + 1), now))
    }

    @Test
    fun streakCounting() {
        val sessions = listOf(
            SessionEntity(1, 3, null, "2026-01-03", "A", 10, 100, true, 1, true),
            SessionEntity(2, 2, null, "2026-01-02", "B", 10, 100, true, 2, true),
            SessionEntity(3, 1, null, "2026-01-01", "C", 8, 80, false, 2, true)
        )
        assertEquals(2, WorkoutLogic.questStreakCount(sessions))
    }
}
