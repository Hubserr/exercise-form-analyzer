package com.formanalyzer.app.data

import androidx.room.withTransaction
import com.formanalyzer.app.squat.SessionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
class TrainingSessionRepository(private val database: AppDatabase) {

    private val dao = database.trainingSessionDao()


    suspend fun saveSession(summary: SessionSummary): Long =
        database.withTransaction {
            val sessionId = dao.insertSession(summary.toSessionEntity())
            dao.insertReps(summary.reps.map { it.toEntity(sessionId) })
            sessionId
        }

    /** Saved sets, newest first; emits a new list whenever the history changes. */
    fun observeHistory(): Flow<List<SavedSession>> =
        dao.observeSessionsWithReps().map { sessions -> sessions.map { it.toSavedSession() } }


}