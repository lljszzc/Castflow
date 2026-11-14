package dev.podcast.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test
import org.json.JSONObject

class PlaybackQueueTest {
    private fun episode(id: String, audio: String = "https://example.com/$id.mp3") =
        PodcastItem(id, "podcast-episodes", "Episode $id", "Author", "", audio, "", 60_000)

    @Test fun currentEpisodeKeepsShowOrderAndFiltersUnplayableItems() {
        val current = episode("2")
        val queue = buildPlaybackQueue(current, listOf(
            episode("1"),
            episode("2"),
            episode("3", audio = ""),
            episode("4")
        ))

        assertEquals(listOf("1", "2", "4"), queue.map { it.id })
    }

    @Test fun currentEpisodeIsPrependedWhenShowListDoesNotContainIt() {
        val current = episode("2")
        val queue = buildPlaybackQueue(current, listOf(episode("1"), episode("3")))

        assertEquals(listOf("2", "1", "3"), queue.map { it.id })
    }

    @Test fun duplicateEpisodesAreRemovedAndCurrentMetadataWins() {
        val current = episode("2", "https://example.com/current.mp3")
        val queue = buildPlaybackQueue(current, listOf(
            episode("2", "https://example.com/stale.mp3"),
            episode("2")
        ))

        assertEquals(1, queue.size)
        assertEquals("https://example.com/current.mp3", queue.single().audio)
    }

    @Test fun episodeUsesRelatedShowIdForAutomaticQueueDiscovery() {
        val episode = JSONObject(
            """{"id":"334","type":"podcast-episodes","attributes":{"name":"Episode"},"relationships":{"podcasts":{"data":[{"id":"1456922519","type":"podcasts"}]}}}"""
        ).item()

        assertEquals("1456922519", episode.showId)
    }

    @Test fun episodeFallsBackToShowIdInPublicUrl() {
        val episode = JSONObject(
            """{"id":"334","type":"podcast-episodes","attributes":{"name":"Episode","url":"https://podcasts.apple.com/cn/podcast/trash-taste/id1456922519?i=1000582819794"}}"""
        ).item()

        assertEquals("1456922519", episode.showId)
    }
}
