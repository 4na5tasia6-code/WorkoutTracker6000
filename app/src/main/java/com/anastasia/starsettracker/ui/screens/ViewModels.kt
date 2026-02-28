package com.anastasia.starsettracker.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.anastasia.starsettracker.data.db.AppDatabase
import com.anastasia.starsettracker.data.db.MachineEntity
import com.anastasia.starsettracker.data.db.SessionEntity
import com.anastasia.starsettracker.data.repo.WorkoutRepository
import com.anastasia.starsettracker.domain.WorkoutLogic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val dayType: String = "A",
    val stars: Int = 0,
    val points: Int = 0,
    val machines: List<MachineEntity> = emptyList(),
    val sessionId: Long? = null,
    val questCleared: Boolean = false,
    val bonusReached: Boolean = false
)

class TodayViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.get(app).dao(), app)
    private val eventFlow = MutableStateFlow(0)

    init {
        viewModelScope.launch { repo.seedDefaultsIfEmpty() }
    }

    val uiState: StateFlow<TodayUiState> = combine(
        repo.todayPlanFlow(),
        repo.activeSessionFlow(),
        eventFlow
    ) { dayPlan, session, _ ->
        val dayType = dayPlan.first.name
        TodayUiState(
            dayType = dayType,
            stars = session?.starsEarned ?: 0,
            points = session?.pointsEarned ?: 0,
            machines = dayPlan.second,
            sessionId = session?.id,
            questCleared = (session?.starsEarned ?: 0) >= 10,
            bonusReached = (session?.starsEarned ?: 0) >= 15
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayUiState())

    val logs = repo.logsForActiveSessionFlow().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun logSet(machine: MachineEntity, weight: Int, onLogged: (Long) -> Unit) {
        viewModelScope.launch {
            val id = repo.logSet(machine.id, weight)
            onLogged(id)
            eventFlow.value++
        }
    }

    fun undo(logId: Long) {
        viewModelScope.launch {
            repo.undoLastLog(logId)
            eventFlow.value++
        }
    }

    fun finishWorkout() {
        viewModelScope.launch {
            repo.finishSession()
            eventFlow.value++
        }
    }

    fun saveMachine(machine: MachineEntity) {
        viewModelScope.launch { repo.saveMachine(machine) }
    }
}

data class StatsUiState(
    val questStreak: Int = 0,
    val softStreak: Int = 0,
    val sessions: List<SessionEntity> = emptyList(),
    val weeklyTotalPoints: Int = 0,
    val bestSessionPoints: Int = 0
)

class StatsViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.get(app).dao(), app)

    val uiState = repo.recentSessionsFlow().combine(repo.recentSessionsFlow(200)) { recent, all ->
        val quest = WorkoutLogic.questStreakCount(all)
        val dayKeys = all.map { it.workoutDayKey }.distinct()
        val soft = WorkoutLogic.softStreakDayCount(dayKeys)
        val weekCut = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        StatsUiState(
            questStreak = quest,
            softStreak = soft,
            sessions = recent,
            weeklyTotalPoints = all.filter { it.startTimestampMs >= weekCut }.sumOf { it.pointsEarned },
            bestSessionPoints = all.maxOfOrNull { it.pointsEarned } ?: 0
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsUiState())

    fun export(onIntent: (android.content.Intent) -> Unit) {
        viewModelScope.launch { onIntent(repo.exportCsvShareIntent()) }
    }
}

class MachinesViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = WorkoutRepository(AppDatabase.get(app).dao(), app)

    init {
        viewModelScope.launch { repo.seedDefaultsIfEmpty() }
    }

    val machines = repo.machinesFlow.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(machine: MachineEntity) {
        viewModelScope.launch { repo.saveMachine(machine) }
    }

    fun move(machine: MachineEntity, up: Boolean) {
        viewModelScope.launch {
            val all = machines.value.sortedBy { it.orderIndex }.toMutableList()
            val idx = all.indexOfFirst { it.id == machine.id }
            val target = if (up) idx - 1 else idx + 1
            if (idx in all.indices && target in all.indices) {
                val a = all[idx]
                val b = all[target]
                save(a.copy(orderIndex = b.orderIndex))
                save(b.copy(orderIndex = a.orderIndex))
            }
        }
    }

    fun resetDefaults() {
        viewModelScope.launch { repo.resetDefaults() }
    }
}
