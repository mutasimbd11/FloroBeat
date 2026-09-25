package com.florosoft.florobeat.update

import com.florosoft.florobeat.data.update.SemanticVersion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [SemanticVersion] comparison and parsing.
 */
class SemanticVersionTest {

    @Test
    fun `parses standard semantic version strings`() {
        val v1 = SemanticVersion.parse("1.2.3")
        assertEquals(1, v1.major)
        assertEquals(2, v1.minor)
        assertEquals(3, v1.patch)
        assertEquals(0, v1.build)

        val v2 = SemanticVersion.parse("v2.0.0")
        assertEquals(2, v2.major)
        assertEquals(0, v2.minor)
        assertEquals(0, v2.patch)

        val v3 = SemanticVersion.parse("1.6")
        assertEquals(1, v3.major)
        assertEquals(6, v3.minor)
        assertEquals(0, v3.patch)
    }

    @Test
    fun `compares version increments correctly`() {
        val v1_0_0 = SemanticVersion.parse("1.0.0")
        val v1_1_0 = SemanticVersion.parse("1.1.0")
        val v1_1_1 = SemanticVersion.parse("1.1.1")
        val v2_0_0 = SemanticVersion.parse("2.0.0")

        assertTrue(v1_1_0.isNewerThan(v1_0_0))
        assertTrue(v1_1_1.isNewerThan(v1_1_0))
        assertTrue(v2_0_0.isNewerThan(v1_1_1))

        assertFalse(v1_0_0.isNewerThan(v1_1_0))
        assertFalse(v1_1_0.isNewerThan(v1_1_0))
    }

    @Test
    fun `compares numerical parts rather than alphabetic`() {
        val v1_9_0 = SemanticVersion.parse("1.9.0")
        val v1_10_0 = SemanticVersion.parse("1.10.0")

        assertTrue("1.10.0 must be newer than 1.9.0", v1_10_0.isNewerThan(v1_9_0))
        assertFalse(v1_9_0.isNewerThan(v1_10_0))

        val v2_2 = SemanticVersion.parse("v2.2.0")
        val v2_15 = SemanticVersion.parse("v2.15.0")
        assertTrue("2.15.0 must be newer than 2.2.0", v2_15.isNewerThan(v2_2))
    }

    @Test
    fun `handles pre-releases where release outranks pre-release`() {
        val beta = SemanticVersion.parse("1.2.0-beta.1")
        val release = SemanticVersion.parse("1.2.0")

        assertTrue("Final release outranks pre-release", release.isNewerThan(beta))
        assertFalse(beta.isNewerThan(release))

        val beta1 = SemanticVersion.parse("1.2.0-beta.1")
        val beta2 = SemanticVersion.parse("1.2.0-beta.2")
        assertTrue(beta2.isNewerThan(beta1))
    }

    @Test
    fun `handles empty and invalid version strings safely`() {
        val empty = SemanticVersion.parse("")
        assertEquals(0, empty.major)
        assertEquals(0, empty.minor)

        val invalid = SemanticVersion.parse("abc")
        assertEquals(0, invalid.major)
    }
}
