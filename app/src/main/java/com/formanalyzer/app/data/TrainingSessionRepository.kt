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

    /** A single saved set */
    suspend fun getSession(id: Long): SavedSession? =
        dao.getSessionWithReps(id)?.toSavedSession()

    /** Saved sets, newest first;  new list whenever the history changes. */
    fun observeHistory(): Flow<List<SavedSession>> =
        dao.observeSessionsWithReps().map { sessions -> sessions.map { it.toSavedSession() } }

    /** Removes all saved sets and their repetitions. */
    suspend fun clearHistory() = database.withTransaction {
        dao.deleteAllReps()
        dao.deleteAllSessions()
    }
}