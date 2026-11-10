package dev.podcast.nativeapp

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class ProgressStoreTest {
    private fun checkpoint(time: String, position: Long = 1000) = JSONObject()
        .put("recordedAtTimestamp", time).put("positionInMilliseconds", position)

    @Test fun newerLocalCheckpointSurvivesRemoteRead() {
        val local = checkpoint("2026-01-02T00:00:00Z")
        assertSame(local, ProgressStore.newerProgress(local, checkpoint("2026-01-01T00:00:00Z")))
        assertSame(local, ProgressStore.newerProgress(local, checkpoint("invalid")))
        assertSame(local, ProgressStore.newerProgress(local, checkpoint("2026-01-02T00:00:00Z")))
    }

    @Test fun newerRemoteCheckpointWins() {
        val remote = checkpoint("2026-01-03T00:00:00Z", 2400)
        val result = ProgressStore.newerProgress(checkpoint("invalid"), remote)
        assertEquals(2400L, ProgressStore.resumePosition(result))
    }

    @Test fun invalidOrCompletedPositionStartsAtZero() {
        assertEquals(0L, ProgressStore.resumePosition(null))
        assertEquals(0L, ProgressStore.resumePosition(checkpoint("", -100)))
        assertEquals(0L, ProgressStore.resumePosition(checkpoint("").put("completed", true)))
    }
}
