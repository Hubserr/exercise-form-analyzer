package com.formanalyzer.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface TrainingSessionDao{

    @Insert
    suspend fun insertSession(session: TrainingSessionEntity): Long

    @Insert
    suspend fun insertReps(reps: List<RepEntity>)

    /** All sets, newest first, with their reps; emits again whenever the data changes. */
    @Transaction
    @Query("SELECT * FROM training_sessions ORDER BY startedAtMs DESC")
    fun observeSessionsWithReps(): Flow<List<SessionWithReps>>

    @Query("DELETE FROM reps")
    suspend fun deleteAllReps()

    @Query("DELETE FROM training_sessions")
    suspend fun deleteAllSessions()
    @Transaction
    @Query("SELECT * FROM training_sessions WHERE id = :id")
    suspend fun getSessionWithReps(id: Long): SessionWithReps?

}