package com.anastasia.starsettracker.data.repo

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.anastasia.starsettracker.data.db.AppDao
import com.anastasia.starsettracker.data.db.AppStateEntity
import com.anastasia.starsettracker.data.db.MachineEntity
import com.anastasia.starsettracker.data.db.SessionEntity
import com.anastasia.starsettracker.data.db.SetLogEntity
import com.anastasia.starsettracker.domain.Defaults
import com.anastasia.starsettracker.domain.WorkoutLogic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import java.io.File
import java.time.Instant
import java.time.ZoneId

class WorkoutRepository(
    private val dao: AppDao,
    private val appContext: Context
) {
    private val refresh = MutableStateFlow(0)

    val machinesFlow: Flow<List<MachineEntity>> = dao.observeMachines()
    val activeMachinesFlow: Flow<List<MachineEntity>> = dao.observeActiveMachines()

    suspend fun seedDefaultsIfEmpty() {
        if (dao.getMachines().isEmpty()) {
            dao.insertMachines(Defaults.machines)
            dao.upsertAppState(AppStateEntity(rotationIndex = 0, lastCompletedSessionId = null))
        }
    }

    suspend fun currentDayTypeLabel(): String {
        val state = dao.getAppState() ?: AppStateEntity(rotationIndex = 0, lastCompletedSessionId = null)
        return WorkoutLogic.dayTypeFromRotation(state.rotationIndex).name
    }

    fun todayPlanFlow() = combine(machinesFlow, refresh) { machines, _ ->
        val state = dao.getAppState() ?: AppStateEntity(rotationIndex = 0, lastCompletedSessionId = null)
        val dayType = WorkoutLogic.dayTypeFromRotation(state.rotationIndex)
        dayType to WorkoutLogic.selectMachinesForDay(dayType, machines)
    }

    suspend fun getOrCreateActiveSession(nowMs: Long = System.currentTimeMillis()): SessionEntity {
        val open = dao.getOpenSession()
        if (open != null) {
            val lastTs = dao.getLastLogTimestamp(open.id)
            if (!WorkoutLogic.isSessionTimedOut(lastTs, nowMs)) return open
            dao.updateSession(open.copy(isComplete = true, endTimestampMs = nowMs))
        }
        val state = dao.getAppState() ?: AppStateEntity(rotationIndex = 0, lastCompletedSessionId = null)
        val dayType = WorkoutLogic.dayTypeFromRotation(state.rotationIndex).name
        val session = SessionEntity(
            startTimestampMs = nowMs,
            endTimestampMs = null,
            workoutDayKey = WorkoutLogic.localDayKey(nowMs),
            dayType = dayType,
            starsEarned = 0,
            pointsEarned = 0,
            questCleared = false,
            rotationIndexAfter = state.rotationIndex,
            isComplete = false
        )
        val id = dao.insertSession(session)
        return session.copy(id = id)
    }

    suspend fun logSet(machineId: Long, weight: Int, reps: Int? = null): Long {
        val session = getOrCreateActiveSession()
        val machines = dao.getMachines()
        val machine = machines.first { it.id == machineId }
        val starIndex = session.starsEarned + 1
        val setPoints = WorkoutLogic.calculateSetPoints(machine.multiplier, weight, starIndex)
        val now = System.currentTimeMillis()
        val logId = dao.insertSetLog(
            SetLogEntity(
                timestampMs = now,
                workoutDayKey = session.workoutDayKey,
                sessionId = session.id,
                machineId = machineId,
                weight = weight,
                reps = reps,
                notes = null,
                starIndex = starIndex
            )
        )
        dao.updateMachine(machine.copy(lastWeight = weight))
        val stars = session.starsEarned + 1
        dao.updateSession(
            session.copy(
                starsEarned = stars,
                pointsEarned = session.pointsEarned + setPoints,
                questCleared = stars >= 10
            )
        )
        refresh.value++
        return logId
    }

    suspend fun undoLastLog(logId: Long) {
        val session = dao.getOpenSession() ?: return
        val logs = dao.observeLogsForSession(session.id).first()
        val target = logs.find { it.id == logId } ?: return
        val machine = dao.getMachines().first { it.id == target.machineId }
        val points = WorkoutLogic.calculateSetPoints(machine.multiplier, target.weight, target.starIndex)
        dao.deleteSetLog(logId)
        val stars = (session.starsEarned - 1).coerceAtLeast(0)
        dao.updateSession(
            session.copy(
                starsEarned = stars,
                pointsEarned = (session.pointsEarned - points).coerceAtLeast(0),
                questCleared = stars >= 10
            )
        )
        refresh.value++
    }

    suspend fun finishSession() {
        val session = dao.getOpenSession() ?: return
        val complete = session.copy(endTimestampMs = System.currentTimeMillis(), isComplete = true)
        dao.updateSession(complete)
        val state = dao.getAppState() ?: AppStateEntity(rotationIndex = 0, lastCompletedSessionId = null)
        val nextRotation = if (session.questCleared) state.rotationIndex + 1 else state.rotationIndex
        dao.upsertAppState(
            state.copy(
                rotationIndex = nextRotation,
                lastCompletedSessionId = session.id
            )
        )
        refresh.value++
    }

    fun activeSessionFlow() = dao.observeOpenSession()

    fun logsForActiveSessionFlow(): Flow<List<SetLogEntity>> = dao.observeOpenSession().flatMapLatest { open ->
        if (open == null) kotlinx.coroutines.flow.flowOf(emptyList()) else dao.observeLogsForSession(open.id)
    }

    fun recentSessionsFlow(limit: Int = 14): Flow<List<SessionEntity>> = dao.observeRecentSessions(limit)

    suspend fun getAllSessions(): List<SessionEntity> = dao.getAllSessions()
    suspend fun getAllLogs(): List<SetLogEntity> = dao.getAllLogs()

    suspend fun saveMachine(machine: MachineEntity) = dao.updateMachine(machine)

    suspend fun resetDefaults() {
        dao.replaceMachines(Defaults.machines)
    }

    suspend fun exportCsvShareIntent(): Intent {
        val sessions = dao.getAllSessions()
        val logs = dao.getAllLogs()
        val dir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "starset_export_${System.currentTimeMillis()}.csv")
        file.writeText(buildString {
            appendLine("Sessions")
            appendLine("id,start,end,dayKey,dayType,stars,points,questCleared,isComplete")
            sessions.forEach {
                appendLine("${it.id},${it.startTimestampMs},${it.endTimestampMs ?: ""},${it.workoutDayKey},${it.dayType},${it.starsEarned},${it.pointsEarned},${it.questCleared},${it.isComplete}")
            }
            appendLine()
            appendLine("SetLogs")
            appendLine("id,sessionId,machineId,dayKey,timestamp,weight,reps,starIndex")
            logs.forEach {
                appendLine("${it.id},${it.sessionId},${it.machineId},${it.workoutDayKey},${it.timestampMs},${it.weight},${it.reps ?: ""},${it.starIndex}")
            }
        })

        val uri = FileProvider.getUriForFile(appContext, "${appContext.packageName}.fileprovider", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun dayKeyFromTimestamp(ts: Long): String = Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()).toLocalDate().toString()
}
