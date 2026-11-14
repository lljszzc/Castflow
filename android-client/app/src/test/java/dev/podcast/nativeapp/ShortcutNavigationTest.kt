package dev.podcast.nativeapp

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShortcutNavigationTest {
    @Test fun actionsMapToStableLibraryAndSearchSections() {
        assertEquals(ShortcutDestination.SEARCH, ShortcutDestination.fromAction(ShortcutActions.SEARCH))
        assertEquals(ShortcutDestination.LATEST_EPISODE, ShortcutDestination.fromAction(ShortcutActions.LATEST_EPISODE))
        assertEquals(ShortcutDestination.RECENTLY_PLAYED, ShortcutDestination.fromAction(ShortcutActions.RECENTLY_PLAYED))
        assertEquals("资料库", ShortcutDestination.LATEST_EPISODE.tab)
        assertEquals("最新单集", ShortcutDestination.LATEST_EPISODE.section)
        assertNull(ShortcutDestination.fromAction("dev.podcast.OPEN_PLAYER"))
    }
}
