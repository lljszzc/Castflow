package dev.podcast.nativeapp
import org.junit.Test
import org.junit.Assert.*
class EditorialPolicyTest {
    @Test fun traditionalAndSimplifiedHeadingsMerge() {
        val result = uniqueEditorialSections(listOf(EditorialSection("a","热门节目",emptyList()),EditorialSection("b","熱門節目",emptyList()),EditorialSection("c","新单集",emptyList())))
        assertEquals(listOf("热门节目","新单集"),result.map { it.title })
    }
    @Test fun discoveryKeepsOfficialRankAndUsesEditorialFallbackOnlyForMissingCharts() {
        fun episode(id: String) = PodcastItem(id, "podcast-episodes", id, "", "", "", "", 0)
        val previews = DiscoveryChart.entries.map { EditorialSection(it.shelfId, it.title, emptyList()) }.toMutableList()
        previews[0] = previews[0].copy(items = listOf(episode("2"), episode("1")))
        val editorial = listOf(EditorialSection("a", "熱門節目排行", listOf(episode("3"))),
            EditorialSection("b", "熱門單集排行", listOf(episode("4"))), EditorialSection("c", "其他", emptyList()))
        val merged = mergeDiscovery(previews, editorial)
        assertEquals(listOf("chart-shows", "chart-episodes", "chart-subscribers", "c"), merged.map { it.id })
        assertEquals(listOf("2", "1"), merged[0].items.map { it.id })
        assertEquals("4", merged[1].items.single().id)
    }
    @Test fun differentEditorialSectionsRemainSeparate() {
        assertEquals(2,uniqueEditorialSections(listOf(EditorialSection("a","最近播放",emptyList()),EditorialSection("b","继续播放",emptyList()))).size)
    }
}
