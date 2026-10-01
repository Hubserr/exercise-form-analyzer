package com.formanalyzer.app.squat

import androidx.camera.core.AspectRatio
import kotlin.math.hypot
import kotlin.math.acos

/**
 * Geometric helpers for computing joint angles from 2D landmarks.
 *
 * MediaPipe normalizes x by image width and y by image height, so on a
 * non-square frame the two axes have different scales. Pass
 * aspectRatio = imageWidth / imageHeight to measure true angles.
 */

object JointAngleCalculator{
    private const val EPSILON = 1e-9


    /**
     * Angle at vertex between two vectors created by 3 points
     */

    fun angleAt(
        first: PoseLandmarkPoint,
        vertex: PoseLandmarkPoint,
        last: PoseLandmarkPoint,
        aspectRatio: Double =1.0
    ): Double?{
        val v1x = (first.x - vertex.x) * aspectRatio
        val v1y = first.y -vertex.y
        val v2x = (last.x - vertex.x)*aspectRatio
        val v2y = last.y -vertex.y

        val len1= hypot(v1x,v1y)
        val len2 = hypot(v2x,v2y)
        if(len1<EPSILON||len2<EPSILON) return null

        val cos = ( (v1x*v2x + v1y*v2y)/(len1*len2)).coerceIn(-1.0,1.0)
        return Math.toDegrees(acos(cos))
    }
    /**
     * Angle between  two segments  and the vertical "up" direction,
     * in degrees [0..180]. 0 = perfectly upright, 90 = horizontal.
     * Image y grows downward, so "up" is the vector (0, -1).
     */
    fun angleFromVertical(
        upper: PoseLandmarkPoint,
        lower: PoseLandmarkPoint,
        aspectRatio: Double =1.0
    ): Double? {
        val dx = (upper.x -lower.x)*aspectRatio
        val dy = upper.y - lower.y
        val len = hypot(dx,dy)
        if(len<EPSILON) return null
        val cos = (-dy/len).coerceIn(-1.0,1.0)
        return Math.toDegrees(acos(cos))

    }

    fun midpoint(a: PoseLandmarkPoint,b: PoseLandmarkPoint): PoseLandmarkPoint= PoseLandmarkPoint(
        x=(a.x+b.x)/2,
        y=(a.y+b.y)/2,
        z =(a.z +b.z)/2,
        visibility = minOf(a.visibility,b.visibility)
    )

}
