package com.david.barbellbase.data.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateVersionTest {

    @Test
    fun `parses v prefixed versions`() {
        assertEquals(listOf(1, 1), UpdateVersion.parse("v1.1"))
    }

    @Test
    fun `parses plain and patch versions`() {
        assertEquals(listOf(1, 2, 3), UpdateVersion.parse("1.2.3"))
    }

    @Test
    fun `ignores prerelease and build suffixes`() {
        assertEquals(listOf(1, 2, 0), UpdateVersion.parse("v1.2.0-beta.1"))
        assertEquals(listOf(1, 2, 0), UpdateVersion.parse("1.2.0+7"))
    }

    @Test
    fun `rejects malformed versions`() {
        assertNull(UpdateVersion.parse("latest"))
        assertNull(UpdateVersion.parse(""))
        assertNull(UpdateVersion.parse("1.x"))
    }

    @Test
    fun `newer versions win`() {
        assertTrue(UpdateVersion.isNewer("1.1", "1.0"))
        assertTrue(UpdateVersion.isNewer("v2.0", "1.9.9"))
        assertTrue(UpdateVersion.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun `same version is not newer`() {
        assertFalse(UpdateVersion.isNewer("1.1", "1.1"))
        assertFalse(UpdateVersion.isNewer("v1.1", "1.1"))
    }

    @Test
    fun `older version is not newer`() {
        assertFalse(UpdateVersion.isNewer("1.0", "1.1"))
    }
}
