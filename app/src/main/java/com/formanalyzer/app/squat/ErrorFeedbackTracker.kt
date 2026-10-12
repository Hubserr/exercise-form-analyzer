package com.formanalyzer.app.squat


/** Decides which errors should be announced to te user - each at most once per rep
 * Some errors may stay active for many frames - so we would report the same error in every frame*/
class ErrorFeedbackTracker{

    private val announcedInRep = mutableSetOf<SquatErrorType>()

    /** Return errors that are active in current frame and not announced yet in rep*/
    fun newErrors(state: SquatAnalysisState): Set<SquatErrorType>{
        val fresh = state.activeErrors - announcedInRep
        announcedInRep += fresh
        if(state.lastCompletedRep!=null){
            announcedInRep.clear()
        }
        return fresh
    }
    fun reset(){
        announcedInRep.clear()
    }


}