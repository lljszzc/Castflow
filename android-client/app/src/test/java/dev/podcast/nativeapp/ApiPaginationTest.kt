package dev.podcast.nativeapp

import org.junit.Assert.*
import org.junit.Test
import org.json.JSONObject

class ApiPaginationTest {
    private fun decode(api: Api, source: String): List<PodcastItem> {
        val method = Api::class.java.getDeclaredMethod("resultsPage",JSONObject::class.java)
        method.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return method.invoke(api,JSONObject(source)) as List<PodcastItem>
    }
    @Test fun chartsUseNestedCursorAndResourceData() {
        val api = Api { null }
        val rows = decode(api,"""{"results":{"podcasts":[{"data":[{"id":"123","type":"podcasts","attributes":{"name":"A"}}],"next":"/v1/catalog/cn/charts?offset=30"}]}}""")
        assertEquals("123",rows.single().id)
        assertEquals("/v1/catalog/cn/charts?offset=30",api.next)
    }
    @Test fun searchRetainsBothIndependentResourceCursors() {
        val api = Api { null }
        val rows = decode(api,"""{"results":{"podcasts":{"data":[{"id":"1","type":"podcasts"}],"next":"/v1/catalog/cn/search?types=podcasts&offset=20"},"podcast-episodes":{"data":[{"id":"2","type":"podcast-episodes"}],"next":"/v1/catalog/cn/search?types=podcast-episodes&offset=20"}}}""")
        assertEquals(setOf("1","2"), rows.map { it.id }.toSet())
        val field = Api::class.java.getDeclaredField("pendingPages").apply { isAccessible = true }
        assertEquals(1,(field.get(api) as List<*>).size)
        assertNotNull(api.next)
        api.clearPagination()
        assertNull(api.next)
        assertEquals(0,(field.get(api) as List<*>).size)
    }
    @Test fun accountEmptyListDoesNotInventContentOrPagination() {
        val api = Api { null }
        assertTrue(decode(api,"""{"data":[]}""").isEmpty())
        assertNull(api.next)
    }
}
