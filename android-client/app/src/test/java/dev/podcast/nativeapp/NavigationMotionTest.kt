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

    @Test fun tabChangesNeverReuseThePreviousPageContent() {
        val loaded = "zh-Hans-CN/主页/概览/null/null/"
        val search = "zh-Hans-CN/搜索/概览/null/null/"

        org.junit.Assert.assertFalse(canReusePageContent(loaded, search, null))
    }

    @Test fun searchQueryChangesCanKeepPreviousResultsDuringDebounce() {
        val root = "zh-Hans-CN/搜索/概览/null/null"
        val loaded = "$root/电影"
        val next = "$root/电影史"

        org.junit.Assert.assertTrue(canReusePageContent(loaded, next, root))
    }
}
