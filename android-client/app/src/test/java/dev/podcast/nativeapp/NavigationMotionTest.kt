package dev.podcast.nativeapp

import org.junit.Assert.assertNotEquals
import org.junit.Test

class NavigationMotionTest {
    @Test fun categoryAndShowLayersChangeThePageMotionKey() {
        val search = NavigationMotionKey("搜索", "概览", null, null)
        val category = NavigationMotionKey("搜索", "概览", "197713", null)
        val show = NavigationMotionKey("搜索", "概览", "197713", "1456922519")

        assertNotEquals(search, category)
        assertNotEquals(category, show)
        assertNotEquals(search, show)
    }

    @Test fun returningRestoresThePreviousLayerKey() {
        val category = NavigationMotionKey("搜索", "概览", "197713", null)
        val show = NavigationMotionKey("搜索", "概览", "197713", "1456922519")

        assertNotEquals(show, category)
    }
}
