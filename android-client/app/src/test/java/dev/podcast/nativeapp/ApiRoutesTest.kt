package dev.podcast.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Test

class ApiRoutesTest {
    @Test fun editorialGroupingAddsRequiredWebContext() {
        assertEquals(
            "/v1/editorial/cn/groupings/193394?platform=web&l=zh-Hans-CN",
            withEditorialWebContext("/v1/editorial/cn/groupings/193394", "zh-Hans-CN")
        )
    }

    @Test fun editorialRoutePreservesExistingQueryAndAvoidsDuplicates() {
        assertEquals(
            "/v1/editorial/cn/multirooms/6752873127?platform=web&l=en-US",
            withEditorialWebContext("/v1/editorial/cn/multirooms/6752873127?platform=web", "en-US")
        )
        assertEquals(
            "/v1/editorial/cn/groupings/193394?platform=web&l=zh-Hans-CN",
            withEditorialWebContext("/v1/editorial/cn/groupings/193394?platform=web&l=zh-Hans-CN", "zh-Hans-CN")
        )
    }

    @Test fun catalogRouteRemainsUnchanged() {
        val route = "/v1/catalog/cn/charts?chart=top&genre=1303&types=podcasts&limit=30"
        assertEquals(route, withEditorialWebContext(route, "zh-Hans-CN"))
    }
}
