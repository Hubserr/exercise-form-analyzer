package com.formanalyzer.app.squat

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import kotlin.math.atan
class JointAngleCalculatorTest {

    private val delta =1e-6

    private fun p(x: Double,y: Double)= PoseLandmarkPoint(x,y)

    @Test
    fun `straight leg gives 180 deegres`(){
        val angle = JointAngleCalculator.angleAt(p(0.5,0.2),p(0.5,0.5),p(0.5,0.8))
        assertEquals(180.0,angle!!, delta)
    }
    @Test
    fun `right angle gives 90 deegres`(){
        val angle = JointAngleCalculator.angleAt(p(0.5,0.0),p(0.5,0.5),p(1.0,0.5))
        assertEquals(90.0,angle!!, delta)
    }
    @Test
    fun `diagonal gives 45 degrees`(){
        val angle = JointAngleCalculator.angleAt(p(0.0,0.0),p(0.0,1.0),p(1.0,0.0))
        assertEquals(45.0,angle!!, delta)
    }
    @Test
    fun `angle does not depend on point order`(){
        val a = p(0.23,0.75); val b= p(0.43,0.15); val c=p(0.67,0.67)
        assertEquals(JointAngleCalculator.angleAt(a,b,c)!!,
                    JointAngleCalculator.angleAt(c,b,a)!!,
                    delta
        )
    }
    @Test
    fun `zero length segment returns null`(){
        assertNull(JointAngleCalculator.angleAt(p(0.0,0.0),p(0.0,0.0),p(1.0,0.5)))
    }

    @Test
    fun `aspect ratio correction gives true angle`() {
        val angle = JointAngleCalculator.angleAt(
            p(0.5, 0.0), p(0.5, 0.5), p(0.75, 0.25), aspectRatio = 2.0
        )
        assertEquals(Math.toDegrees(atan(2.0)), angle!!, delta)

    }
    @Test
    fun `upright trunk is 0 degrees from vertical`(){
        val angle = JointAngleCalculator.angleFromVertical(upper =p(0.5,0.2),lower = p(0.5,0.6))
        assertEquals(0.0,angle!!,delta)
    }
    @Test
    fun `horizontal trunk is 90 degrees from vertical`(){
        val angle = JointAngleCalculator.angleFromVertical(upper =p(0.5,0.2),lower = p(1.0,0.2))
        assertEquals(90.0,angle!!,delta)
    }
    @Test
    fun `trunk leaning forward is 45 degrees from vertical`(){
        val angle = JointAngleCalculator.angleFromVertical(upper =p(0.6,0.4),lower = p(0.5,0.5))
        assertEquals(45.0,angle!!,delta)
    }

    @Test
    fun `midpoint averages coordinates and takes lower visibility`(){
        val m = JointAngleCalculator.midpoint(
            PoseLandmarkPoint(0.2,0.4, visibility = 0.9),
            PoseLandmarkPoint(0.6,0.8, visibility = 0.5)
        )
        assertEquals(0.4, m.x,delta)
        assertEquals(0.6, m.y,delta)
        assertEquals(0.5, m.visibility,delta)
    }
    @Test
    fun `segment length uses pythagoras`() {
        assertEquals(0.5, JointAngleCalculator.segmentLength(p(0.0, 0.0), p(0.3, 0.4)), delta)
    }

    @Test
    fun `segment length applies aspect ratio to x`() {
        val length = JointAngleCalculator.segmentLength(p(0.0, 0.0), p(0.5, 0.0), aspectRatio = 0.5)
        assertEquals(0.25, length, delta)
    }

    @Test
    fun `knee outside its hip gives positive offset`() {
        val offset = JointAngleCalculator.kneeCenterlineOffset(
            knee = p(0.65, 0.7), hip1 = p(0.6, 0.5), hip2 = p(0.4, 0.5)
        )
        assertEquals(0.15, offset, delta)
    }

    @Test
    fun `knee past the centerline gives negative offset`() {
        val offset = JointAngleCalculator.kneeCenterlineOffset(
            knee = p(0.45, 0.7), hip1 = p(0.6, 0.5), hip2 = p(0.4, 0.5)
        )
        assertEquals(-0.05, offset, delta)
    }

    @Test
    fun `offset is symmetric for left and right leg`() {
        val right = JointAngleCalculator.kneeCenterlineOffset(
            knee = p(0.65, 0.7), hip1 = p(0.6, 0.5), hip2 = p(0.4, 0.5)
        )
        val left = JointAngleCalculator.kneeCenterlineOffset(
            knee = p(0.35, 0.7), hip1 = p(0.4, 0.5), hip2 = p(0.6, 0.5)
        )
        assertEquals(right, left, delta)
    }

}