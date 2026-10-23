package com.formanalyzer.app.squat

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BodyProfileTest {

    @Test
    fun `missing values are valid`() {
        assertTrue(BodyProfile.isValidWeight(null))
        assertTrue(BodyProfile.isValidHeight(null))
        assertTrue(BodyProfile.isValidAge(null))
    }

    @Test
    fun `typical values are valid`() {
        assertTrue(BodyProfile.isValidWeight(75.5))
        assertTrue(BodyProfile.isValidHeight(180.0))
        assertTrue(BodyProfile.isValidAge(23))
    }

    @Test
    fun `range limits are inclusive`() {
        assertTrue(BodyProfile.isValidWeight(30.0))
        assertTrue(BodyProfile.isValidWeight(250.0))
        assertTrue(BodyProfile.isValidAge(10))
        assertTrue(BodyProfile.isValidAge(100))
    }

    @Test
    fun `obvious typos are rejected`() {
        assertFalse(BodyProfile.isValidWeight(7.0))
        assertFalse(BodyProfile.isValidHeight(18.0))
        assertFalse(BodyProfile.isValidAge(230))
    }

    @Test
    fun `profile with all fields is complete`() {
        val profile = BodyProfile(80.0, 180.0, 25, Sex.MALE)
        assertTrue(profile.isComplete)
        assertFalse(profile.isEmpty)
    }

    @Test
    fun `partial profile is neither complete nor empty`() {
        val profile = BodyProfile(weightKg = 80.0)
        assertFalse(profile.isComplete)
        assertFalse(profile.isEmpty)
    }

    @Test
    fun `default profile is empty`() {
        assertTrue(BodyProfile().isEmpty)
    }
}