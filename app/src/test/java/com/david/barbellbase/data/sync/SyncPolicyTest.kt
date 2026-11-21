package com.david.barbellbase.data.sync

import com.david.barbellbase.data.prefs.DurableSettings
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncPolicyTest {

    @Test
    fun `local edit newer than remote wins`() {
        assertTrue(SyncPolicy.localWins(localChangedAt = 200L, remoteUpdatedAt = 100L))
    }

    @Test
    fun `remote edit newer than local wins`() {
        assertFalse(SyncPolicy.localWins(localChangedAt = 100L, remoteUpdatedAt = 200L))
    }

    @Test
    fun `ties go to remote so devices converge`() {
        assertFalse(SyncPolicy.localWins(localChangedAt = 100L, remoteUpdatedAt = 100L))
    }

    @Test
    fun `missing local change defers to remote`() {
        assertFalse(SyncPolicy.localWins(localChangedAt = null, remoteUpdatedAt = 100L))
    }

    @Test
    fun `first sync has no overlap window`() {
        assertEquals(0L, SyncPolicy.overlapSince(0L))
    }

    @Test
    fun `later sync re-reads five minutes`() {
        assertEquals(700_000L, SyncPolicy.overlapSince(1_000_000L))
    }

    @Test
    fun `overlap never goes negative`() {
        assertEquals(0L, SyncPolicy.overlapSince(60_000L))
    }

    @Test
    fun `durable settings round trip`() {
        val json = Json { encodeDefaults = true }
        val value = DurableSettings(units = "LB", theme = "LIGHT", waterTargetMl = 3000)
        val encoded = json.encodeToString(DurableSettings.serializer(), value)
        val decoded = json.decodeFromString(DurableSettings.serializer(), encoded)
        assertEquals(value, decoded)
    }
}
