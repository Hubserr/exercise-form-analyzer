package com.formanalyzer.app.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.formanalyzer.app.squat.SquatErrorType
import androidx.room.Embedded
import androidx.room.Relation

@Entity(tableName = "training_sessions")
data class TrainingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long =0,
    val startedAtMs: Long,
    val durationMs: Long
)

@Entity(
    tableName = "reps",
    foreignKeys =[
        ForeignKey(
            entity = TrainingSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
        indices = [Index("sessionId")]
)

data class  RepEntity(
    @PrimaryKey(autoGenerate = true) val id: Long =0,
    val sessionId: Long,
    val repNumber: Int,
    val minKneeAngleDeg: Double,
    val maxTrunkLeanVelocityDegPerSec: Double,
    val errors: Set<SquatErrorType>
)

/** A set loaded together with all its repetitions (one-to-many relation). */
data class SessionWithReps(
    @Embedded val session: TrainingSessionEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionId")
    val reps: List<RepEntity>
)