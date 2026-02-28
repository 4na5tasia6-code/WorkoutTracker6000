package com.anastasia.starsettracker.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM machines ORDER BY orderIndex")
    fun observeMachines(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines WHERE isActive = 1 ORDER BY orderIndex")
    fun observeActiveMachines(): Flow<List<MachineEntity>>

    @Query("SELECT * FROM machines ORDER BY orderIndex")
    suspend fun getMachines(): List<MachineEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMachines(machines: List<MachineEntity>)

    @Update
    suspend fun updateMachine(machine: MachineEntity)

    @Query("DELETE FROM machines")
    suspend fun clearMachines()

    @Query("SELECT * FROM app_state WHERE id = 1")
    suspend fun getAppState(): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAppState(state: AppStateEntity)

    @Query("SELECT * FROM sessions WHERE isComplete = 0 ORDER BY startTimestampMs DESC LIMIT 1")
    suspend fun getOpenSession(): SessionEntity?

    @Insert
    suspend fun insertSession(session: SessionEntity): Long

    @Update
    suspend fun updateSession(session: SessionEntity)

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions ORDER BY startTimestampMs DESC LIMIT :limit")
    fun observeRecentSessions(limit: Int = 14): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions ORDER BY startTimestampMs DESC")
    suspend fun getAllSessions(): List<SessionEntity>

    @Insert
    suspend fun insertSetLog(log: SetLogEntity): Long

    @Query("DELETE FROM set_logs WHERE id = :id")
    suspend fun deleteSetLog(id: Long)

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY id DESC")
    fun observeLogsForSession(sessionId: Long): Flow<List<SetLogEntity>>

    @Query("SELECT * FROM set_logs WHERE sessionId = :sessionId ORDER BY id DESC LIMIT 1")
    suspend fun getLastLogForSession(sessionId: Long): SetLogEntity?

    @Query("SELECT * FROM set_logs ORDER BY timestampMs DESC")
    suspend fun getAllLogs(): List<SetLogEntity>

    @Query("SELECT * FROM sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SessionEntity?>

    @Query("SELECT MAX(timestampMs) FROM set_logs WHERE sessionId = :sessionId")
    suspend fun getLastLogTimestamp(sessionId: Long): Long?

    @Query("SELECT * FROM sessions WHERE isComplete = 0 ORDER BY startTimestampMs DESC LIMIT 1")
    fun observeOpenSession(): Flow<SessionEntity?>

    @Transaction
    suspend fun replaceMachines(machines: List<MachineEntity>) {
        clearMachines()
        insertMachines(machines)
    }
}
