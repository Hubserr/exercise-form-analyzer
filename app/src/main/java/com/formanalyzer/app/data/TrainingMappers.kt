package com.formanalyzer.app.data

import com.formanalyzer.app.squat.RepAnalysis
import com.formanalyzer.app.squat.SessionSummary

fun SessionSummary.toSessionEntity() = TrainingSessionEntity(
    startedAtMs = startedAtMs,
    durationMs = durationMs
)

fun RepAnalysis.toEntity(sessionId: Long) = RepEntity(
    sessionId = sessionId,
    repNumber = repNumber,
    minKneeAngleDeg = minKneeAngleDeg,
    maxTrunkLeanVelocityDegPerSec = maxTrunkLeanVelocityDegPerSec,
    errors = errors
)

fun RepEntity.toRepAnalysis() = RepAnalysis(
    repNumber = repNumber,
    minKneeAngleDeg = minKneeAngleDeg,
    maxTrunkLeanVelocityDegPerSec = maxTrunkLeanVelocityDegPerSec,
    errors = errors
)

data class SavedSession(
    val id: Long,
    val summary: SessionSummary
)

fun SessionWithReps.toSavedSession() = SavedSession(
    id = session.id,
    summary = SessionSummary(
        startedAtMs = session.startedAtMs,
        durationMs = session.durationMs,
        // Room does not guarantee the order of related rows.
        reps = reps.sortedBy { it.repNumber }.map { it.toRepAnalysis() }
    )
)