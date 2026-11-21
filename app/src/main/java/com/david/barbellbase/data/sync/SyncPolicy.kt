package com.david.barbellbase.data.sync

/** Pure conflict rules used by [SyncRepository], kept separate so they are unit-testable. */
object SyncPolicy {

    private const val CLOCK_OVERLAP_MS = 5 * 60_000L

    /**
     * Per-record last-write-wins: a local edit beats a remote one only when it
     * is strictly newer. Ties go to the remote so devices converge.
     */
    fun localWins(localChangedAt: Long?, remoteUpdatedAt: Long): Boolean =
        localChangedAt != null && localChangedAt > remoteUpdatedAt

    /**
     * Re-reads a small window before the last sync so clock skew between the
     * device and Firestore cannot permanently skip a record.
     */
    fun overlapSince(lastSyncedAt: Long): Long =
        if (lastSyncedAt <= 0L) 0L else (lastSyncedAt - CLOCK_OVERLAP_MS).coerceAtLeast(0L)
}
