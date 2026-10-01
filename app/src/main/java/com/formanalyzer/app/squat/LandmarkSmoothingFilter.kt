package com.formanalyzer.app.squat

class LandmarkSmoothingFilter(private val alpha: Double= 0.5) {
    init {
        require(alpha>0.0&&alpha<=1.0){"alpha must be in (0,1], was $alpha"}
    }
    private var previous: List<PoseLandmarkPoint>? = null

    fun smooth(current: List<PoseLandmarkPoint>): List<PoseLandmarkPoint>{
        val prev = previous
        val result = if(prev==null||prev.size!=current.size){
            current
        }else{
            current.mapIndexed { index, point -> blend(prev[index], point)  }
        }
        previous = result
        return result
    }

    /** Forgets history when person leaves frame */
    fun reset(){
        previous = null
    }
    private fun blend(old: PoseLandmarkPoint, new: PoseLandmarkPoint) = PoseLandmarkPoint(
        x = alpha*new.x + (1-alpha) * old.x,
        y = alpha*new.y + (1-alpha) * old.y,
        z = alpha*new.z + (1-alpha) * old.z,
        visibility = new.visibility

    )

}