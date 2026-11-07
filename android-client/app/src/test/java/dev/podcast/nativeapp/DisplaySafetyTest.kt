package dev.podcast.nativeapp
import org.junit.Test
import org.junit.Assert.*
class DisplaySafetyTest {
    @Test fun hiddenGestureLineStillHasClearance() { assertTrue(DisplaySafety.lift(60f,16f,0f,16f,6f) >= 16f) }
    @Test fun threeButtonNavigationNeverOverlapsDock() { assertTrue(DisplaySafety.lift(60f,16f,48f,16f,6f) >= 54f) }
    @Test fun largerCornerNeedsAtLeastAsMuchLift() { assertTrue(DisplaySafety.lift(80f,16f,0f,16f,6f) >= DisplaySafety.lift(30f,16f,0f,16f,6f)) }
}
