package com.formanalyzer.app.squat

data class SessionSummary(
    val startedAtMs: Long,
    val durationMs: Long,
    val reps: List<RepAnalysis>
){
    val totalReps: Int
        get() = reps.size
    val correctReps: Int
        get() = reps.count{it.isCorrect}

    fun errorCount(error: SquatErrorType): Int = reps.count(){error in it.errors }
}

class SessionRecorder{

    private var startedAtMs: Long? = null
    private val reps = mutableListOf<RepAnalysis>()
    val isStarted: Boolean
        get() = startedAtMs != null

    val repCount: Int
        get() = reps.size

    /** Starts the set*/
    fun start(nowMs: Long){
        if (startedAtMs==null){
            startedAtMs = nowMs
        }
    }

    /** stores repetition completed in frame*/
    fun record(state: SquatAnalysisState){
        if(!isStarted) return

        state.lastCompletedRep?.let{reps +=it}
    }

    fun finish(nowMs: Long): SessionSummary? {
        val start = startedAtMs ?: return null
        val summary = SessionSummary(
            startedAtMs = start,
            durationMs = (nowMs-start).coerceAtLeast(0),
            reps =reps.toList()
        )
        startedAtMs =null
        reps.clear()
        return summary

    }


}