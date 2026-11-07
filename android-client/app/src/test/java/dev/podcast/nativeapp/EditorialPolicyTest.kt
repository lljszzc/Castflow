package dev.podcast.nativeapp
import org.junit.Test
import org.junit.Assert.*
class EditorialPolicyTest {
    @Test fun traditionalAndSimplifiedHeadingsMerge() {
        val result = uniqueEditorialSections(listOf(EditorialSection("a","热门节目",emptyList()),EditorialSection("b","熱門節目",emptyList()),EditorialSection("c","新单集",emptyList())))
        assertEquals(listOf("热门节目","新单集"),result.map { it.title })
    }
    @Test fun differentEditorialSectionsRemainSeparate() {
        assertEquals(2,uniqueEditorialSections(listOf(EditorialSection("a","最近播放",emptyList()),EditorialSection("b","继续播放",emptyList()))).size)
    }
}
