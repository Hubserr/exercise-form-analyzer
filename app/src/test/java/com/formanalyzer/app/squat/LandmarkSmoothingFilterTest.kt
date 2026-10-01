package com.formanalyzer.app.squat

import org.junit.Assert.assertEquals
import org.junit.Test

class LandmarkSmoothingFilterTest {
    private val delta =1e-9
    private fun frame(x:Double, y:Double) = listOf(PoseLandmarkPoint(x,y))

    @Test
    fun`first frame is returned unchanged`(){
        val filter = LandmarkSmoothingFilter(alpha=0.5)
        val result = filter.smooth(frame(0.3,0.7))
        assertEquals(0.3,result[0].x,delta)
        assertEquals(0.7,result[0].y,delta)

    }
    @Test
    fun`second frame is blened with first`(){
        val filter = LandmarkSmoothingFilter(alpha=0.5)
        filter.smooth(frame(0.0,0.0))
        val result = filter.smooth(frame(1.0,1.0))
        assertEquals(0.5,result[0].x,delta)
        assertEquals(0.5,result[0].y,delta)
    }
    @Test
    fun`constant input stays constant`(){
        val filter = LandmarkSmoothingFilter(alpha=0.3)
        repeat(10) {filter.smooth(frame(0.4,0.6))}
        val result = filter.smooth(frame(0.4,0.6))
        assertEquals(0.4,result[0].x,delta)
        assertEquals(0.6,result[0].y,delta)
    }

    @Test
    fun`output converges to new value after jump`(){
        val filter = LandmarkSmoothingFilter(alpha=0.5)
        filter.smooth(frame(0.0,0.0))
        var result = filter.smooth(frame(0.0,0.0))
        repeat(20) { result = filter.smooth(frame(1.0,0.6))}
        assertEquals(1.0,result[0].x,1e-5)
        assertEquals(0.6,result[0].y,1e-5)
    }
    @Test
    fun`alpha 1 disables smoothing`(){
        val filter = LandmarkSmoothingFilter(alpha=1.0)
        filter.smooth(frame(0.0,0.0))
        val result = filter.smooth(frame(0.8,0.8))

        assertEquals(0.8,result[0].x,delta)
        assertEquals(0.8,result[0].y,delta)

    }
    @Test
    fun`reset forgets the history`(){
        val filter = LandmarkSmoothingFilter(alpha=0.5)
        filter.smooth(frame(0.0,0.0))
        filter.reset()
        val result = filter.smooth(frame(0.8,0.8))

        assertEquals(0.8,result[0].x,delta)
        assertEquals(0.8,result[0].y,delta)

    }
    @Test
    fun`different landmark count resetes hisotry`(){
        val filter = LandmarkSmoothingFilter(alpha=0.5)
        filter.smooth(frame(0.0,0.0))
        val twoPoints=listOf(PoseLandmarkPoint(1.0,1.0), PoseLandmarkPoint(0.5,0.5))
        val result = filter.smooth(twoPoints)

        assertEquals(1.0,result[0].x,delta)
        assertEquals(1.0,result[0].y,delta)

    }
    @Test(expected = IllegalArgumentException::class)
    fun`alpha 0 is rejected`(){
        LandmarkSmoothingFilter(alpha=0.0)
    }


}