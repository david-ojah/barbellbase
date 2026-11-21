package com.david.barbellbase.data.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateSkipTest {

    private val update = AvailableUpdate(
        version = "1.2",
        tag = "v1.2",
        apkUrl = "https://example.com/app.apk",
        sha256 = "abc",
        sizeBytes = 100,
        releaseNotes = "Notes",
    )

    @Test
    fun `hidden on the day the version was skipped`() {
        val stored = StoredUpdate(update = update, skippedTag = "v1.2", skippedDay = 20_000)
        assertTrue(stored.isSuppressed(20_000))
    }

    @Test
    fun `visible again the next day`() {
        val stored = StoredUpdate(update = update, skippedTag = "v1.2", skippedDay = 20_000)
        assertFalse(stored.isSuppressed(20_001))
    }

    @Test
    fun `a newer version ignores the old skip`() {
        val newer = update.copy(version = "1.3", tag = "v1.3")
        val stored = StoredUpdate(update = newer, skippedTag = "v1.2", skippedDay = 20_000)
        assertFalse(stored.isSuppressed(20_000))
    }

    @Test
    fun `no available update is never suppressed`() {
        val stored = StoredUpdate(update = null, skippedTag = "v1.2", skippedDay = 20_000)
        assertFalse(stored.isSuppressed(20_000))
    }
}
