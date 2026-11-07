package dev.podcast.nativeapp
import org.junit.Assert.*
import org.junit.Test
class PageDiskCacheTest {
    @Test fun snapshotRoundTripDropsAudioAndForcesRefresh() {
        val item = PodcastItem("1","podcast-episodes","标题","作者","https://image","https://private-audio","介绍",123,"https://podcasts.apple.com/1")
        val cached = CachedPage(listOf(item),listOf(EditorialSection("s","新单集",listOf(item))),listOf(BrowseCategory("c","喜剧","image","charts:1")),"",listOf("cursor"),123L)
        val restored = PageDiskCache.decode(PageDiskCache.encode(cached))
        assertEquals(item.copy(audio = ""),restored.rows.single())
        assertEquals("新单集",restored.shelves.single().title)
        assertEquals(cached.categories,restored.categories)
        assertTrue(restored.cursors.isEmpty())
        assertTrue(restored.time < 0)
    }
    @Test fun changesRiseToFrontWithoutDroppingUnchangedItems() {
        val before = listOf("a" to 1,"b" to 2,"c" to 3)
        val fresh = listOf("a" to 1,"b" to 4,"d" to 5,"c" to 3)
        assertEquals(listOf("b" to 4,"d" to 5,"a" to 1,"c" to 3),updatedFirst(fresh,before) { it })
    }
}
